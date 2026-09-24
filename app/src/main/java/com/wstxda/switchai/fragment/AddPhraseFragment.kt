package com.wstxda.switchai.fragment

import android.os.Bundle
import android.view.View
import android.widget.Spinner
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.textfield.TextInputEditText
import com.wstxda.switchai.R
import com.wstxda.switchai.wakeword.WakePhrase
import com.wstxda.switchai.wakeword.WakePhraseStore

class AddPhraseFragment : Fragment(R.layout.fragment_add_phrase) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.findViewById<View>(R.id.savePhrase).setOnClickListener {
            val phrase = view.findViewById<TextInputEditText>(R.id.phraseInput).text?.toString()?.trim().orEmpty()
            if (phrase.isEmpty()) return@setOnClickListener
            val assistantPos = view.findViewById<Spinner>(R.id.assistantSpinner).selectedItemPosition
            val languagePos = view.findViewById<Spinner>(R.id.languageSpinner).selectedItemPosition
            val keys = arrayOf("chatgpt_assistant","gemini_assistant","claude_assistant","alice_assistant")
            val langs = arrayOf("und","ru-RU","en-US")
            val store = WakePhraseStore(requireContext())
            store.save(store.load() + WakePhrase(phrase, keys[assistantPos.coerceIn(keys.indices)], langs[languagePos.coerceIn(langs.indices)], true))
            findNavController().navigateUp()
        }
    }
}
