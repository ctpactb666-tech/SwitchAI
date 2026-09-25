package com.wstxda.switchai.overlay

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.wstxda.switchai.R
import com.wstxda.switchai.activity.MainActivity
import kotlin.math.abs

class NeonOverlayService : Service() {
    private lateinit var windowManager: WindowManager
    private var overlay: View? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return
        }
        showOverlay()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return START_NOT_STICKY
        }
        if (overlay == null) showOverlay()
        return START_STICKY
    }

    override fun onDestroy() {
        overlay?.let { runCatching { windowManager.removeView(it) } }
        overlay = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun showOverlay() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(13), dp(7), dp(7), dp(7))
            setBackgroundResource(R.drawable.bg_neon_card_glow)
            elevation = dp(10).toFloat()
        }

        val label = TextView(this).apply {
            text = getString(R.string.neon_overlay_ready)
            setTextColor(ContextCompat.getColor(this@NeonOverlayService, R.color.neon_text_primary))
            textSize = 12f
            maxLines = 1
            setPadding(0, 0, dp(10), 0)
        }

        val mic = ImageView(this).apply {
            setImageResource(R.drawable.ic_mic)
            setColorFilter(ContextCompat.getColor(this@NeonOverlayService, android.R.color.white))
            setBackgroundResource(R.drawable.bg_neon_round_button)
            setPadding(dp(13), dp(13), dp(13), dp(13))
            contentDescription = getString(R.string.neon_overlay_open)
            layoutParams = LinearLayout.LayoutParams(dp(48), dp(48))
            setOnClickListener { openSwitchAi() }
        }

        root.addView(label, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        root.addView(mic)

        val params = WindowManager.LayoutParams(
            dp(190),
            dp(62),
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = dp(12)
            y = dp(120)
        }

        var startX = 0
        var startY = 0
        var touchX = 0f
        var touchY = 0f
        var moved = false
        root.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    startX = params.x
                    startY = params.y
                    touchX = event.rawX
                    touchY = event.rawY
                    moved = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (touchX - event.rawX).toInt()
                    val dy = (event.rawY - touchY).toInt()
                    if (abs(dx) > dp(3) || abs(dy) > dp(3)) moved = true
                    params.x = (startX + dx).coerceAtLeast(0)
                    params.y = (startY + dy).coerceAtLeast(0)
                    runCatching { windowManager.updateViewLayout(root, params) }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!moved) openSwitchAi()
                    true
                }
                else -> false
            }
        }

        windowManager.addView(root, params)
        overlay = root
    }

    private fun openSwitchAi() {
        startActivity(
            Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
        )
    }

    private fun buildNotification() = NotificationCompat.Builder(this, CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_mic)
        .setContentTitle(getString(R.string.app_name))
        .setContentText(getString(R.string.neon_overlay_notification))
        .setOngoing(true)
        .setCategory(NotificationCompat.CATEGORY_SERVICE)
        .setContentIntent(
            PendingIntent.getActivity(
                this,
                0,
                Intent(this, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        )
        .build()

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    getString(R.string.neon_overlay_channel),
                    NotificationManager.IMPORTANCE_LOW,
                )
            )
        }
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    companion object {
        private const val CHANNEL_ID = "switchai_overlay"
        private const val NOTIFICATION_ID = 8422

        fun start(context: Context) {
            if (Settings.canDrawOverlays(context)) {
                ContextCompat.startForegroundService(context, Intent(context, NeonOverlayService::class.java))
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, NeonOverlayService::class.java))
        }
    }
}
