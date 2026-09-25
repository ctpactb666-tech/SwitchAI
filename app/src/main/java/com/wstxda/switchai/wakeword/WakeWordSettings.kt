package com.wstxda.switchai.wakeword

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.preference.PreferenceManager

/**
 * Keys and helpers for the "Голосовая активация" screen.
 */
object WakeWordSettings {
    const val KEY_ENABLED = "wake_word_enabled"
    const val KEY_LOCKED_SCREEN = "wake_locked_screen"
    const val KEY_BLUETOOTH = "wake_bluetooth"
    const val KEY_SOUND = "wake_sound_feedback"
    const val KEY_VIBRATION = "wake_vibration_feedback"
    const val KEY_SENSITIVITY = "wake_sensitivity"
    const val KEY_OWNER_ONLY = "owner_voice_only"

    private fun prefs(context: Context) = PreferenceManager.getDefaultSharedPreferences(context)

    fun isEnabled(context: Context) = prefs(context).getBoolean(KEY_ENABLED, false)
    fun setEnabled(context: Context, value: Boolean) =
        prefs(context).edit { putBoolean(KEY_ENABLED, value) }

    fun getBoolean(context: Context, key: String, default: Boolean = true) =
        prefs(context).getBoolean(key, default)

    fun putBoolean(context: Context, key: String, value: Boolean) =
        prefs(context).edit { putBoolean(key, value) }

    /** 0 = low sensitivity (strict), 1 = high sensitivity (triggers easily). */
    fun sensitivity(context: Context): Float =
        prefs(context).getFloat(KEY_SENSITIVITY, 0.5f).coerceIn(0f, 1f)

    fun setSensitivity(context: Context, value: Float) =
        prefs(context).edit { putFloat(KEY_SENSITIVITY, value.coerceIn(0f, 1f)) }

    /** Owner-voice similarity threshold derived from sensitivity: 0.90 (strict) … 0.60 (loose). */
    fun ownerThreshold(context: Context): Float = 0.90f - sensitivity(context) * 0.30f

    fun hasMicPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED

    fun startService(context: Context) {
        ContextCompat.startForegroundService(context, Intent(context, WakeWordService::class.java))
    }

    fun stopService(context: Context) {
        context.stopService(Intent(context, WakeWordService::class.java))
    }

    /** Starts the service if the user enabled activation but it is not running (e.g. after reboot). */
    fun syncService(context: Context) {
        if (isEnabled(context) && hasMicPermission(context) &&
            WakeWordState.current == WakeWordState.Status.STOPPED
        ) {
            runCatching { startService(context) }
        }
    }
}
