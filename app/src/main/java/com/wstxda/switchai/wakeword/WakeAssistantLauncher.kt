package com.wstxda.switchai.wakeword

import android.content.Context
import android.content.Intent
import com.wstxda.switchai.utils.AssistantsMap

object WakeAssistantLauncher {
    fun launch(context: Context, phrase: WakePhrase): Boolean = launch(context, phrase.assistantKey)

    fun launch(context: Context, assistantKey: String): Boolean {
        val activity = AssistantsMap.assistantActivity[assistantKey] ?: return false
        return runCatching {
            context.startActivity(Intent(context, activity).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            })
        }.isSuccess
    }
}
