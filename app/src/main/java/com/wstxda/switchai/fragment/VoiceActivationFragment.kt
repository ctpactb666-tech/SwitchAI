package com.wstxda.switchai.fragment

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.slider.Slider
import com.wstxda.switchai.R
import com.wstxda.switchai.wakeword.WakeWordService

class VoiceActivationFragment : Fragment(R.layout.fragment_voice_activation) {

    private val prefs by lazy { PreferenceManager.getDefaultSharedPreferences(requireContext()) }
    private var pendingEnable = false

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        if (hasMicPermission() && pendingEnable) {
            setEnabled(true)
        } else if (view != null) {
            requireView().findViewById<MaterialSwitch>(R.id.activationSwitch).isChecked = false
        }
        pendingEnable = false
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val activation = view.findViewById<MaterialSwitch>(R.id.activationSwitch)
        val locked = view.findViewById<MaterialSwitch>(R.id.lockedScreenSwitch)
        val bluetooth = view.findViewById<MaterialSwitch>(R.id.bluetoothSwitch)
        val sound = view.findViewById<MaterialSwitch>(R.id.soundSwitch)
        val vibration = view.findViewById<MaterialSwitch>(R.id.vibrationSwitch)
        val slider = view.findViewById<Slider>(R.id.sensitivitySlider)

        activation.isChecked = prefs.getBoolean("wake_word_enabled", false)
        locked.isChecked = prefs.getBoolean("wake_locked_screen", true)
        bluetooth.isChecked = prefs.getBoolean("wake_bluetooth", true)
        sound.isChecked = prefs.getBoolean("wake_sound_feedback", true)
        vibration.isChecked = prefs.getBoolean("wake_vibration_feedback", true)
        slider.value = prefs.getFloat("owner_voice_threshold", 0.72f).coerceIn(0.5f, 0.95f)
        updateStatus(view, activation.isChecked)
        updateSensitivityLabel(view, slider.value)

        activation.setOnCheckedChangeListener { button, checked ->
            if (checked && !hasMicPermission()) {
                button.isChecked = false
                pendingEnable = true
                requestPermissions()
            } else {
                setEnabled(checked)
                updateStatus(view, checked)
            }
        }

        locked.setOnCheckedChangeListener { _, v -> prefs.edit().putBoolean("wake_locked_screen", v).apply() }
        bluetooth.setOnCheckedChangeListener { _, v -> prefs.edit().putBoolean("wake_bluetooth", v).apply() }
        sound.setOnCheckedChangeListener { _, v -> prefs.edit().putBoolean("wake_sound_feedback", v).apply() }
        vibration.setOnCheckedChangeListener { _, v -> prefs.edit().putBoolean("wake_vibration_feedback", v).apply() }
        slider.addOnChangeListener { _, value, _ ->
            prefs.edit().putFloat("owner_voice_threshold", value).apply()
            updateSensitivityLabel(view, value)
        }
    }

    private fun setEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("wake_word_enabled", enabled).apply()
        val intent = Intent(requireContext(), WakeWordService::class.java)
        if (enabled) {
            ContextCompat.startForegroundService(requireContext(), intent)
        } else {
            requireContext().stopService(intent)
        }
        view?.let { updateStatus(it, enabled) }
    }

    private fun updateStatus(view: View, enabled: Boolean) {
        view.findViewById<android.widget.TextView>(R.id.statusText).text =
            if (enabled) "Активно" else "Неактивно"
    }

    private fun updateSensitivityLabel(view: View, value: Float) {
        view.findViewById<android.widget.TextView>(R.id.sensitivityLabel).text = when {
            value < 0.65f -> "Низкая"
            value < 0.82f -> "Средняя"
            else -> "Высокая"
        }
    }

    private fun hasMicPermission(): Boolean =
        ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.RECORD_AUDIO,
        ) == PackageManager.PERMISSION_GRANTED

    private fun requestPermissions() {
        val requested = buildList {
            add(Manifest.permission.RECORD_AUDIO)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        permissionLauncher.launch(requested.toTypedArray())
    }
}
