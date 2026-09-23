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
                    add(
                        WakePhrase(
                            phrase = item.getString("phrase"),
                            assistantKey = item.getString("assistant"),
                            languageTag = item.optString("language", "und"),
                            enabled = item.optBoolean("enabled", true),
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
                put("phrase", item.phrase.trim())
                put("assistant", item.assistantKey)
                put("language", item.languageTag)
                put("enabled", item.enabled)
            })
        }
        preferences.edit { putString(KEY, array.toString()) }
    }

    companion object {
        private const val KEY = "wake_word_phrases"
    }
}
