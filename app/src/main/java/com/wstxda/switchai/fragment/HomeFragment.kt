package com.wstxda.switchai.fragment

import android.Manifest
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.wstxda.switchai.R
import com.wstxda.switchai.ui.NeonViews
import com.wstxda.switchai.ui.catalog.AssistantCatalog
import com.wstxda.switchai.wakeword.WakeAssistantLauncher
import com.wstxda.switchai.wakeword.WakePhraseStore
import com.wstxda.switchai.wakeword.WakeWordSettings
import com.wstxda.switchai.wakeword.WakeWordState

class HomeFragment : Fragment(R.layout.fragment_home) {

    private val animators = mutableListOf<ValueAnimator>()

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        if (WakeWordSettings.hasMicPermission(requireContext())) {
            enableActivation()
        } else {
            Toast.makeText(requireContext(), R.string.neon_mic_permission_needed, Toast.LENGTH_LONG).show()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.findViewById<View>(R.id.homeHeaderListen).setOnClickListener { NeonViews.openListening(this) }
        view.findViewById<View>(R.id.homeOrbContainer).setOnClickListener { NeonViews.openListening(this) }
        view.findViewById<View>(R.id.homeToggle).setOnClickListener { toggleActivation() }
        view.findViewById<View>(R.id.homePhrases).setOnClickListener {
            findNavController().navigate(R.id.phrasesFragment)
        }

        WakeWordState.status.observe(viewLifecycleOwner) { renderStatus(view, it) }
    }

    override fun onResume() {
        super.onResume()
        view?.let {
            renderGrid(it)
            renderStatus(it, WakeWordState.current)
        }
    }

    private fun renderStatus(view: View, status: WakeWordState.Status) {
        val enabled = WakeWordSettings.isEnabled(requireContext())
        val statusText = view.findViewById<TextView>(R.id.homeStatus)
        val phraseText = view.findViewById<TextView>(R.id.homePhrase)

        statusText.setText(
            when {
                status == WakeWordState.Status.LISTENING -> R.string.neon_home_waiting
                status == WakeWordState.Status.UNAVAILABLE && enabled -> R.string.neon_home_unavailable
                status == WakeWordState.Status.LOCKED -> R.string.neon_home_locked
                else -> R.string.neon_home_off
            }
        )

        val firstPhrase = WakePhraseStore(requireContext()).load().firstOrNull { it.enabled }
        phraseText.text = firstPhrase?.let { getString(R.string.neon_quoted, it.displayText) }
            ?: getString(R.string.neon_home_no_phrases)

        val active = status == WakeWordState.Status.LISTENING
        view.findViewById<View>(R.id.homeToggle).alpha = if (active) 1f else 0.6f
        setOrbAnimated(view, active)
    }

    private fun renderGrid(view: View) {
        val grid = view.findViewById<GridLayout>(R.id.homeGrid)
        grid.removeAllViews()
        val inflater = LayoutInflater.from(requireContext())
        val margin = resources.getDimensionPixelSize(R.dimen.neon_grid_gap)

        AssistantCatalog.homeGrid(requireContext()).forEach { info ->
            val card = inflater.inflate(R.layout.item_home_assistant_card, grid, false)
            NeonViews.bindAssistantTile(
                card.findViewById<FrameLayout>(R.id.cardTile),
                card.findViewById<ImageView>(R.id.cardIcon),
                info,
            )
            card.findViewById<TextView>(R.id.cardName).text = info.name
            card.contentDescription = getString(R.string.neon_launch_assistant, info.name)

            val installed = AssistantCatalog.isInstalled(requireContext(), info.packageName)
            card.alpha = if (installed) 1f else 0.55f

            // Tap: launch (if not installed, the launcher opens the store page).
            card.setOnClickListener { WakeAssistantLauncher.launch(requireContext(), info.key) }
            // Long press: assistant settings.
            card.setOnLongClickListener {
                findNavController().navigate(
                    R.id.assistantSettingsFragment,
                    bundleOf(AssistantSettingsFragment.ARG_KEY to info.key),
                )
                true
            }

            card.layoutParams = GridLayout.LayoutParams(
                GridLayout.spec(GridLayout.UNDEFINED, 1f),
                GridLayout.spec(GridLayout.UNDEFINED, 1f),
            ).apply {
                width = 0
                height = resources.getDimensionPixelSize(R.dimen.neon_home_card_height)
                setMargins(margin, margin, margin, margin)
            }
            grid.addView(card)
        }
    }

    private fun toggleActivation() {
        val context = requireContext()
        when {
            WakeWordSettings.isEnabled(context) -> {
                WakeWordSettings.setEnabled(context, false)
                WakeWordSettings.stopService(context)
                view?.let { renderStatus(it, WakeWordState.current) }
            }

            !WakeWordSettings.hasMicPermission(context) -> {
                permissionLauncher.launch(
                    buildList {
                        add(Manifest.permission.RECORD_AUDIO)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            add(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }.toTypedArray()
                )
            }

            else -> enableActivation()
        }
    }

    private fun enableActivation() {
        val context = requireContext()
        if (WakeWordState.current == WakeWordState.Status.UNAVAILABLE) {
            MaterialAlertDialogBuilder(context)
                .setTitle(R.string.neon_status_unavailable)
                .setMessage(R.string.neon_unavailable_message)
                .setPositiveButton(android.R.string.ok, null)
                .show()
        }
        WakeWordSettings.setEnabled(context, true)
        WakeWordSettings.startService(context)
    }

    /** Gentle "breathing" of the orb while the app is really listening. */
    private fun setOrbAnimated(view: View, animated: Boolean) {
        animators.forEach { it.cancel() }
        animators.clear()
        val orb = view.findViewById<View>(R.id.homeOrb)
        val wave = view.findViewById<View>(R.id.homeWave)
        if (!animated) {
            orb.scaleX = 1f; orb.scaleY = 1f
            wave.scaleY = 1f; wave.alpha = 0.7f
            return
        }
        animators += ObjectAnimator.ofPropertyValuesHolder(
            orb,
            PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 1.045f),
            PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 1.045f),
        ).apply { configureLoop(2600) }
        animators += ObjectAnimator.ofPropertyValuesHolder(
            wave,
            PropertyValuesHolder.ofFloat(View.SCALE_Y, 0.8f, 1.2f),
            PropertyValuesHolder.ofFloat(View.ALPHA, 0.75f, 1f),
        ).apply { configureLoop(1300) }
        animators.forEach { it.start() }
    }

    private fun ValueAnimator.configureLoop(durationMs: Long) {
        duration = durationMs
        repeatMode = ValueAnimator.REVERSE
        repeatCount = ValueAnimator.INFINITE
        interpolator = AccelerateDecelerateInterpolator()
    }

    override fun onDestroyView() {
        animators.forEach { it.cancel() }
        animators.clear()
        super.onDestroyView()
    }
}
