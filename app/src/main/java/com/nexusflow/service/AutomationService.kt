package com.nexusflow.service

import android.app.*
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.nexusflow.R
import com.nexusflow.service.SystemBroadcastReceiver

class AutomationService : Service() {

    private val receiver = SystemBroadcastReceiver()

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("NexusFlow Engine Active")
            .setContentText("Monitoring system events for automations...")
            .setSmallIcon(android.R.drawable.ic_lock_idle_low_battery)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
        
        startForeground(NOTIFICATION_ID, notification)

        // Register dynamic broadcasts
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
            addAction(Intent.ACTION_HEADSET_PLUG)
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            addAction(NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED)
            addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED)
            priority = IntentFilter.SYSTEM_HIGH_PRIORITY
        }
        registerReceiver(receiver, filter)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        try {
            unregisterReceiver(receiver)
        } catch (e: Exception) {
            // Already unregistered
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Automation Service Channel",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps the automation engine running in the background"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    companion object {
        const val CHANNEL_ID = "automation_service_channel"
        const val NOTIFICATION_ID = 1001
    }
}
