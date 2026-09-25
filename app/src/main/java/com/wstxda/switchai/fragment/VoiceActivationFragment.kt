package com.wstxda.switchai.fragment

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.annotation.IdRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.slider.Slider
import com.wstxda.switchai.R
import com.wstxda.switchai.ui.NeonViews
import com.wstxda.switchai.wakeword.WakeWordSettings
import com.wstxda.switchai.wakeword.WakeWordState

class VoiceActivationFragment : Fragment(R.layout.fragment_voice_activation) {

    private lateinit var activationSwitch: MaterialSwitch

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        if (WakeWordSettings.hasMicPermission(requireContext())) {
            setActivation(true)
        } else {
            setSwitchSilently(false)
            Toast.makeText(requireContext(), R.string.neon_mic_permission_needed, Toast.LENGTH_LONG).show()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val context = requireContext()
        NeonViews.setupTopBar(this, view.findViewById(R.id.topBar), getString(R.string.neon_activation_title))

        // main switch
        activationSwitch = view.findViewById(R.id.activationSwitch)
        activationSwitch.isChecked = WakeWordSettings.isEnabled(context)
        activationSwitch.setOnCheckedChangeListener { _, checked -> onActivationChanged(checked) }
        view.findViewById<View>(R.id.activationCard).setOnClickListener { activationSwitch.toggle() }

        // sensitivity
        val slider = view.findViewById<Slider>(R.id.sensitivitySlider)
        slider.value = snap(WakeWordSettings.sensitivity(context))
        updateSensitivityLabel(view, slider.value)
        slider.addOnChangeListener { _, value, fromUser ->
            if (fromUser) WakeWordSettings.setSensitivity(requireContext(), value)
            updateSensitivityLabel(view, value)
        }

        // behaviour
        bindPref(view, R.id.rowLocked, R.drawable.ic_smartphone, R.string.neon_locked_screen, WakeWordSettings.KEY_LOCKED_SCREEN)
        bindPref(view, R.id.rowBluetooth, R.drawable.ic_headset, R.string.neon_bluetooth, WakeWordSettings.KEY_BLUETOOTH)
        bindPref(view, R.id.rowSound, R.drawable.ic_volume, R.string.neon_sound, WakeWordSettings.KEY_SOUND)
        bindPref(view, R.id.rowVibration, R.drawable.ic_vibration, R.string.neon_vibration, WakeWordSettings.KEY_VIBRATION)

        WakeWordState.status.observe(viewLifecycleOwner) { renderStatus(view, it) }
    }

    private fun bindPref(
        view: View,
        @IdRes rowId: Int,
        @DrawableRes icon: Int,
        @StringRes title: Int,
        key: String,
    ) {
        NeonViews.bindSwitchRow(
            view.findViewById(rowId), icon, getString(title),
            WakeWordSettings.getBoolean(requireContext(), key),
        ) { WakeWordSettings.putBoolean(requireContext(), key, it) }
    }

    private fun onActivationChanged(checked: Boolean) {
        if (checked && !WakeWordSettings.hasMicPermission(requireContext())) {
            setSwitchSilently(false)
            permissionLauncher.launch(
                buildList {
                    add(Manifest.permission.RECORD_AUDIO)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        add(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }.toTypedArray()
            )
        } else {
            setActivation(checked)
        }
    }

    private fun setActivation(enabled: Boolean) {
        val context = requireContext()
        setSwitchSilently(enabled)
        WakeWordSettings.setEnabled(context, enabled)
        if (enabled) WakeWordSettings.startService(context) else WakeWordSettings.stopService(context)
        view?.let { renderStatus(it, WakeWordState.current) }
    }

    private fun setSwitchSilently(checked: Boolean) {
        activationSwitch.setOnCheckedChangeListener(null)
        activationSwitch.isChecked = checked
        activationSwitch.setOnCheckedChangeListener { _, c -> onActivationChanged(c) }
    }

    private fun renderStatus(view: View, status: WakeWordState.Status) {
        val context = requireContext()
        val enabled = WakeWordSettings.isEnabled(context)
        val (textRes, colorRes) = when {
            status == WakeWordState.Status.LISTENING -> R.string.neon_status_active to R.color.neon_teal
            status == WakeWordState.Status.UNAVAILABLE && enabled -> R.string.neon_status_unavailable to R.color.neon_danger
            status == WakeWordState.Status.LOCKED -> R.string.neon_status_locked to R.color.neon_danger
            else -> R.string.neon_status_inactive to R.color.neon_text_muted
        }
        val color = ContextCompat.getColor(context, colorRes)
        view.findViewById<TextView>(R.id.statusText).apply {
            setText(textRes)
            setTextColor(color)
        }
        view.findViewById<ImageView>(R.id.statusIcon).setColorFilter(color)
        view.findViewById<View>(R.id.statusWave).alpha =
            if (status == WakeWordState.Status.LISTENING) 1f else 0.3f

        if (status == WakeWordState.Status.UNAVAILABLE && enabled && isResumed) {
            MaterialAlertDialogBuilder(context)
                .setTitle(R.string.neon_status_unavailable)
                .setMessage(R.string.neon_unavailable_message)
                .setPositiveButton(android.R.string.ok, null)
                .show()
        }
    }

    private fun updateSensitivityLabel(view: View, value: Float) {
        view.findViewById<TextView>(R.id.sensitivityLabel).setText(
            when {
                value < 0.34f -> R.string.neon_sensitivity_low
                value < 0.67f -> R.string.neon_sensitivity_medium
                else -> R.string.neon_sensitivity_high
            }
        )
    }

    /** Slider requires the value to sit exactly on a step. */
    private fun snap(value: Float): Float = (Math.round(value / 0.05f) * 0.05f).coerceIn(0f, 1f)
}
