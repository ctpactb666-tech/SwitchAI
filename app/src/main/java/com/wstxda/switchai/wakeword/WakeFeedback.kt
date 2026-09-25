package com.wstxda.switchai.wakeword

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.net.toUri
import com.wstxda.switchai.R

/** "Звуковое подтверждение" and "Вибрация при срабатывании". */
object WakeFeedback {

    fun onPhraseRecognized(context: Context) {
        if (WakeWordSettings.getBoolean(context, WakeWordSettings.KEY_SOUND)) playSound(context)
        if (WakeWordSettings.getBoolean(context, WakeWordSettings.KEY_VIBRATION)) vibrate(context)
    }

    private fun playSound(context: Context) {
        runCatching {
            MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANT)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setDataSource(
                    context,
                    "android.resource://${context.packageName}/${R.raw.open_sound}".toUri(),
                )
                setOnPreparedListener { it.start() }
                setOnCompletionListener { it.release() }
                setOnErrorListener { mp, _, _ -> mp.release(); true }
                prepareAsync()
            }
        }
    }

    private fun vibrate(context: Context) {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            context.getSystemService(Vibrator::class.java)
        } ?: return
        runCatching { vibrator.vibrate(VibrationEffect.createOneShot(45, VibrationEffect.DEFAULT_AMPLITUDE)) }
    }
}
