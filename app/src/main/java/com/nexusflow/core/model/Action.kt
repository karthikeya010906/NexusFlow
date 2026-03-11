package com.nexusflow.core.model

import android.content.Context

interface Action {
    val type: ActionType
    val parameters: Map<String, String>
    suspend fun execute(context: Context): ActionResult
    fun describe(): String
}

enum class ActionType(val displayName: String, val iconRes: Int) {
    SHOW_NOTIFICATION("Show Notification", android.R.drawable.ic_dialog_info),
    SET_BRIGHTNESS("Set Brightness", android.R.drawable.ic_menu_today),
    SET_VOLUME("Set Volume", android.R.drawable.ic_lock_silent_mode_off),
    VIBRATE("Vibrate", android.R.drawable.ic_menu_call),
    SET_WIFI("Wi-Fi", android.R.drawable.ic_menu_share),
    SET_BLUETOOTH("Bluetooth", android.R.drawable.ic_menu_share),
    SET_FLASHLIGHT("Flashlight", android.R.drawable.ic_menu_camera),
    TOGGLE_FLASHLIGHT("Toggle Flashlight", android.R.drawable.ic_menu_camera),
    SET_DND("DND Mode", android.R.drawable.ic_lock_silent_mode),
    SET_SOUND_MODE("Sound Mode", android.R.drawable.ic_lock_silent_mode_off),
    LAUNCH_APP("Launch App", android.R.drawable.ic_menu_view),
    OPEN_WEBSITE("Open Website", android.R.drawable.ic_menu_mapmode),
    PLAY_ALARM("Play Alarm", android.R.drawable.ic_lock_idle_alarm),
    SPEAK_TEXT("Speak Text", android.R.drawable.ic_btn_speak_now),
    MEDIA_CONTROL("Media Control", android.R.drawable.ic_media_play),
    TOGGLE_DARK_MODE("Dark Mode", android.R.drawable.ic_menu_day),
    SET_HOTSPOT("Hotspot", android.R.drawable.ic_menu_share),
    LOG_EVENT("Log Event", android.R.drawable.ic_menu_info_details),
    SEND_BROADCAST("Send Broadcast", android.R.drawable.ic_menu_send)
}

data class ActionResult(
    val success: Boolean,
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
)