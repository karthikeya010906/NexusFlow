package com.nexusflow.core.conditions

import com.nexusflow.core.model.Condition
import com.nexusflow.core.model.ConditionType
import com.nexusflow.core.model.TriggerContext
import java.util.Calendar

class BatteryBelowCondition(
    override val parameters: Map<String, String>
) : Condition {
    override val type = ConditionType.BATTERY_BELOW
    private val threshold: Int get() = parameters["threshold"]?.toIntOrNull() ?: 20
    override fun evaluate(context: TriggerContext) = context.batteryLevel < threshold
    override fun describe() = "Battery < $threshold%"
}

class BatteryAboveCondition(
    override val parameters: Map<String, String>
) : Condition {
    override val type = ConditionType.BATTERY_ABOVE
    private val threshold: Int get() = parameters["threshold"]?.toIntOrNull() ?: 80
    override fun evaluate(context: TriggerContext) = context.batteryLevel > threshold
    override fun describe() = "Battery > $threshold%"
}

class TimeBetweenCondition(
    override val parameters: Map<String, String>
) : Condition {
    override val type = ConditionType.TIME_BETWEEN
    private val startHour: Int get() = parameters["startHour"]?.toIntOrNull() ?: 9
    private val startMinute: Int get() = parameters["startMinute"]?.toIntOrNull() ?: 0
    private val endHour: Int get() = parameters["endHour"]?.toIntOrNull() ?: 17
    private val endMinute: Int get() = parameters["endMinute"]?.toIntOrNull() ?: 0

    override fun evaluate(context: TriggerContext): Boolean {
        val cal = Calendar.getInstance().apply { timeInMillis = context.currentTimeMillis }
        val now = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        val start = startHour * 60 + startMinute
        val end = endHour * 60 + endMinute
        return if (start <= end) now in start..end
        else now >= start || now <= end
    }
    override fun describe() = "Between %02d:%02d and %02d:%02d".format(startHour, startMinute, endHour, endMinute)
}

class WifiIsCondition(
    override val parameters: Map<String, String>
) : Condition {
    override val type = ConditionType.WIFI_IS
    private val shouldBeConnected: Boolean get() = parameters["connected"] != "false"
    private val targetSsid: String get() = parameters["ssid"] ?: ""

    override fun evaluate(context: TriggerContext): Boolean {
        if (shouldBeConnected != context.isWifiConnected) return false
        return targetSsid.isEmpty() || context.wifiSsid == targetSsid
    }
    override fun describe() = "Wi-Fi ${if (shouldBeConnected) "Connected" else "Disconnected"}"
}

class ScreenStateCondition(
    override val parameters: Map<String, String>
) : Condition {
    override val type = ConditionType.SCREEN_STATE
    private val shouldBeOn: Boolean get() = parameters["state"] != "off"
    override fun evaluate(context: TriggerContext) = context.isScreenOn == shouldBeOn
    override fun describe() = "Screen is ${if (shouldBeOn) "On" else "Off"}"
}

object ConditionFactory {
    fun create(type: ConditionType, parameters: Map<String, String>): Condition = when (type) {
        ConditionType.BATTERY_BELOW  -> BatteryBelowCondition(parameters)
        ConditionType.BATTERY_ABOVE  -> BatteryAboveCondition(parameters)
        ConditionType.TIME_BETWEEN   -> TimeBetweenCondition(parameters)
        ConditionType.WIFI_IS        -> WifiIsCondition(parameters)
        ConditionType.SCREEN_STATE   -> ScreenStateCondition(parameters)
    }
}