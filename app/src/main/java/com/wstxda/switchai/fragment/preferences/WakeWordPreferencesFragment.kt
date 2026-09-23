package com.wstxda.switchai.fragment.preferences

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.preference.SwitchPreferenceCompat
import com.wstxda.switchai.R
import com.wstxda.switchai.fragment.BasePreferenceFragment
import com.wstxda.switchai.wakeword.WakeWordService

class WakeWordPreferencesFragment : BasePreferenceFragment() {

    override val preferencesResId: Int get() = R.xml.preferences_wake_word

    private val permissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val micGranted = result[Manifest.permission.RECORD_AUDIO] == true ||
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED

        val toggle = findPreference<SwitchPreferenceCompat>(KEY_WAKE_WORD_ENABLED)
        if (micGranted) {
            toggle?.isChecked = true
            startWakeWordService()
        } else {
            toggle?.isChecked = false
        }
    }

    override fun setupListeners() {
        findPreference<SwitchPreferenceCompat>(KEY_WAKE_WORD_ENABLED)
            ?.setOnPreferenceChangeListener { _, newValue ->
                val enabled = newValue as Boolean
                if (enabled) {
                    if (hasMicrophonePermission()) {
                        startWakeWordService()
                        true
                    } else {
                        requestVoicePermissions()
                        false
                    }
                } else {
                    requireContext().stopService(
                        Intent(requireContext(), WakeWordService::class.java)
                    )
                    true
                }
            }
    }

    private fun hasMicrophonePermission(): Boolean =
        ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

    private fun requestVoicePermissions() {
        val permissions = buildList {
            add(Manifest.permission.RECORD_AUDIO)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        permissionsLauncher.launch(permissions.toTypedArray())
    }

    private fun startWakeWordService() {
        val intent = Intent(requireContext(), WakeWordService::class.java)
        ContextCompat.startForegroundService(requireContext(), intent)
    }

    companion object {
        private const val KEY_WAKE_WORD_ENABLED = "wake_word_enabled"
    }
}
