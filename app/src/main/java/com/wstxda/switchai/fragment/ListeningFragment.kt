package com.wstxda.switchai.fragment

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.wstxda.switchai.R
import com.wstxda.switchai.ui.catalog.AssistantCatalog
import com.wstxda.switchai.ui.catalog.AssistantUiPreferences
import com.wstxda.switchai.wakeword.WakeAssistantLauncher
import com.wstxda.switchai.wakeword.WakeFeedback
import com.wstxda.switchai.wakeword.WakePhraseMatcher
import com.wstxda.switchai.wakeword.WakePhraseStore
import com.wstxda.switchai.wakeword.WakeWordSettings
import com.wstxda.switchai.wakeword.WakeWordState

/**
 * "Слушаю…" screen: one-shot recognition in the foreground.
 * Matches the activation phrases first, then plain assistant names ("Gemini").
 */
class ListeningFragment : Fragment(R.layout.fragment_listening), RecognitionListener {

    private val handler = Handler(Looper.getMainLooper())
    private var recognizer: SpeechRecognizer? = null
    private var listening = false
    private var attempts = 0
    private var resumeBackgroundService = false
    private var launched = false
    private var dotIndex = 0

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startListening() else showStopped(getString(R.string.neon_mic_permission_needed))
    }

    private val dotsTicker = object : Runnable {
        override fun run() {
            val dots = view?.findViewById<LinearLayout>(R.id.listenDots) ?: return
            for (i in 0 until dots.childCount) {
                dots.getChildAt(i).setBackgroundResource(
                    if (i == dotIndex) R.drawable.bg_dot_active else R.drawable.bg_dot_inactive
                )
            }
            dotIndex = (dotIndex + 1) % dots.childCount.coerceAtLeast(1)
            if (listening) handler.postDelayed(this, DOT_STEP_MS)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        buildDots(view)
        view.findViewById<View>(R.id.cancelListening).setOnClickListener { findNavController().navigateUp() }
        view.findViewById<View>(R.id.listenRestart).setOnClickListener { restart() }
        view.findViewById<View>(R.id.listenMic).setOnClickListener { restart() }
        view.findViewById<TextView>(R.id.listenHint).text = defaultHint()

        // Background listening and this screen cannot share the microphone.
        if (WakeWordState.current == WakeWordState.Status.LISTENING) {
            resumeBackgroundService = true
            WakeWordSettings.stopService(requireContext())
        }

        if (!SpeechRecognizer.isRecognitionAvailable(requireContext())) {
            showStopped(getString(R.string.neon_listening_no_recognizer))
            return
        }

        if (WakeWordSettings.hasMicPermission(requireContext())) {
            // Small delay lets the background recognizer release the microphone.
            handler.postDelayed({ startListening() }, if (resumeBackgroundService) 450L else 0L)
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun buildDots(view: View) {
        val dots = view.findViewById<LinearLayout>(R.id.listenDots)
        val margin = (resources.displayMetrics.density * 3).toInt()
        repeat(DOTS) {
            dots.addView(View(requireContext()).apply {
                setBackgroundResource(R.drawable.bg_dot_inactive)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                ).apply { setMargins(margin, 0, margin, 0) }
            })
        }
    }

    private fun defaultHint(): String {
        val phrase = WakePhraseStore(requireContext()).load().firstOrNull { it.enabled }
        return phrase?.let { getString(R.string.neon_listening_say, it.displayText) }
            ?: getString(R.string.neon_listening_say_name)
    }

    private fun restart() {
        if (launched) return
        attempts = 0
        startListening()
    }

    private fun startListening() {
        if (!isAdded || launched) return
        val context = requireContext()
        if (!WakeWordSettings.hasMicPermission(context)) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            return
        }

        if (recognizer == null) {
            recognizer = SpeechRecognizer.createSpeechRecognizer(context).also {
                it.setRecognitionListener(this)
            }
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
        }

        view?.findViewById<TextView>(R.id.listenTitle)?.setText(R.string.neon_listening)
        listening = true
        handler.removeCallbacks(dotsTicker)
        handler.post(dotsTicker)
        runCatching { recognizer?.startListening(intent) }
            .onFailure { showStopped(getString(R.string.neon_listening_error)) }
    }

    /** Returns true when something was launched. */
    private fun tryMatch(candidates: List<String>): Boolean {
        val context = requireContext()
        val prefs = AssistantUiPreferences(context)
        val phrases = WakePhraseStore(context).load().filter { prefs.wakeEnabled(it.assistantKey) }

        for (text in candidates) {
            WakePhraseMatcher.match(text, phrases)?.let {
                launch(it.assistantKey)
                return true
            }
        }

        // No phrase: a plain assistant name also works ("Gemini", "Алиса").
        val assistants = AssistantCatalog.visible(context)
        for (text in candidates) {
            val spoken = " ${WakePhraseMatcher.normalize(text)} "
            assistants.firstOrNull { spoken.contains(" ${WakePhraseMatcher.normalize(it.name)} ") }?.let {
                launch(it.key)
                return true
            }
        }
        return false
    }

    private fun launch(assistantKey: String) {
        if (launched) return
        launched = true
        stopRecognizer()
        val name = AssistantCatalog.find(requireContext(), assistantKey)?.name ?: assistantKey
        view?.findViewById<TextView>(R.id.listenTitle)?.text =
            getString(R.string.neon_listening_launching, name)
        WakeFeedback.onPhraseRecognized(requireContext())
        WakeAssistantLauncher.launch(requireContext(), assistantKey)
        handler.postDelayed({
            if (isAdded && findNavController().currentDestination?.id == R.id.listeningFragment) {
                findNavController().navigateUp()
            }
        }, 600L)
    }

    private fun showStopped(message: String) {
        listening = false
        handler.removeCallbacks(dotsTicker)
        view?.findViewById<TextView>(R.id.listenHint)?.text = message
        view?.findViewById<View>(R.id.listenMic)?.animate()?.scaleX(1f)?.scaleY(1f)?.setDuration(150)?.start()
    }

    private fun retryOrStop(message: String) {
        attempts++
        if (attempts < MAX_ATTEMPTS) {
            view?.findViewById<TextView>(R.id.listenHint)?.text = message
            handler.postDelayed({ startListening() }, 900L)
        } else {
            showStopped(message)
        }
    }

    private fun stopRecognizer() {
        listening = false
        handler.removeCallbacks(dotsTicker)
        recognizer?.cancel()
    }

    // --- RecognitionListener -------------------------------------------------

    override fun onRmsChanged(rmsdB: Float) {
        // rmsdB is roughly -2..10; turn it into a soft pulse of the mic and glow.
        val level = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
        view?.findViewById<View>(R.id.listenMic)?.animate()
            ?.scaleX(1f + level * 0.18f)?.scaleY(1f + level * 0.18f)?.setDuration(90)?.start()
        view?.findViewById<View>(R.id.listenGlow)?.animate()
            ?.scaleX(1f + level * 0.08f)?.scaleY(1f + level * 0.08f)?.setDuration(120)?.start()
        view?.findViewById<View>(R.id.listenWave)?.animate()
            ?.scaleY(0.7f + level * 0.8f)?.setDuration(90)?.start()
    }

    override fun onPartialResults(partialResults: Bundle?) {
        val texts = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty()
        texts.firstOrNull()?.takeIf { it.isNotBlank() }?.let {
            view?.findViewById<TextView>(R.id.listenHint)?.text = getString(R.string.neon_listening_heard, it)
        }
        tryMatch(texts)
    }

    override fun onResults(results: Bundle?) {
        if (launched) return
        listening = false
        val texts = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty()
        if (!tryMatch(texts)) retryOrStop(getString(R.string.neon_listening_not_matched))
    }

    override fun onError(error: Int) {
        if (launched || !isAdded) return
        listening = false
        retryOrStop(getString(R.string.neon_listening_error))
    }

    override fun onReadyForSpeech(params: Bundle?) = Unit
    override fun onBeginningOfSpeech() = Unit
    override fun onBufferReceived(buffer: ByteArray?) = Unit
    override fun onEndOfSpeech() = Unit
    override fun onEvent(eventType: Int, params: Bundle?) = Unit

    // --- lifecycle ------------------------------------------------------------

    override fun onStop() {
        super.onStop()
        if (!launched) stopRecognizer()
    }

    override fun onDestroyView() {
        handler.removeCallbacksAndMessages(null)
        recognizer?.destroy()
        recognizer = null
        if (resumeBackgroundService && WakeWordSettings.isEnabled(requireContext())) {
            WakeWordSettings.startService(requireContext())
        }
        super.onDestroyView()
    }

    companion object {
        private const val DOTS = 5
        private const val DOT_STEP_MS = 280L
        private const val MAX_ATTEMPTS = 3
    }
}
