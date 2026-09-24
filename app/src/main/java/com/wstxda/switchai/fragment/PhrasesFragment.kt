package com.wstxda.switchai.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.materialswitch.MaterialSwitch
import com.wstxda.switchai.R
import com.wstxda.switchai.wakeword.WakePhraseStore

class PhrasesFragment : Fragment(R.layout.fragment_phrases) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.findViewById<View>(R.id.addPhrase).setOnClickListener {
            findNavController().navigate(R.id.addPhraseFragment)
        }
        render(view)
    }

    override fun onResume() {
        super.onResume()
        view?.let(::render)
    }

    private fun render(view: View) {
        val store = WakePhraseStore(requireContext())
        val container = view.findViewById<LinearLayout>(R.id.phraseList)
        container.removeAllViews()
        val inflater = LayoutInflater.from(requireContext())
        store.load().forEach { phrase ->
            val row = inflater.inflate(R.layout.item_phrase_neon, container, false)
            row.findViewById<TextView>(R.id.phraseText).text = "“${phrase.phrase}”"
            row.findViewById<TextView>(R.id.phraseAssistant).text = assistantName(phrase.assistantKey)
            row.findViewById<MaterialSwitch>(R.id.phraseEnabled).apply {
                isChecked = phrase.enabled
                setOnCheckedChangeListener { _, checked ->
                    val updated = store.load().map {
                        if (it.phrase == phrase.phrase && it.assistantKey == phrase.assistantKey) it.copy(enabled = checked) else it
                    }
                    store.save(updated)
                }
            }
            row.setOnLongClickListener {
                store.save(store.load().filterNot { it.phrase == phrase.phrase && it.assistantKey == phrase.assistantKey })
                render(view)
                true
            }
            container.addView(row)
        }
    }

    private fun assistantName(key: String) = when (key) {
        "chatgpt_assistant" -> "ChatGPT"
        "gemini_assistant" -> "Gemini"
        "claude_assistant" -> "Claude"
        "alice_assistant" -> "Алиса"
        else -> key
    }
}
