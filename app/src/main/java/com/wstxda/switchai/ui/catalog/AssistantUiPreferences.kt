package com.wstxda.switchai.ui.catalog

import android.content.Context
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.wstxda.switchai.utils.Constants

/**
 * Per-assistant options from the "Настройки ассистента" screen.
 */
class AssistantUiPreferences(context: Context) {

    private val prefs = PreferenceManager.getDefaultSharedPreferences(context)

    /** "Запускать голосовой режим" — overrides the global voice-input setting. */
    fun voiceMode(key: String): Boolean =
        if (prefs.contains(voiceModeKey(key))) {
            prefs.getBoolean(voiceModeKey(key), true)
        } else {
            prefs.getBoolean(Constants.VOICE_INPUT_PREF_KEY, true)
        }

    fun setVoiceMode(key: String, value: Boolean) =
        prefs.edit { putBoolean(voiceModeKey(key), value) }

    /** Returns the explicit per-assistant value, or null when the user never changed it. */
    fun voiceModeOverride(key: String): Boolean? =
        if (prefs.contains(voiceModeKey(key))) prefs.getBoolean(voiceModeKey(key), true) else null

    /** "Использовать голосовой ввод" — the assistant may be launched by activation phrases. */
    fun wakeEnabled(key: String): Boolean = prefs.getBoolean(wakeKey(key), true)
    fun setWakeEnabled(key: String, value: Boolean) = prefs.edit { putBoolean(wakeKey(key), value) }

    /** "Автоматически начинать разговор" — launch right away instead of a tap-to-start notification. */
    fun autoConversation(key: String): Boolean = prefs.getBoolean(autoKey(key), true)
    fun setAutoConversation(key: String, value: Boolean) = prefs.edit { putBoolean(autoKey(key), value) }

    /** "Показывать приоритетно" — pinned to the top of lists and the home grid. */
    fun isPriority(key: String): Boolean = key in prefs.getStringSet(KEY_PRIORITY, emptySet()).orEmpty()
    fun setPriority(key: String, value: Boolean) = updateSet(KEY_PRIORITY, key, value)

    /** "Удалить из списка" — hides the assistant from the UI (the app itself is untouched). */
    fun hidden(): Set<String> = prefs.getStringSet(KEY_HIDDEN, emptySet()).orEmpty()
    fun setHidden(key: String, value: Boolean) = updateSet(KEY_HIDDEN, key, value)
    fun clearHidden() = prefs.edit { remove(KEY_HIDDEN) }

    private fun updateSet(prefKey: String, key: String, add: Boolean) {
        val current = prefs.getStringSet(prefKey, emptySet()).orEmpty().toMutableSet()
        if (add) current.add(key) else current.remove(key)
        prefs.edit { putStringSet(prefKey, current) }
    }

    private fun voiceModeKey(key: String) = "assistant_${key}_voice_mode"
    private fun wakeKey(key: String) = "assistant_${key}_wake_enabled"
    private fun autoKey(key: String) = "assistant_${key}_auto_conversation"

    companion object {
        private const val KEY_PRIORITY = "assistant_priority_set"
        private const val KEY_HIDDEN = "assistant_hidden_set"
    }
}
