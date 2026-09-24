package com.wstxda.switchai.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButtonToggleGroup
import com.wstxda.switchai.R
import com.wstxda.switchai.wakeword.WakeAssistantLauncher
import com.wstxda.switchai.wakeword.WakePhrase

class AssistantsFragment : Fragment(R.layout.fragment_assistants) {

    private data class AssistantUi(
        val key: String,
        val name: String,
        val vendor: String,
        val packageName: String,
        val iconRes: Int,
        val popular: Boolean = true,
    )

    private val assistants = listOf(
        AssistantUi("chatgpt_assistant", "ChatGPT", "OpenAI", "com.openai.chatgpt", R.drawable.ic_assistant_chatgpt),
        AssistantUi("gemini_assistant", "Gemini", "Google", "com.google.android.apps.bard", R.drawable.ic_assistant_gemini),
        AssistantUi("claude_assistant", "Claude", "Anthropic", "com.anthropic.claude", R.drawable.ic_assistant_claude),
        AssistantUi("alice_assistant", "Алиса", "Яндекс", "com.yandex.aliceapp", R.drawable.ic_assistant_alice),
        AssistantUi("copilot_assistant", "Copilot", "Microsoft", "com.microsoft.copilot", R.drawable.ic_assistant_copilot),
        AssistantUi("grok_assistant", "Grok", "xAI", "ai.x.grok", R.drawable.ic_assistant_grok),
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val list = view.findViewById<LinearLayout>(R.id.assistantList)
        render(list, assistants)

        view.findViewById<MaterialButtonToggleGroup>(R.id.filterGroup)
            .addOnButtonCheckedListener { _, checkedId, isChecked ->
                if (!isChecked) return@addOnButtonCheckedListener
                val filtered = when (checkedId) {
                    R.id.filterInstalled -> assistants.filter { isInstalled(it.packageName) }
                    R.id.filterPopular -> assistants.filter { it.popular }
                    else -> assistants
                }
                render(list, filtered)
            }
    }

    private fun render(container: LinearLayout, items: List<AssistantUi>) {
        container.removeAllViews()
        val inflater = LayoutInflater.from(requireContext())
        items.forEach { item ->
            val row = inflater.inflate(R.layout.item_assistant_neon, container, false)
            row.findViewById<ImageView>(R.id.assistantIcon).setImageResource(item.iconRes)
            row.findViewById<TextView>(R.id.assistantName).text = item.name
            row.findViewById<TextView>(R.id.assistantVendor).text = item.vendor
            val installed = isInstalled(item.packageName)
            row.findViewById<TextView>(R.id.assistantStatus).text =
                if (installed) "● Установлен" else "Не установлен"
            row.setOnClickListener {
                if (installed) {
                    WakeAssistantLauncher.launch(
                        requireContext(),
                        WakePhrase(item.name, item.key),
                    )
                }
            }
            container.addView(row)
        }
    }

    private fun isInstalled(packageName: String): Boolean =
        runCatching {
            requireContext().packageManager.getPackageInfo(packageName, 0)
        }.isSuccess
}
