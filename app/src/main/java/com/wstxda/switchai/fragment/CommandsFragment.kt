package com.wstxda.switchai.fragment

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.wstxda.switchai.R

class CommandsFragment : Fragment(R.layout.fragment_commands) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.findViewById<View>(R.id.openActivation).setOnClickListener {
            findNavController().navigate(R.id.voiceActivationFragment)
        }
        view.findViewById<View>(R.id.openPhrases).setOnClickListener {
            findNavController().navigate(R.id.phrasesFragment)
        }
        view.findViewById<View>(R.id.openOwnerVoice).setOnClickListener {
            findNavController().navigate(R.id.wakeWordPreferencesFragment)
        }
    }
}
