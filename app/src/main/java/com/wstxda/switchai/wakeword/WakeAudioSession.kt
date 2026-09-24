package com.wstxda.switchai.wakeword

import android.content.Context

class WakeAudioSession(
    context: Context,
    private val speakerEngine: SpeakerEmbeddingEngine = SherpaSpeakerEmbeddingEngine(context),
) {
    private val recentAudio = RecentAudioBuffer(
        maxSamples = MicrophoneAudioSource.DEFAULT_SAMPLE_RATE * RECENT_AUDIO_SECONDS
    )

    private val microphone = MicrophoneAudioSource(context) { frame ->
        recentAudio.append(frame.samples)
    }

    fun start(): Boolean = microphone.start()

    fun stop() {
        microphone.stop()
        recentAudio.clear()
    }

    fun currentSpeakerEmbedding(): FloatArray? {
        if (!speakerEngine.isReady) return null
        val audio = recentAudio.snapshot()
        if (audio.size < MIN_SPEAKER_SAMPLES) return null
        return speakerEngine.compute(
            samples = audio,
            sampleRate = MicrophoneAudioSource.DEFAULT_SAMPLE_RATE,
        )
    }

    companion object {
        private const val RECENT_AUDIO_SECONDS = 4
        private const val MIN_SPEAKER_SAMPLES =
            MicrophoneAudioSource.DEFAULT_SAMPLE_RATE * 2
    }
}
