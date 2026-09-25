package com.wstxda.switchai.wakeword

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothHeadset
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

/**
 * Routes recognition to a connected Bluetooth headset when the user enabled it.
 * Modern Android uses communication-device routing; older releases fall back to SCO.
 */
class BluetoothWakeRouter(context: Context) {
    private val appContext = context.applicationContext
    private val audioManager = appContext.getSystemService(AudioManager::class.java)

    fun applyIfEnabled(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            ContextCompat.checkSelfPermission(appContext, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED
        ) return false
        if (!WakeWordSettings.getBoolean(appContext, WakeWordSettings.KEY_BLUETOOTH, false)) {
            clear()
            return false
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val device = audioManager.availableCommunicationDevices.firstOrNull {
                it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                    it.type == AudioDeviceInfo.TYPE_BLE_HEADSET
            } ?: return false
            return audioManager.setCommunicationDevice(device)
        }

        @Suppress("DEPRECATION")
        return runCatching {
            val adapter = BluetoothAdapter.getDefaultAdapter() ?: return@runCatching false
            if (adapter.getProfileConnectionState(BluetoothProfile.HEADSET) != BluetoothProfile.STATE_CONNECTED) {
                return@runCatching false
            }
            audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
            audioManager.startBluetoothSco()
            audioManager.isBluetoothScoOn = true
            true
        }.getOrDefault(false)
    }

    fun clear() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            ContextCompat.checkSelfPermission(appContext, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED
        ) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            audioManager.clearCommunicationDevice()
        } else {
            @Suppress("DEPRECATION")
            runCatching {
                audioManager.stopBluetoothSco()
                audioManager.isBluetoothScoOn = false
                audioManager.mode = AudioManager.MODE_NORMAL
            }
        }
    }
}
