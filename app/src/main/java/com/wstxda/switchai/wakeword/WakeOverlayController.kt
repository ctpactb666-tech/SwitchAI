package com.wstxda.switchai.wakeword

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import androidx.core.content.ContextCompat
import com.wstxda.switchai.R
import com.wstxda.switchai.activity.MainActivity
import kotlin.math.abs

/** Compact draggable microphone shown above other apps. */
class WakeOverlayController(private val context: Context) {
    private val windowManager = context.getSystemService(WindowManager::class.java)
    private var bubble: ImageView? = null
    private var params: WindowManager.LayoutParams? = null

    fun canDraw(): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(context)

    fun permissionIntent(): Intent = Intent(
        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
        Uri.parse("package:${context.packageName}")
    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    fun show() {
        if (!canDraw() || bubble != null) return
        val size = dp(58)
        val p = WindowManager.LayoutParams(
            size, size,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = dp(14)
            y = dp(180)
        }

        val view = ImageView(context).apply {
            setImageResource(R.drawable.ic_mic)
            setPadding(dp(17), dp(17), dp(17), dp(17))
            background = bubbleBackground(false)
            elevation = dp(12).toFloat()
        }
        installDrag(view, p)
        windowManager.addView(view, p)
        bubble = view
        params = p
    }

    fun render(status: WakeWordState.Status) {
        bubble?.background = bubbleBackground(status == WakeWordState.Status.LISTENING)
        bubble?.alpha = if (status == WakeWordState.Status.LOCKED) 0.55f else 1f
    }

    fun hide() {
        bubble?.let { runCatching { windowManager.removeView(it) } }
        bubble = null
        params = null
    }

    private fun installDrag(view: View, p: WindowManager.LayoutParams) {
        var downX = 0f
        var downY = 0f
        var startX = 0
        var startY = 0
        view.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.rawX; downY = event.rawY
                    startX = p.x; startY = p.y
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    p.x = (startX - (event.rawX - downX)).toInt().coerceAtLeast(0)
                    p.y = (startY + (event.rawY - downY)).toInt().coerceAtLeast(0)
                    bubble?.let { windowManager.updateViewLayout(it, p) }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (abs(event.rawX - downX) < dp(8) && abs(event.rawY - downY) < dp(8)) {
                        context.startActivity(Intent(context, MainActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                        })
                    }
                    true
                }
                else -> false
            }
        }
    }

    private fun bubbleBackground(active: Boolean) = GradientDrawable(
        GradientDrawable.Orientation.TL_BR,
        intArrayOf(
            Color.parseColor(if (active) "#14D9FF" else "#182A55"),
            Color.parseColor(if (active) "#7357FF" else "#392D78"),
            Color.parseColor("#B84DFF")
        )
    ).apply {
        shape = GradientDrawable.OVAL
        setStroke(dp(2), Color.parseColor(if (active) "#91F5FF" else "#6E65D8"))
    }

    private fun dp(value: Int) = (value * context.resources.displayMetrics.density).toInt()
}
