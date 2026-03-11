package com.nexusflow.core.model

interface Condition {
    val type: ConditionType
    val parameters: Map<String, String>
    fun evaluate(context: TriggerContext): Boolean
    fun describe(): String
}

enum class ConditionType(val displayName: String) {
    BATTERY_BELOW("Battery Below %"),
    BATTERY_ABOVE("Battery Above %"),
    TIME_BETWEEN("Time Between"),
    WIFI_IS("Wi-Fi Is"),
    SCREEN_STATE("Screen State")
}