package com.wstxda.switchai.wakeword

import android.content.Context
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import org.json.JSONArray
import org.json.JSONObject

class WakePhraseStore(context: Context) {
    private val preferences = PreferenceManager.getDefaultSharedPreferences(context)

    fun load(): List<WakePhrase> {
        val raw = preferences.getString(KEY, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val item = array.getJSONObject(i)
                    val phrase = item.getString("phrase")
                    val assistant = item.getString("assistant")
                    add(
                        WakePhrase(
                            phrase = phrase,
                            assistantKey = assistant,
                            languageTag = item.optString("language", "und"),
                            enabled = item.optBoolean("enabled", true),
                            exactMatch = item.optBoolean("exact", false),
                            ignoreWhenMusic = item.optBoolean("ignore_music", true),
                            offline = item.optBoolean("offline", false),
                            // Phrases saved by older versions have no id: derive a stable one.
                            id = item.optString("id").ifBlank { "legacy-$i-${(phrase + assistant).hashCode()}" },
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    fun save(items: List<WakePhrase>) {
        val array = JSONArray()
        items.forEach { item ->
            array.put(JSONObject().apply {
                put("id", item.id)
                put("phrase", item.phrase.trim())
                put("assistant", item.assistantKey)
                put("language", item.languageTag)
                put("enabled", item.enabled)
                put("exact", item.exactMatch)
                put("ignore_music", item.ignoreWhenMusic)
                put("offline", item.offline)
            })
        }
        preferences.edit { putString(KEY, array.toString()) }
    }

    fun find(id: String): WakePhrase? = load().firstOrNull { it.id == id }

    /** Adds a new phrase or replaces the one with the same id (keeps its position). */
    fun upsert(phrase: WakePhrase) {
        val current = load()
        save(
            if (current.any { it.id == phrase.id }) {
                current.map { if (it.id == phrase.id) phrase else it }
            } else {
                current + phrase
            }
        )
    }

    fun delete(id: String) = save(load().filterNot { it.id == id })

    fun setEnabled(id: String, enabled: Boolean) =
        save(load().map { if (it.id == id) it.copy(enabled = enabled) else it })

    companion object {
        private const val KEY = "wake_word_phrases"
    }
}
