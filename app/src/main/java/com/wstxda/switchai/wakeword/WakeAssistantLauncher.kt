package com.wstxda.switchai.wakeword

import android.content.Context
import android.content.Intent
import com.wstxda.switchai.utils.AssistantsMap

object WakeAssistantLauncher {
    fun launch(context: Context, phrase: WakePhrase): Boolean {
        val activity = AssistantsMap.assistantActivity[phrase.assistantKey] ?: return false
        context.startActivity(Intent(context, activity).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        })
        return true
    }
}
