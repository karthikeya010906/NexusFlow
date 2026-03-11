package com.nexusflow

import android.app.Application
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.room.Room
import com.nexusflow.core.engine.AutomationEngine
import com.nexusflow.data.db.AppDatabase
import com.nexusflow.data.repository.LogRepository
import com.nexusflow.data.repository.RuleRepository
import com.nexusflow.service.AutomationService
import com.nexusflow.worker.AutomationWorker

class NexusFlowApp : Application() {

    lateinit var database: AppDatabase
    lateinit var ruleRepository: RuleRepository
    lateinit var logRepository: LogRepository
    lateinit var engine: AutomationEngine

    override fun onCreate() {
        super.onCreate()
        instance = this

        database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            AppDatabase.DB_NAME
        ).fallbackToDestructiveMigration().build()

        ruleRepository = RuleRepository(database.ruleDao())
        logRepository = LogRepository(database.logDao())
        engine = AutomationEngine(applicationContext)

        // Start the background service for real-time monitoring
        startAutomationService()
        
        // Start periodic automation worker
        AutomationWorker.enqueue(this)
        Log.i("NexusFlowApp", "App initialized and Services started")
    }

    private fun startAutomationService() {
        val intent = Intent(this, AutomationService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    companion object {
        private var instance: NexusFlowApp? = null
        fun getInstance(): NexusFlowApp {
            return instance ?: throw IllegalStateException("NexusFlowApp not initialized")
        }
    }
}