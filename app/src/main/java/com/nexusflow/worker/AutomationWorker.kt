package com.nexusflow.worker

import android.content.Context
import android.util.Log
import androidx.work.*
import com.nexusflow.NexusFlowApp
import com.nexusflow.core.model.TriggerContextFactory
import com.nexusflow.core.model.TriggerType
import java.util.concurrent.TimeUnit

private const val TAG = "AutomationWorker"

class AutomationWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        Log.d(TAG, "AutomationWorker running periodic check")

        return try {
            val engine = NexusFlowApp.getInstance().engine
            val ctx = TriggerContextFactory.build(applicationContext)
            
            // The worker primarily handles time-based triggers that don't have a system broadcast
            engine.evaluate(ctx, TriggerType.TIME_OF_DAY)
            
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "AutomationWorker failed", e)
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "nexusflow_automation_worker"

        fun enqueue(context: Context) {
            val request = PeriodicWorkRequestBuilder<AutomationWorker>(
                repeatInterval = 15,
                repeatIntervalTimeUnit = TimeUnit.MINUTES
            ).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
            Log.d(TAG, "AutomationWorker enqueued/updated")
        }
    }
}
