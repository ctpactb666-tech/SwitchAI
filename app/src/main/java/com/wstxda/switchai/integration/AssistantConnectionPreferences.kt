package com.wstxda.switchai.integration

import android.content.Context
import androidx.core.content.edit
import androidx.preference.PreferenceManager

class AssistantConnectionPreferences(context: Context) {
    private val prefs = PreferenceManager.getDefaultSharedPreferences(context)

    fun getMode(assistantKey: String): AssistantConnectionMode =
        AssistantConnectionMode.from(
            prefs.getString(modeKey(assistantKey), AssistantConnectionMode.AUTO.value)
        )

    fun setMode(assistantKey: String, mode: AssistantConnectionMode) {
        prefs.edit { putString(modeKey(assistantKey), mode.value) }
    }

    fun isEmbeddedSessionConnected(assistantKey: String): Boolean =
        prefs.getBoolean(connectedKey(assistantKey), false)

    fun setEmbeddedSessionConnected(assistantKey: String, connected: Boolean) {
        prefs.edit { putBoolean(connectedKey(assistantKey), connected) }
    }

    private fun modeKey(assistantKey: String) = "assistant_connection_mode_$assistantKey"
    private fun connectedKey(assistantKey: String) = "assistant_embedded_connected_$assistantKey"
}
