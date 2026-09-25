package com.wstxda.switchai.fragment

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.wstxda.switchai.R
import com.wstxda.switchai.logic.openOnStore
import com.wstxda.switchai.ui.NeonViews
import com.wstxda.switchai.ui.catalog.AssistantCatalog
import com.wstxda.switchai.ui.catalog.AssistantInfo
import com.wstxda.switchai.ui.catalog.AssistantUiPreferences
import com.wstxda.switchai.wakeword.WakeAssistantLauncher

class AssistantSettingsFragment : Fragment(R.layout.fragment_assistant_settings) {

    private lateinit var info: AssistantInfo
    private val prefs by lazy { AssistantUiPreferences(requireContext()) }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val key = arguments?.getString(ARG_KEY).orEmpty()
        info = AssistantCatalog.find(requireContext(), key) ?: run {
            findNavController().navigateUp()
            return
        }

        NeonViews.setupTopBar(
            fragment = this,
            root = view.findViewById(R.id.topBar),
            title = getString(R.string.neon_assistant_settings_title),
            actionIcon = R.drawable.ic_mic,
        )

        NeonViews.bindAssistantTile(
            view.findViewById<FrameLayout>(R.id.assistantTile),
            view.findViewById<ImageView>(R.id.assistantIcon),
            info,
        )
        view.findViewById<TextView>(R.id.assistantName).text = info.name
        view.findViewById<TextView>(R.id.assistantVendor).text = info.vendor
        view.findViewById<View>(R.id.assistantLaunch).apply {
            contentDescription = getString(R.string.neon_launch_assistant, info.name)
            setOnClickListener { launch() }
        }

        bindSwitches(view)

        NeonViews.bindNavRow(
            view.findViewById(R.id.rowCheck), R.drawable.ic_availability,
            getString(R.string.neon_check_availability),
        ) { checkAvailability() }
        NeonViews.bindNavRow(
            view.findViewById(R.id.rowOpen), R.drawable.ic_open_in_new,
            getString(R.string.neon_open_app),
        ) { launch() }
        NeonViews.bindNavRow(
            view.findViewById(R.id.rowMore), R.drawable.ic_nav_settings,
            getString(R.string.neon_more_settings),
        ) { openAppInfo() }

        view.findViewById<View>(R.id.rowRemove).setOnClickListener { confirmRemove() }
    }

    override fun onResume() {
        super.onResume()
        view?.let(::renderInstalled)
    }

    private fun bindSwitches(view: View) {
        val voiceMode = NeonViews.bindSwitchRow(
            view.findViewById(R.id.rowVoiceMode), R.drawable.ic_voice_input,
            getString(R.string.neon_voice_mode), prefs.voiceMode(info.key),
        ) { prefs.setVoiceMode(info.key, it) }
        if (!info.supportsVoiceMode) {
            voiceMode.isEnabled = false
            view.findViewById<View>(R.id.rowVoiceMode).setOnClickListener {
                Toast.makeText(requireContext(), R.string.neon_voice_mode_unsupported, Toast.LENGTH_SHORT).show()
            }
        }

        NeonViews.bindSwitchRow(
            view.findViewById(R.id.rowVoiceInput), R.drawable.ic_headset,
            getString(R.string.neon_voice_input), prefs.wakeEnabled(info.key),
        ) { prefs.setWakeEnabled(info.key, it) }

        NeonViews.bindSwitchRow(
            view.findViewById(R.id.rowAutoConversation), R.drawable.ic_volume,
            getString(R.string.neon_auto_conversation), prefs.autoConversation(info.key),
        ) { prefs.setAutoConversation(info.key, it) }

        NeonViews.bindSwitchRow(
            view.findViewById(R.id.rowPriority), R.drawable.ic_pin_filled,
            getString(R.string.neon_priority), prefs.isPriority(info.key),
        ) { prefs.setPriority(info.key, it) }
    }

    private fun renderInstalled(view: View) {
        val installed = AssistantCatalog.isInstalled(requireContext(), info.packageName)
        val color = ContextCompat.getColor(
            requireContext(), if (installed) R.color.neon_teal else R.color.neon_text_muted
        )
        view.findViewById<TextView>(R.id.assistantStatus).apply {
            setText(if (installed) R.string.neon_installed else R.string.neon_not_installed)
            setTextColor(color)
        }
        view.findViewById<View>(R.id.assistantStatusIcon).visibility =
            if (installed) View.VISIBLE else View.GONE
    }

    private fun launch() {
        WakeAssistantLauncher.launch(requireContext(), info.key)
    }

    private fun checkAvailability() {
        if (AssistantCatalog.isInstalled(requireContext(), info.packageName)) {
            Toast.makeText(
                requireContext(), getString(R.string.neon_available, info.name), Toast.LENGTH_SHORT
            ).show()
        } else {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(info.name)
                .setMessage(getString(R.string.neon_unavailable, info.name))
                .setNegativeButton(R.string.neon_cancel, null)
                .setPositiveButton(R.string.neon_open_store) { _, _ ->
                    requireContext().openOnStore(info.packageName)
                }
                .show()
        }
    }

    private fun openAppInfo() {
        if (!AssistantCatalog.isInstalled(requireContext(), info.packageName)) {
            checkAvailability()
            return
        }
        runCatching {
            startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    .setData("package:${info.packageName}".toUri())
            )
        }
    }

    private fun confirmRemove() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.neon_remove_title, info.name))
            .setMessage(R.string.neon_remove_message)
            .setNegativeButton(R.string.neon_cancel, null)
            .setPositiveButton(R.string.neon_remove_confirm) { _, _ ->
                prefs.setHidden(info.key, true)
                prefs.setPriority(info.key, false)
                findNavController().navigateUp()
            }
            .show()
    }

    companion object {
        const val ARG_KEY = "assistant_key"
    }
}
