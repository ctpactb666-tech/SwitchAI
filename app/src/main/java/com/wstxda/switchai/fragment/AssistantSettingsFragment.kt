package com.wstxda.switchai.fragment

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.wstxda.switchai.R
import com.wstxda.switchai.wakeword.WakeAssistantLauncher
import com.wstxda.switchai.wakeword.WakePhrase

class AssistantSettingsFragment : Fragment(R.layout.fragment_assistant_settings) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val key = arguments?.getString(ARG_KEY).orEmpty()
        val name = arguments?.getString(ARG_NAME).orEmpty().ifBlank { "Ассистент" }
        val packageName = arguments?.getString(ARG_PACKAGE).orEmpty()

        view.findViewById<TextView>(R.id.assistantTitle).text = name
        view.findViewById<TextView>(R.id.assistantSubtitle).text =
            "Персональные настройки $name"

        view.findViewById<View>(R.id.checkAssistant).setOnClickListener {
            val installed = runCatching {
                requireContext().packageManager.getPackageInfo(packageName, 0)
            }.isSuccess
            Toast.makeText(
                requireContext(),
                if (installed) "$name доступен" else "$name не установлен",
                Toast.LENGTH_SHORT,
            ).show()
        }

        view.findViewById<View>(R.id.openAssistant).setOnClickListener {
            WakeAssistantLauncher.launch(
                requireContext(),
                WakePhrase(name, key),
            )
        }

        view.findViewById<View>(R.id.removeAssistant).setOnClickListener {
            Toast.makeText(
                requireContext(),
                "Скрытие ассистента будет добавлено в следующем проходе",
                Toast.LENGTH_SHORT,
            ).show()
        }
    }

    companion object {
        const val ARG_KEY = "assistant_key"
        const val ARG_NAME = "assistant_name"
        const val ARG_PACKAGE = "assistant_package"
    }
}
