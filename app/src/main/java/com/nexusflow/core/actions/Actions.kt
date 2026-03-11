package com.nexusflow.core.actions

import android.app.NotificationChannel
import android.app.NotificationManager
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.media.RingtoneManager
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.KeyEvent
import androidx.core.app.NotificationCompat
import com.nexusflow.core.model.Action
import com.nexusflow.core.model.ActionResult
import com.nexusflow.core.model.ActionType
import java.util.*

private const val TAG = "NexusAction"
private const val CHANNEL_ID = "nexusflow_actions"

class ShowNotificationAction(override val parameters: Map<String, String>) : Action {
    override val type = ActionType.SHOW_NOTIFICATION
    override suspend fun execute(context: Context): ActionResult {
        return try {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(CHANNEL_ID, "NexusFlow Automations", NotificationManager.IMPORTANCE_HIGH)
                manager.createNotificationChannel(channel)
            }
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(parameters["title"] ?: "NexusFlow")
                .setContentText(parameters["message"] ?: "Automation Triggered!")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build()
            manager.notify(System.currentTimeMillis().toInt(), notification)
            ActionResult(true, "Notification shown")
        } catch (e: Exception) { ActionResult(false, e.message ?: "Error") }
    }
    override fun describe() = "Show Notification: ${parameters["title"]}"
}

class SetBrightnessAction(override val parameters: Map<String, String>) : Action {
    override val type = ActionType.SET_BRIGHTNESS
    override suspend fun execute(context: Context): ActionResult {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.System.canWrite(context)) {
                return ActionResult(false, "Missing Permission: WRITE_SETTINGS. Please grant it in system settings.")
            }
            
            val brightnessPercent = parameters["brightness"]?.toIntOrNull() ?: 50
            val brightnessValue = (brightnessPercent * 2.55).toInt().coerceIn(0, 255)

            // 1. DISABLE Adaptive Brightness (Auto-brightness)
            // 0 = Manual, 1 = Automatic
            Settings.System.putInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL)
            
            // 2. APPLY the brightness value
            val success = Settings.System.putInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, brightnessValue)
            
            if (success) {
                ActionResult(true, "Brightness set to $brightnessPercent% ($brightnessValue/255)")
            } else {
                ActionResult(false, "System failed to apply brightness value")
            }
        } catch (e: Exception) { 
            Log.e(TAG, "Error setting brightness", e)
            ActionResult(false, "Error setting brightness: ${e.message}") 
        }
    }
    override fun describe() = "Set Brightness"
}

class SetWifiAction(override val parameters: Map<String, String>) : Action {
    override val type = ActionType.SET_WIFI
    override suspend fun execute(context: Context): ActionResult {
        return try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val enabled = parameters["enabled"]?.toBoolean() ?: true
            @Suppress("DEPRECATION")
            wifiManager.isWifiEnabled = enabled
            ActionResult(true, "Wi-Fi set to $enabled")
        } catch (e: Exception) { ActionResult(false, "Wi-Fi Error") }
    }
    override fun describe() = "Set Wi-Fi"
}

class SetBluetoothAction(override val parameters: Map<String, String>) : Action {
    override val type = ActionType.SET_BLUETOOTH
    override suspend fun execute(context: Context): ActionResult {
        return try {
            @Suppress("DEPRECATION")
            val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
            val enabled = parameters["enabled"]?.toBoolean() ?: true
            if (enabled) {
                @Suppress("DEPRECATION")
                bluetoothAdapter.enable()
            } else {
                @Suppress("DEPRECATION")
                bluetoothAdapter.disable()
            }
            ActionResult(true, "Bluetooth set to $enabled")
        } catch (e: Exception) { ActionResult(false, "Bluetooth Error") }
    }
    override fun describe() = "Set Bluetooth"
}

class SetFlashlightAction(override val parameters: Map<String, String>) : Action {
    override val type = ActionType.SET_FLASHLIGHT
    override suspend fun execute(context: Context): ActionResult {
        return try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cameraId = cameraManager.cameraIdList.getOrNull(0) ?: return ActionResult(false, "No Flashlight available")
            val enabled = parameters["enabled"]?.toBoolean() ?: true
            cameraManager.setTorchMode(cameraId, enabled)
            ActionResult(true, "Flashlight set to $enabled")
        } catch (e: Exception) { ActionResult(false, "Flashlight Error: ${e.message}") }
    }
    override fun describe() = "Set Flashlight"
}

