package com.nexusflow.core.triggers

import com.nexusflow.core.model.Trigger
import com.nexusflow.core.model.TriggerContext
import com.nexusflow.core.model.TriggerType
import java.util.Calendar

class BatteryLevelTrigger(override val parameters: Map<String, String>) : Trigger {
    override val type = TriggerType.BATTERY_LEVEL
    private val threshold: Int get() = parameters["threshold"]?.toIntOrNull() ?: 20
    private val operator: String get() = parameters["operator"] ?: "below"
    override fun checkCondition(context: TriggerContext): Boolean = when (operator) {
        "below" -> context.batteryLevel < threshold
        "above" -> context.batteryLevel > threshold
        else -> context.batteryLevel == threshold
    }
    override fun describe() = "Battery $operator $threshold%"
}

class SimpleTrigger(override val type: TriggerType, val desc: String, val check: (TriggerContext) -> Boolean) : Trigger {
    override val parameters: Map<String, String> = emptyMap()
    override fun checkCondition(context: TriggerContext) = check(context)
    override fun describe() = desc
}

class TimeOfDayTrigger(override val parameters: Map<String, String>) : Trigger {
    override val type = TriggerType.TIME_OF_DAY
    private val hour: Int get() = parameters["hour"]?.toIntOrNull() ?: 8
    private val minute: Int get() = parameters["minute"]?.toIntOrNull() ?: 0
    
    override fun checkCondition(context: TriggerContext): Boolean {
        val cal = Calendar.getInstance().apply { timeInMillis = context.currentTimeMillis }
        val currentHour = cal.get(Calendar.HOUR_OF_DAY)
        val currentMinute = cal.get(Calendar.MINUTE)
        
        // Use a 15-minute window for periodic workers, or exact for real-time
        // If the context is very fresh (within 1 min of now), check exact.
        // Otherwise, allow a window.
        val diff = Math.abs((currentHour * 60 + currentMinute) - (hour * 60 + minute))
        return diff <= 15 // 15 minute window to match WorkManager interval
    }
    override fun describe() = "At %02d:%02d".format(hour, minute)
}

object TriggerFactory {
    fun create(type: TriggerType, parameters: Map<String, String>): Trigger = when (type) {
        TriggerType.BATTERY_LEVEL -> BatteryLevelTrigger(parameters)
        TriggerType.TIME_OF_DAY -> TimeOfDayTrigger(parameters)
        TriggerType.BATTERY_CHARGING -> SimpleTrigger(type, "Charger Connected") { it.isCharging }
        TriggerType.BATTERY_DISCHARGING -> SimpleTrigger(type, "Charger Disconnected") { !it.isCharging }
        TriggerType.SCREEN_ON -> SimpleTrigger(type, "Screen On") { it.isScreenOn }
        TriggerType.SCREEN_OFF -> SimpleTrigger(type, "Screen Off") { !it.isScreenOn }
        TriggerType.WIFI_CONNECTED -> SimpleTrigger(type, "Wi-Fi Connected") { it.isWifiConnected }
        TriggerType.WIFI_DISCONNECTED -> SimpleTrigger(type, "Wi-Fi Disconnected") { !it.isWifiConnected }
        TriggerType.BLUETOOTH_CONNECTED -> SimpleTrigger(type, "Bluetooth Connected") { it.isBluetoothConnected }
        TriggerType.BLUETOOTH_DISCONNECTED -> SimpleTrigger(type, "Bluetooth Disconnected") { !it.isBluetoothConnected }
        TriggerType.DND_ON -> SimpleTrigger(type, "DND On") { it.isDndEnabled }
        TriggerType.DND_OFF -> SimpleTrigger(type, "DND Off") { !it.isDndEnabled }
        TriggerType.HEADPHONES_PLUGGED -> SimpleTrigger(type, "Headphones Plugged") { it.isHeadphonesPlugged }
        TriggerType.HEADPHONES_UNPLUGGED -> SimpleTrigger(type, "Headphones Unplugged") { !it.isHeadphonesPlugged }
        TriggerType.APP_OPENED -> SimpleTrigger(type, "App Opened") { it.foregroundAppPackage == parameters["package"] }
        TriggerType.APP_CLOSED -> SimpleTrigger(type, "App Closed") { it.foregroundAppPackage != parameters["package"] }
        TriggerType.INCOMING_CALL -> SimpleTrigger(type, "Incoming Call") { it.incomingNumber.isNotEmpty() }
        TriggerType.SMS_RECEIVED -> SimpleTrigger(type, "SMS Received") { it.smsSender.isNotEmpty() }
        TriggerType.NFC_DETECTED -> SimpleTrigger(type, "NFC Detected") { true }
        TriggerType.AIRPLANE_MODE_ON -> SimpleTrigger(type, "Airplane Mode") { it.isAirplaneModeOn }
        TriggerType.DRIVING_MODE -> SimpleTrigger(type, "Driving Mode") { it.isDriving }
    }
}
