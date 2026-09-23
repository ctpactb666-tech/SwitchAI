package com.wstxda.switchai.wakeword

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import java.util.concurrent.atomic.AtomicBoolean

class MicrophoneAudioSource(
    private val sampleRate: Int = DEFAULT_SAMPLE_RATE,
    private val frameSize: Int = DEFAULT_FRAME_SIZE,
    private val onFrame: (AudioFrame) -> Unit,
) {
    private val running = AtomicBoolean(false)
    private var audioRecord: AudioRecord? = null
    private var worker: Thread? = null

    fun start(): Boolean {
        if (running.get()) return true

        val minBuffer = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )
        if (minBuffer <= 0) return false

        val bufferSize = maxOf(minBuffer, frameSize * 2 * 4)
        val recorder = AudioRecord(
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize,
        )
        if (recorder.state != AudioRecord.STATE_INITIALIZED) {
            recorder.release()
            return false
        }

        audioRecord = recorder
        running.set(true)
        recorder.startRecording()

        worker = Thread({
            val pcm = ShortArray(frameSize)
            while (running.get()) {
                val count = recorder.read(pcm, 0, pcm.size)
                if (count > 0) {
                    val samples = FloatArray(count) { index ->
                        pcm[index] / 32768.0f
                    }
                    onFrame(AudioFrame(samples, sampleRate))
                }
            }
        }, "SwitchAI-Microphone").also { it.start() }

        return true
    }

    fun stop() {
        if (!running.getAndSet(false)) return
        runCatching { audioRecord?.stop() }
        runCatching { worker?.join(500) }
        worker = null
        audioRecord?.release()
        audioRecord = null
    }

    companion object {
        const val DEFAULT_SAMPLE_RATE = 16_000
        const val DEFAULT_FRAME_SIZE = 1_600
    }
}
