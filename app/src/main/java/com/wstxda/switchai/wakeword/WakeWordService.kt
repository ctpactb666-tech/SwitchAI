package com.wstxda.switchai.wakeword

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.wstxda.switchai.R

class WakeWordService : Service() {

    private val ownerSecurity by lazy { OwnerVoiceSecurity(this) }
    private val coordinator by lazy { WakeWordCoordinator(this) }
    private var phraseRecognizer: OnDevicePhraseRecognizer? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startAsForeground()

        phraseRecognizer = OnDevicePhraseRecognizer(
            context = this,
            onText = { recognized ->
                when (coordinator.onRecognizedText(recognized)) {
                    WakeWordCoordinator.Result.Locked -> stopSelf()
                    else -> Unit
                }
            },
            onUnavailable = { stopSelf() },
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (ownerSecurity.isLocked) {
            stopSelf()
            return START_NOT_STICKY
        }

        phraseRecognizer?.start()
        return START_STICKY
    }

    override fun onDestroy() {
        phraseRecognizer?.stop()
        phraseRecognizer = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startAsForeground() {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_voice_input)
            .setContentTitle(getString(R.string.wake_word_notification_title))
            .setContentText(getString(R.string.wake_word_notification_text))
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
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
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
    }
}
