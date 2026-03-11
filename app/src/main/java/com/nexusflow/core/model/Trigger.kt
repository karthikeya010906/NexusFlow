package com.nexusflow.core.model

interface Trigger {
    val type: TriggerType
    val parameters: Map<String, String>
    fun checkCondition(context: TriggerContext): Boolean
    fun describe(): String
}

enum class TriggerType(val displayName: String, val iconRes: Int) {
    BATTERY_LEVEL("Battery Level", android.R.drawable.ic_lock_idle_low_battery),
    BATTERY_CHARGING("Charger Connected", android.R.drawable.ic_input_add),
    BATTERY_DISCHARGING("Charger Disconnected", android.R.drawable.ic_delete),
    TIME_OF_DAY("Specific Time", android.R.drawable.ic_menu_recent_history),
    SCREEN_ON("Screen On", android.R.drawable.ic_menu_view),
    SCREEN_OFF("Screen Off", android.R.drawable.ic_lock_power_off),
    WIFI_CONNECTED("Wi-Fi Connected", android.R.drawable.ic_menu_share),
    WIFI_DISCONNECTED("Wi-Fi Disconnected", android.R.drawable.ic_menu_close_clear_cancel),
    BLUETOOTH_CONNECTED("Bluetooth Connected", android.R.drawable.ic_menu_share),
    BLUETOOTH_DISCONNECTED("Bluetooth Disconnected", android.R.drawable.ic_menu_close_clear_cancel),
    HEADPHONES_PLUGGED("Headphones Plugged", android.R.drawable.ic_menu_call),
    HEADPHONES_UNPLUGGED("Headphones Unplugged", android.R.drawable.ic_menu_close_clear_cancel),
    APP_OPENED("App Opened", android.R.drawable.ic_menu_compass),
    APP_CLOSED("App Closed", android.R.drawable.ic_menu_close_clear_cancel),
    DND_ON("DND Mode On", android.R.drawable.ic_lock_silent_mode),
    DND_OFF("DND Mode Off", android.R.drawable.ic_lock_silent_mode_off),
    INCOMING_CALL("Incoming Call", android.R.drawable.stat_sys_phone_call),
    SMS_RECEIVED("SMS Received", android.R.drawable.ic_dialog_email),
    NFC_DETECTED("NFC Tag Detected", android.R.drawable.ic_menu_share),
    AIRPLANE_MODE_ON("Airplane Mode On", android.R.drawable.ic_menu_send),
    DRIVING_MODE("Driving Mode", android.R.drawable.ic_menu_directions)
}

data class TriggerContext(
    val batteryLevel: Int = 100,
    val isCharging: Boolean = false,
    val currentTimeMillis: Long = System.currentTimeMillis(),
    val isScreenOn: Boolean = true,
    val isWifiConnected: Boolean = false,
    val wifiSsid: String = "",
    val isBluetoothConnected: Boolean = false,
    val bluetoothDeviceName: String = "",
    val isHeadphonesPlugged: Boolean = false,
    val foregroundAppPackage: String = "",
    val isDndEnabled: Boolean = false,
    val incomingNumber: String = "",
    val smsSender: String = "",
    val isAirplaneModeOn: Boolean = false,
    val isDriving: Boolean = false,
    val extra: Map<String, String> = emptyMap()
)