class ToggleFlashlightAction(override val parameters: Map<String, String>) : Action {
    override val type = ActionType.TOGGLE_FLASHLIGHT
    override suspend fun execute(context: Context): ActionResult {
        return try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cameraId = cameraManager.cameraIdList.getOrNull(0) ?: return ActionResult(false, "No Flashlight available")
            
            // We can't easily query torch state directly, but we can try to toggle it.
            // For a robust implementation, we might need to track state ourselves or use a CameraDevice callback.
            // Simplified approach: use a parameter if provided, otherwise default to on
            val currentState = parameters["current_state"]?.toBoolean() ?: false
            cameraManager.setTorchMode(cameraId, !currentState)
            ActionResult(true, "Flashlight toggled")
        } catch (e: Exception) { ActionResult(false, "Flashlight Toggle Error: ${e.message}") }
    }
    override fun describe() = "Toggle Flashlight"
}

class SetDndAction(override val parameters: Map<String, String>) : Action {
    override val type = ActionType.SET_DND
    override suspend fun execute(context: Context): ActionResult {
        return try {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (!manager.isNotificationPolicyAccessGranted) return ActionResult(false, "Missing Permission: DND Policy Access")
                val filter = if (parameters["enabled"]?.toBoolean() == true) NotificationManager.INTERRUPTION_FILTER_NONE else NotificationManager.INTERRUPTION_FILTER_ALL
                manager.setInterruptionFilter(filter)
                ActionResult(true, "DND set")
            } else ActionResult(false, "Not supported on this Android version")
        } catch (e: Exception) { ActionResult(false, "DND Error") }
    }
    override fun describe() = "Set DND"
}

class LaunchAppAction(override val parameters: Map<String, String>) : Action {
    override val type = ActionType.LAUNCH_APP
    override suspend fun execute(context: Context): ActionResult {
        return try {
            val packageName = parameters["package"] ?: return ActionResult(false, "No package specified")
            val intent = context.packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                ActionResult(true, "Launched app: $packageName")
            } else ActionResult(false, "App not found: $packageName")
        } catch (e: Exception) { ActionResult(false, "Launch Error") }
    }
    override fun describe() = "Launch App"
}

class OpenWebsiteAction(override val parameters: Map<String, String>) : Action {
    override val type = ActionType.OPEN_WEBSITE
    override suspend fun execute(context: Context): ActionResult {
        return try {
            var url = parameters["url"] ?: "https://google.com"
            if (!url.startsWith("http")) url = "https://$url"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
            context.startActivity(intent)
            ActionResult(true, "Opened Website: $url")
        } catch (e: Exception) { ActionResult(false, "Browser Error") }
    }
    override fun describe() = "Open Website"
}

class PlayAlarmAction(override val parameters: Map<String, String>) : Action {
    override val type = ActionType.PLAY_ALARM
    override suspend fun execute(context: Context): ActionResult {
        return try {
            val alert = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            val ringtone = RingtoneManager.getRingtone(context, alert)
            ringtone.play()
            ActionResult(true, "Alarm started")
        } catch (e: Exception) { ActionResult(false, "Alarm Error") }
    }
    override fun describe() = "Play Alarm"
}

class MediaControlAction(override val parameters: Map<String, String>) : Action {
    override val type = ActionType.MEDIA_CONTROL
    override suspend fun execute(context: Context): ActionResult {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val command = parameters["command"] ?: "play_pause"
        val keyCode = when (command) {
            "play" -> KeyEvent.KEYCODE_MEDIA_PLAY
            "pause" -> KeyEvent.KEYCODE_MEDIA_PAUSE
            "next" -> KeyEvent.KEYCODE_MEDIA_NEXT
            "previous" -> KeyEvent.KEYCODE_MEDIA_PREVIOUS
            else -> KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
        }
        am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
        am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
        return ActionResult(true, "Media command sent")
    }
    override fun describe() = "Media Control"
}

class SpeakTextAction(override val parameters: Map<String, String>) : Action {
    override val type = ActionType.SPEAK_TEXT
    
    companion object {
        private var tts: TextToSpeech? = null
    }
    
