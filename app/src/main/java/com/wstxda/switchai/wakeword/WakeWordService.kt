package com.wstxda.switchai.wakeword

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.wstxda.switchai.R
import com.wstxda.switchai.activity.MainActivity

class WakeWordService : Service() {

    private val ownerSecurity by lazy { OwnerVoiceSecurity(this) }
    private val coordinator by lazy { WakeWordCoordinator(this) }
    private val phraseStore by lazy { WakePhraseStore(this) }
    private var phraseRecognizer: OnDevicePhraseRecognizer? = null
    private val recognizerAudio = RecentAudioBuffer(
        maxSamples = MicrophoneAudioSource.DEFAULT_SAMPLE_RATE * OWNER_SAMPLE_SECONDS,
    )
    private val speakerEngine by lazy { SherpaSpeakerEmbeddingEngine(this) }
    private val bluetoothRouter by lazy { BluetoothWakeRouter(this) }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startAsForeground()

        bluetoothRouter.applyIfEnabled()

        phraseRecognizer = OnDevicePhraseRecognizer(
            context = this,
            onText = { recognized ->
                val embedding = currentOwnerEmbedding()
                when (coordinator.onRecognizedText(recognized, embedding)) {
                    WakeWordCoordinator.Result.Locked -> {
                        WakeWordState.set(WakeWordState.Status.LOCKED)
                        stopSelf()
                    }

                    else -> Unit
                }
            },
            onAudioBuffer = { pcm -> appendRecognizerAudio(pcm) },
            onUnavailable = {
                WakeWordState.set(WakeWordState.Status.UNAVAILABLE)
                stopSelf()
            },
            languageProvider = { preferredLanguage() },
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (ownerSecurity.isLocked) {
            WakeWordState.set(WakeWordState.Status.LOCKED)
            stopSelf()
            return START_NOT_STICKY
        }

        WakeWordState.set(WakeWordState.Status.LISTENING)
        phraseRecognizer?.start()
        return START_STICKY
    }

    override fun onDestroy() {
        phraseRecognizer?.stop()
        phraseRecognizer = null
        recognizerAudio.clear()
        bluetoothRouter.clear()
        if (WakeWordState.current == WakeWordState.Status.LISTENING) {
            WakeWordState.set(WakeWordState.Status.STOPPED)
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun appendRecognizerAudio(pcm: ByteArray) {
        if (pcm.size < 2) return
        val sampleCount = pcm.size / 2
        val samples = FloatArray(sampleCount)
        var offset = 0
        for (index in 0 until sampleCount) {
            val low = pcm[offset].toInt() and 0xff
            val high = pcm[offset + 1].toInt()
            val value = (high shl 8) or low
            samples[index] = value.toShort() / 32768.0f
            offset += 2
        }
        recognizerAudio.append(samples)
    }

    private fun currentOwnerEmbedding(): FloatArray? {
        if (!WakeWordSettings.getBoolean(this, WakeWordSettings.KEY_OWNER_ONLY, true)) return null
        if (!speakerEngine.isReady) return null
        val samples = recognizerAudio.snapshot()
        if (samples.size < MicrophoneAudioSource.DEFAULT_SAMPLE_RATE) return null
        return speakerEngine.compute(samples, MicrophoneAudioSource.DEFAULT_SAMPLE_RATE)
    }

    /** Most common explicit language among enabled phrases; null = device default. */
    private fun preferredLanguage(): String? =
        phraseStore.load()
            .filter { it.enabled && it.languageTag != "und" }
            .groupingBy { it.languageTag }
            .eachCount()
            .maxByOrNull { it.value }
            ?.key

    private fun startAsForeground() {
        val openApp = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_mic)
            .setContentTitle(getString(R.string.wake_word_notification_title))
            .setContentText(getString(R.string.wake_word_notification_text))
            .setContentIntent(openApp)
            .setOngoing(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()

        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
        } else {
            0
        }

        ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, type)
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.wake_word_notification_channel),
            NotificationManager.IMPORTANCE_LOW,
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    companion object {
        private const val CHANNEL_ID = "wake_word_service"
        private const val NOTIFICATION_ID = 4101
        private const val OWNER_SAMPLE_SECONDS = 4
    }
}
