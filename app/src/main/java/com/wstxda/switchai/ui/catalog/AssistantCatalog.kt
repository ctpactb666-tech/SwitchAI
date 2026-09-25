package com.wstxda.switchai.ui.catalog

import android.content.Context
import android.content.pm.PackageManager
import com.wstxda.switchai.R
import com.wstxda.switchai.ui.utils.AssistantResourcesManager
import com.wstxda.switchai.utils.AssistantsMap

/**
 * Single source of truth for the assistants shown in the new neon UI
 * (home grid, assistants list, phrase picker, assistant settings).
 * Built on top of [AssistantsMap], so every assistant supported by the
 * launcher activities appears here automatically.
 */
data class AssistantInfo(
    val key: String,
    val name: String,
    val vendor: String,
    val packageName: String,
    val iconRes: Int,
    val tileRes: Int,
    val popular: Boolean,
    val supportsVoiceMode: Boolean,
)

object AssistantCatalog {

    /** Order of the "popular" assistants — also the default home grid order. */
    val popularKeys = listOf(
        "chatgpt_assistant",
        "gemini_assistant",
        "claude_assistant",
        "alice_assistant",
        "copilot_assistant",
        "grok_assistant",
    )

    private val vendors = mapOf(
        "accio_assistant" to "Alibaba",
        "alexa_assistant" to "Amazon",
        "alice_assistant" to "Яндекс",
        "bixby_assistant" to "Samsung",
        "breeno_assistant" to "OPPO",
        "chatgpt_assistant" to "OpenAI",
        "claude_assistant" to "Anthropic",
        "copilot_assistant" to "Microsoft",
        "deepseek_assistant" to "DeepSeek",
        "doubao_assistant" to "ByteDance",
        "dola_assistant" to "ByteDance",
        "gemini_assistant" to "Google",
        "grok_assistant" to "xAI",
        "home_assistant" to "Home Assistant",
        "home_minimal_assistant" to "Home Assistant",
        "ima_assistant" to "Tencent",
        "kimi_assistant" to "Moonshot AI",
        "lumo_assistant" to "Proton",
        "marusya_assistant" to "VK",
        "meta_assistant" to "Meta",
        "minimax_assistant" to "MiniMax",
        "moto_assistant" to "Motorola",
        "perplexity_assistant" to "Perplexity",
        "pi_assistant" to "Inflection AI",
        "poe_assistant" to "Quora",
        "qingyan_assistant" to "Zhipu AI",
        "qwen_assistant" to "Alibaba",
        "spark_assistant" to "iFlytek",
        "stepfun_assistant" to "StepFun",
        "vibe_assistant" to "Mistral AI",
        "wenxiaoyan_assistant" to "Baidu",
        "xiaoai_assistant" to "Xiaomi",
        "yuanbao_assistant" to "Tencent",
    )

    private val tiles = mapOf(
        "chatgpt_assistant" to R.drawable.bg_icon_chatgpt,
        "gemini_assistant" to R.drawable.bg_icon_gemini,
        "claude_assistant" to R.drawable.bg_icon_claude,
        "alice_assistant" to R.drawable.bg_icon_alice,
        "copilot_assistant" to R.drawable.bg_icon_copilot,
        "grok_assistant" to R.drawable.bg_icon_grok,
    )

    fun all(context: Context): List<AssistantInfo> {
        val resources = AssistantResourcesManager(context)
        return AssistantsMap.assistantPackage.map { (key, pkg) ->
            AssistantInfo(
                key = key,
                name = resources.getAssistantName(key),
                vendor = vendors[key] ?: pkg,
                packageName = pkg,
                iconRes = resources.getAssistantIcon(key),
                tileRes = tiles[key] ?: R.drawable.bg_icon_default,
                popular = key in popularKeys,
                supportsVoiceMode = key in AssistantsMap.assistantsVoiceInput,
            )
        }.sortedWith(
            compareBy<AssistantInfo> { info ->
                popularKeys.indexOf(info.key).let { if (it < 0) Int.MAX_VALUE else it }
            }.thenBy { it.name.lowercase() }
        )
    }

    fun find(context: Context, key: String): AssistantInfo? =
        all(context).firstOrNull { it.key == key }

    fun isInstalled(context: Context, packageName: String): Boolean =
        try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }

    /**
     * Visible assistants for lists: hidden ones removed, "priority" ones first.
     */
    fun visible(context: Context): List<AssistantInfo> {
        val prefs = AssistantUiPreferences(context)
        val hidden = prefs.hidden()
        return all(context)
            .filterNot { it.key in hidden }
            .sortedByDescending { prefs.isPriority(it.key) }
    }

    /** Six cards for the home screen: priority → installed popular → popular. */
    fun homeGrid(context: Context): List<AssistantInfo> {
        val prefs = AssistantUiPreferences(context)
        val candidates = visible(context)
        return candidates
            .sortedWith(
                compareByDescending<AssistantInfo> { prefs.isPriority(it.key) }
                    .thenByDescending { it.popular && isInstalled(context, it.packageName) }
                    .thenByDescending { it.popular }
            )
            .take(6)
    }
}
