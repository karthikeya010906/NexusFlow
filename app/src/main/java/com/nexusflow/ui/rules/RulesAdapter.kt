package com.nexusflow.ui.rules

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.setPadding
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.nexusflow.R
import com.nexusflow.core.model.Rule
import com.nexusflow.databinding.ItemRuleFlowcardBinding
import java.text.SimpleDateFormat
import java.util.*

class RulesAdapter(
    private val onToggle: (String, Boolean) -> Unit,
    private val onClick: (Rule) -> Unit
) : ListAdapter<Rule, RulesAdapter.ViewHolder>(DiffCallback) {

    private val dateFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemRuleFlowcardBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemRuleFlowcardBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(rule: Rule) {
            val context = binding.root.context
            binding.ruleName.text = rule.name
            
            // Critical fix: Remove listener before setting state to avoid recursion/loops
            binding.ruleToggle.setOnCheckedChangeListener(null)
            binding.ruleToggle.isChecked = rule.isEnabled
            
            // Trigger Pill
            binding.ruleTriggersSummary.text = "WHEN " + (rule.triggers.firstOrNull()?.describe() ?: "No Triggers")
            
            // Clear and build Action Pills
            binding.actionsPillContainer.removeAllViews()
            rule.actions.forEach { action ->
                val card = MaterialCardView(context).apply {
                    radius = 12f * context.resources.displayMetrics.density
                    cardElevation = 0f
                    val typedValue = android.util.TypedValue()
                    context.theme.resolveAttribute(com.google.android.material.R.attr.colorSurfaceVariant, typedValue, true)
                    setCardBackgroundColor(typedValue.data)
                    
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        setMargins(0, 0, (8 * context.resources.displayMetrics.density).toInt(), 0)
                    }
                }
                
                val textView = TextView(context).apply {
                    text = "→ " + action.describe()
                    textSize = 12f
                    val typedValue = android.util.TypedValue()
                    context.theme.resolveAttribute(com.google.android.material.R.attr.colorPrimary, typedValue, true)
                    setTextColor(typedValue.data)
                    setPadding((12 * context.resources.displayMetrics.density).toInt())
                }
                
                card.addView(textView)
                binding.actionsPillContainer.addView(card)
            }
            
            val lastTriggered = rule.lastTriggeredAt ?: 0L
            val timeStr = if (lastTriggered > 0) dateFormat.format(Date(lastTriggered)) else "Never"
            binding.ruleStats.text = "Fired ${rule.triggerCount}x • Last: $timeStr"

            // Re-attach listener correctly
            binding.ruleToggle.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked != rule.isEnabled) {
                    onToggle(rule.id, isChecked)
                }
            }

            binding.root.setOnClickListener {
                onClick(rule)
            }
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<Rule>() {
        override fun areItemsTheSame(oldItem: Rule, newItem: Rule): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Rule, newItem: Rule): Boolean {
            return oldItem == newItem
        }
    }
}