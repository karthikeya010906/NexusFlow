package com.nexusflow.core.engine

import android.content.Context
import android.util.Log
import com.nexusflow.NexusFlowApp
import com.nexusflow.core.model.*
import com.nexusflow.data.db.entities.LogEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private const val TAG = "AutomationEngine"

class AutomationEngine(private val appContext: Context) {

    private val scope = CoroutineScope(Dispatchers.IO)

    @Volatile
    var globalEnabled: Boolean = true

    /**
     * Entry point for evaluation. 
     * @param context The current system state snapshot.
     * @param triggerType The specific event that just occurred (e.g., SCREEN_ON). 
     *                    If null, it's a general state check (e.g., from a periodic worker).
     */
    fun evaluate(context: TriggerContext, triggerType: TriggerType? = null) {
        if (!globalEnabled) {
            Log.d(TAG, "Engine globally disabled — skipping")
            return
        }

        scope.launch {
            try {
                val app = appContext.applicationContext as NexusFlowApp
                val rules = app.ruleRepository.observeAllRules().first()
                
                val enabledRules = rules.filter { it.isEnabled }
                
                Log.d(TAG, "Evaluating ${enabledRules.size} active rules. Event: ${triggerType?.name ?: "STATE_CHECK"}")

                for (rule in enabledRules.sortedByDescending { it.priority }) {
                    // COORDINATION FIX:
                    // 1. If triggerType is null (periodic check), we check if the rule has any state-based triggers 
                    //    that are currently active (like TIME_OF_DAY).
                    // 2. If triggerType is provided, the rule MUST contain a trigger of that type to be considered.
                    
                    val shouldEvaluate = if (triggerType != null) {
                        rule.triggers.any { it.type == triggerType }
                    } else {
                        // For state checks, we evaluate rules that have triggers compatible with periodic polling
                        // e.g. Battery Level, Time, or if they are purely condition-based (though our model expects a trigger).
                        true 
                    }
                    
                    if (shouldEvaluate) {
                        evaluateRule(rule, context, triggerType)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in evaluation loop", e)
            }
        }
    }

    private suspend fun evaluateRule(rule: Rule, context: TriggerContext, eventType: TriggerType?) {
        try {
            val app = appContext.applicationContext as NexusFlowApp
            
            // 1. Triggers: Do they pass?
            // If it was an event-based trigger, we check if the specific event condition is met.
            val triggersPass = when (rule.triggerMode) {
                TriggerMode.ANY -> rule.triggers.any { it.checkCondition(context) }
                TriggerMode.ALL -> rule.triggers.all { it.checkCondition(context) }
            }
            
            if (!triggersPass) {
                return
            }

            // 2. Conditions: These are the "coordinated" state checks.
            // Triggers "Fire" the rule, but Conditions "Allow" it to proceed.
            if (rule.conditions.isNotEmpty()) {
                val conditionsPass = when (rule.conditionMode) {
                    ConditionMode.ALL -> rule.conditions.all { it.evaluate(context) }
                    ConditionMode.ANY -> rule.conditions.any { it.evaluate(context) }
                }
                
                if (!conditionsPass) {
                    Log.d(TAG, "Rule '${rule.name}' trigger met, but conditions (ONLY IF) blocked it")
                    return
                }
            }

            // 3. Execution
            Log.i(TAG, "Rule '${rule.name}' EXECUTING — Event: ${eventType?.name ?: "CHECK"}")
            
            app.ruleRepository.recordTrigger(rule.id)

            // Pass additional state to actions (like current flashlight state for toggles)
            val updatedActions = rule.actions.map { action ->
                if (action.type == ActionType.TOGGLE_FLASHLIGHT) {
                    // We can't easily get flashlight state from TriggerContext yet, 
                    // but we can pass a dummy "toggle" signal or improve TriggerContext later.
                    action
                } else action
            }

            for (action in updatedActions) {
                val result = try {
                    action.execute(appContext)
                } catch (e: Exception) {
                    ActionResult(false, "Action failed: ${e.message}")
                }
                
                // Log the action result to database
                app.logRepository.insert(LogEntry(
                    ruleId = rule.id,
                    ruleName = rule.name,
                    triggerDescription = rule.triggers.firstOrNull { it.checkCondition(context) }?.describe() ?: "Triggered",
                    actionSummary = action.describe(),
                    success = result.success,
                    timestamp = System.currentTimeMillis()
                ))

                Log.d(TAG, "Rule: ${rule.name} -> Action '${action.describe()}': ${result.message}")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Critical failure evaluating rule '${rule.name}'", e)
        }
    }
}
