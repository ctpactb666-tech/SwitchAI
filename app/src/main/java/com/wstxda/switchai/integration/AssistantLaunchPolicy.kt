package com.wstxda.switchai.integration

import android.content.Context
import com.wstxda.switchai.logic.PackageChecker
import com.wstxda.switchai.utils.AssistantsMap

object AssistantLaunchPolicy {

    enum class Target {
        APP,
        EMBEDDED,
        UNAVAILABLE
    }

    fun resolve(context: Context, assistantKey: String): Target {
        val preferences = AssistantConnectionPreferences(context)
        val mode = preferences.getMode(assistantKey)
        val packageName = AssistantsMap.assistantPackage[assistantKey]
        val appInstalled = packageName != null && PackageChecker(context).isInstalled(packageName)
        val embeddedReady = preferences.isEmbeddedSessionConnected(assistantKey)

        return when (mode) {
            AssistantConnectionMode.APP ->
                if (appInstalled) Target.APP else Target.UNAVAILABLE

            AssistantConnectionMode.EMBEDDED ->
                if (embeddedReady) Target.EMBEDDED else Target.UNAVAILABLE

            AssistantConnectionMode.AUTO ->
                when {
                    embeddedReady -> Target.EMBEDDED
                    appInstalled -> Target.APP
                    else -> Target.UNAVAILABLE
                }
        }
    }
}