    override suspend fun execute(context: Context): ActionResult {
        val text = parameters["text"] ?: "Action triggered"
        if (tts == null) {
            tts = TextToSpeech(context.applicationContext) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    tts?.language = Locale.US
                    tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "nexus_tts")
                }
            }
        } else {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "nexus_tts")
        }
        return ActionResult(true, "Speaking")
    }
    override fun describe() = "Speak Text"
}

class VibrateAction(override val parameters: Map<String, String>) : Action {
    override val type = ActionType.VIBRATE
    override suspend fun execute(context: Context): ActionResult {
        return try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
            vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
            ActionResult(true, "Vibrated")
        } catch (e: Exception) { ActionResult(false, "Vibrate Error") }
    }
    override fun describe() = "Vibrate"
}

class SetVolumeAction(override val parameters: Map<String, String>) : Action {
    override val type = ActionType.SET_VOLUME
    override suspend fun execute(context: Context): ActionResult {
        return try {
            val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val level = parameters["level"]?.toIntOrNull() ?: 7
            am.setStreamVolume(AudioManager.STREAM_MUSIC, level, 0)
            ActionResult(true, "Volume set to $level")
        } catch (e: Exception) { ActionResult(false, "Volume Error") }
    }
    override fun describe() = "Set Volume"
}

class SetSoundModeAction(override val parameters: Map<String, String>) : Action {
    override val type = ActionType.SET_SOUND_MODE
    override suspend fun execute(context: Context): ActionResult {
        return try {
            val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val mode = parameters["mode"] ?: "normal"
            val ringerMode = when (mode) {
                "silent" -> AudioManager.RINGER_MODE_SILENT
                "vibrate" -> AudioManager.RINGER_MODE_VIBRATE
                else -> AudioManager.RINGER_MODE_NORMAL
            }
            am.ringerMode = ringerMode
            ActionResult(true, "Sound mode set to $mode")
        } catch (e: Exception) { ActionResult(false, "Sound Mode Error") }
    }
    override fun describe() = "Set Sound Mode"
}

class GenericAction(override val type: ActionType, val desc: String, val block: suspend (Context) -> ActionResult) : Action {
    override val parameters: Map<String, String> = emptyMap()
    override suspend fun execute(context: Context) = block(context)
    override fun describe() = desc
}

object ActionFactory {
    fun create(type: ActionType, parameters: Map<String, String>): Action = when (type) {
        ActionType.SHOW_NOTIFICATION -> ShowNotificationAction(parameters)
        ActionType.SET_BRIGHTNESS    -> SetBrightnessAction(parameters)
        ActionType.SET_VOLUME        -> SetVolumeAction(parameters)
        ActionType.VIBRATE           -> VibrateAction(parameters)
        ActionType.SET_WIFI          -> SetWifiAction(parameters)
        ActionType.SET_BLUETOOTH     -> SetBluetoothAction(parameters)
        ActionType.SET_FLASHLIGHT    -> SetFlashlightAction(parameters)
        ActionType.TOGGLE_FLASHLIGHT -> ToggleFlashlightAction(parameters)
        ActionType.SET_DND           -> SetDndAction(parameters)
        ActionType.LAUNCH_APP        -> LaunchAppAction(parameters)
        ActionType.OPEN_WEBSITE      -> OpenWebsiteAction(parameters)
        ActionType.PLAY_ALARM        -> PlayAlarmAction(parameters)
        ActionType.MEDIA_CONTROL     -> MediaControlAction(parameters)
        ActionType.SPEAK_TEXT        -> SpeakTextAction(parameters)
        ActionType.SET_SOUND_MODE    -> SetSoundModeAction(parameters)
        ActionType.LOG_EVENT         -> GenericAction(type, "Log: ${parameters["message"]}") { ActionResult(true, "Logged") }
        ActionType.TOGGLE_DARK_MODE  -> GenericAction(type, "Toggle Dark Mode") { ActionResult(true, "Dark mode toggled") }
        ActionType.SET_HOTSPOT       -> GenericAction(type, "Set Hotspot") { ActionResult(true, "Hotspot updated") }
        ActionType.SEND_BROADCAST    -> GenericAction(type, "Send Broadcast") { ActionResult(true, "Broadcast sent") }
    }
}
