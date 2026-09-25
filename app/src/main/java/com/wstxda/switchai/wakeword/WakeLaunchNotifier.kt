package com.wstxda.switchai.wakeword

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.wstxda.switchai.R
import com.wstxda.switchai.ui.catalog.AssistantCatalog
import com.wstxda.switchai.utils.AssistantsMap

/**
 * Heads-up notification "SwitchAI — Разговор с ChatGPT…" (last screen of the mockup).
 *
 * It also works as a fallback: Android 10+ may block starting an activity
 * from a background service, and then a tap on this notification opens the assistant.
 */
object WakeLaunchNotifier {

    private const val CHANNEL_ID = "wake_word_launch"
    private const val NOTIFICATION_ID = 4102
    private const val TIMEOUT_MS = 12_000L

    fun showConversation(context: Context, phrase: WakePhrase) =
        show(context, phrase, context.getString(R.string.neon_notification_conversation, name(context, phrase)))

    fun showTapToStart(context: Context, phrase: WakePhrase) =
        show(context, phrase, context.getString(R.string.neon_notification_tap_to_start, name(context, phrase)))

    fun dismiss(context: Context) {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
    }

    private fun name(context: Context, phrase: WakePhrase) =
        AssistantCatalog.find(context, phrase.assistantKey)?.name ?: phrase.assistantKey

    @SuppressLint("MissingPermission") // checked in canNotify()
    private fun show(context: Context, phrase: WakePhrase, text: String) {
        if (!canNotify(context)) return
        val activity = AssistantsMap.assistantActivity[phrase.assistantKey] ?: return
        createChannel(context)

        val open = PendingIntent.getActivity(
            context,
            phrase.assistantKey.hashCode(),
            Intent(context, activity).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val close = PendingIntent.getBroadcast(
            context,
            0,
            Intent(context, DismissReceiver::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_wave_bars)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setContentIntent(open)
            .setAutoCancel(true)
            .setTimeoutAfter(TIMEOUT_MS)
            .addAction(R.drawable.ic_close, context.getString(R.string.neon_close), close)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    private fun canNotify(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    private fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.neon_notification_channel),
            NotificationManager.IMPORTANCE_HIGH,
        )
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    /** Handles the "×" action of the notification. */
    class DismissReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) = dismiss(context)
    }
}
