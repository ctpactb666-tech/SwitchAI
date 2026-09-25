package com.wstxda.switchai.fragment

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.wstxda.switchai.R
import com.wstxda.switchai.ui.NeonViews

class CommandsFragment : Fragment(R.layout.fragment_commands) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.findViewById<View>(R.id.commandsListen).setOnClickListener { NeonViews.openListening(this) }

        NeonViews.bindNavRow(
            view.findViewById(R.id.openActivation), R.drawable.ic_mic,
            getString(R.string.neon_activation_title), getString(R.string.neon_commands_activation_summary),
        ) { findNavController().navigate(R.id.voiceActivationFragment) }

        NeonViews.bindNavRow(
            view.findViewById(R.id.openPhrases), R.drawable.ic_tune_small,
            getString(R.string.neon_phrases_title), getString(R.string.neon_commands_phrases_summary),
        ) { findNavController().navigate(R.id.phrasesFragment) }

        NeonViews.bindNavRow(
            view.findViewById(R.id.openOwnerVoice), R.drawable.ic_voice_input,
            getString(R.string.neon_commands_owner), getString(R.string.neon_commands_owner_summary),
        ) { findNavController().navigate(R.id.wakeWordPreferencesFragment) }

        NeonViews.bindNavRow(
            view.findViewById(R.id.openTest), R.drawable.ic_wave_bars,
            getString(R.string.neon_commands_test), getString(R.string.neon_commands_test_summary),
        ) { NeonViews.openListening(this) }
    }
}
