package com.nexusflow.service

import android.app.NotificationManager
import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.nexusflow.NexusFlowApp
import com.nexusflow.core.model.TriggerContextFactory
import com.nexusflow.core.model.TriggerType

private const val TAG = "NexusBroadcastReceiver"

class SystemBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as NexusFlowApp
        val engine = app.engine
        
        val triggerType: TriggerType? = when (intent.action) {
            Intent.ACTION_POWER_CONNECTED -> TriggerType.BATTERY_CHARGING
            Intent.ACTION_POWER_DISCONNECTED -> TriggerType.BATTERY_DISCHARGING
            Intent.ACTION_BATTERY_CHANGED -> TriggerType.BATTERY_LEVEL
            Intent.ACTION_SCREEN_ON -> TriggerType.SCREEN_ON
            Intent.ACTION_SCREEN_OFF -> TriggerType.SCREEN_OFF
            Intent.ACTION_HEADSET_PLUG -> {
                val state = intent.getIntExtra("state", -1)
                if (state == 1) TriggerType.HEADPHONES_PLUGGED else if (state == 0) TriggerType.HEADPHONES_UNPLUGGED else null
            }
            BluetoothAdapter.ACTION_STATE_CHANGED -> {
                val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1)
                if (state == BluetoothAdapter.STATE_CONNECTED) TriggerType.BLUETOOTH_CONNECTED 
                else if (state == BluetoothAdapter.STATE_DISCONNECTED) TriggerType.BLUETOOTH_DISCONNECTED
                else null
            }
            NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED -> TriggerType.DND_ON
            Intent.ACTION_AIRPLANE_MODE_CHANGED -> TriggerType.AIRPLANE_MODE_ON
            else -> null
        }

        if (triggerType == null) return

        Log.d(TAG, "System event detected: ${intent.action} -> Mapping to: ${triggerType.name}")
        
        // Use the factory to build a consistent and complete context
        val triggerContext = TriggerContextFactory.build(context)
        engine.evaluate(triggerContext, triggerType)
    }
}
