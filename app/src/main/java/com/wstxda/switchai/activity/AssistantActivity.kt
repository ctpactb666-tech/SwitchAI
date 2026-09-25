package com.wstxda.switchai.activity

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import com.wstxda.switchai.logic.PreferenceHelper
import com.wstxda.switchai.ui.catalog.AssistantUiPreferences
import com.wstxda.switchai.utils.AssistantsMap
import com.wstxda.switchai.utils.Constants

abstract class AssistantActivity : BaseActivity() {

    private val preferenceHelper by lazy { PreferenceHelper(this) }

    /** Key of this assistant in [AssistantsMap], e.g. "chatgpt_assistant". */
    private val assistantKey: String? by lazy {
        AssistantsMap.assistantActivity.entries.firstOrNull { it.value == this::class.java }?.key
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        onCreateInternal()
        finish()
    }

    abstract fun onCreateInternal()

    fun createAssistantIntent(
        packageName: String, voiceInputActivity: String, defaultActivity: String
    ): Intent {
        // Per-assistant switch "Запускать голосовой режим" wins over the global setting.
        val override = assistantKey?.let { AssistantUiPreferences(this).voiceModeOverride(it) }
        val useVoiceInput =
            override ?: preferenceHelper.getBoolean(Constants.VOICE_INPUT_PREF_KEY, true)
        val className = if (useVoiceInput) voiceInputActivity else defaultActivity

        return Intent().apply {
            component = ComponentName(packageName, className)
        }
    }
}
