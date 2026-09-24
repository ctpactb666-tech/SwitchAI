package com.wstxda.switchai.fragment

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.wstxda.switchai.R

class HomeFragment : Fragment(R.layout.fragment_home) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.findViewById<View>(R.id.openVoiceActivation).setOnClickListener {
            findNavController().navigate(R.id.voiceActivationFragment)
        }
        view.findViewById<View>(R.id.startListening).setOnClickListener {
            findNavController().navigate(R.id.listeningFragment)
        }
        view.findViewById<View>(R.id.openPhrasesQuick).setOnClickListener {
            findNavController().navigate(R.id.phrasesFragment)
        }
        view.findViewById<View>(R.id.openAssistantsQuick).setOnClickListener {
            findNavController().navigate(R.id.assistantsFragment)
        }

        val cards = listOf(
            R.id.cardChatGpt,
            R.id.cardGemini,
            R.id.cardClaude,
            R.id.cardAlice,
            R.id.cardCopilot,
            R.id.cardGrok,
        )
        cards.forEach { id ->
            view.findViewById<View>(id).setOnClickListener {
                findNavController().navigate(R.id.assistantsFragment)
            }
        }
    }
}
