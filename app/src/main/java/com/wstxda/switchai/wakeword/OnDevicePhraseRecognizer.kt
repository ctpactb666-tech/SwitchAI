package com.wstxda.switchai.wakeword

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.os.Build
import android.os.Handler
import androidx.annotation.RequiresApi
import android.os.Looper

class OnDevicePhraseRecognizer(
    private val context: Context,
    private val onText: (String) -> Unit,
    private val onUnavailable: () -> Unit = {},
) : RecognitionListener {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var recognizer: SpeechRecognizer? = null
    private var running = false

    fun start() {
        if (running) return
        running = true
        mainHandler.post {
            val onDeviceAvailable =
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                    SpeechRecognizer.isOnDeviceRecognitionAvailable(context)

            if (!onDeviceAvailable) {
                running = false
                onUnavailable()
                return@post
            }

            recognizer = createOnDeviceRecognizer().also {
                it.setRecognitionListener(this)
            }

            startListening()
        }
    }

    fun stop() {
        running = false
        mainHandler.post {
            recognizer?.cancel()
            recognizer?.destroy()
            recognizer = null
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun createOnDeviceRecognizer(): SpeechRecognizer =
        SpeechRecognizer.createOnDeviceSpeechRecognizer(context)

    private fun startListening() {
        if (!running) return
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
        }

        runCatching { recognizer?.startListening(intent) }
            .onFailure { restartSoon() }
    }

    private fun restartSoon() {
        if (!running) return
        mainHandler.removeCallbacksAndMessages(null)
        mainHandler.postDelayed({ startListening() }, RESTART_DELAY_MS)
    }

    private fun emitResults(bundle: Bundle?) {
        bundle?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            ?.firstOrNull()
            ?.takeIf { it.isNotBlank() }
            ?.let(onText)
    }

    override fun onResults(results: Bundle?) {
        emitResults(results)
        restartSoon()
    }

    override fun onPartialResults(partialResults: Bundle?) {
        emitResults(partialResults)
    }

    override fun onError(error: Int) {
        restartSoon()
    }

    override fun onReadyForSpeech(params: Bundle?) = Unit
    override fun onBeginningOfSpeech() = Unit
    override fun onRmsChanged(rmsdB: Float) = Unit
    override fun onBufferReceived(buffer: ByteArray?) = Unit
    override fun onEndOfSpeech() = Unit
    override fun onEvent(eventType: Int, params: Bundle?) = Unit

    companion object {
        private const val RESTART_DELAY_MS = 650L
    }
}
