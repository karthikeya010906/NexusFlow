package com.nexusflow.core.model

import android.app.NotificationManager
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.display.DisplayManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.view.Display

object TriggerContextFactory {

    fun build(context: Context): TriggerContext {
        val appContext = context.applicationContext
        
        val batteryManager = appContext.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val batteryLevel = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        val isCharging = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            batteryManager.isCharging
        } else {
            val intent = appContext.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
        }

        val displayManager = appContext.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        val isScreenOn = displayManager.displays.any { it.state == Display.STATE_ON }

        val connManager = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connManager.activeNetwork
        val caps = connManager.getNetworkCapabilities(network)
        val isWifi = caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        
        var ssid = ""
        if (isWifi) {
            val wifiManager = appContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val info = wifiManager.connectionInfo
            if (info != null && info.networkId != -1) {
                ssid = info.ssid.removePrefix("\"").removeSuffix("\"")
            }
        }

        val notificationManager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val isDnd = notificationManager.currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL

        val headsetIntent = appContext.registerReceiver(null, IntentFilter(Intent.ACTION_HEADSET_PLUG))
        val isHeadphones = headsetIntent?.getIntExtra("state", 0) == 1

        // Bluetooth state
        val bluetoothManager = appContext.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val isBluetooth = bluetoothManager.adapter?.let { adapter ->
            adapter.isEnabled && (
                adapter.getProfileConnectionState(BluetoothProfile.A2DP) == BluetoothProfile.STATE_CONNECTED ||
                adapter.getProfileConnectionState(BluetoothProfile.HEADSET) == BluetoothProfile.STATE_CONNECTED
            )
        } ?: false

        return TriggerContext(
            batteryLevel = batteryLevel,
            isCharging = isCharging,
            isScreenOn = isScreenOn,
            isWifiConnected = isWifi,
            wifiSsid = ssid,
            isHeadphonesPlugged = isHeadphones,
            isDndEnabled = isDnd,
            isBluetoothConnected = isBluetooth,
            currentTimeMillis = System.currentTimeMillis()
        )
    }
}
