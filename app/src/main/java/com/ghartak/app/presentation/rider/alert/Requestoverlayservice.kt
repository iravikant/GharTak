package com.ghartak.app.presentation.rider.alert

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.ghartak.app.R

/**
 * Draws an incoming-request card over whatever the rider is doing (including the
 * home screen) and loops the alert sound until they accept, decline, or it times out.
 *
 * Started by [RiderFcmService] when a "delivery_request" push arrives.
 */
class RequestOverlayService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private val autoDismiss = Runnable { stopSelf() }

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var player: MediaPlayer? = null
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        RequestAlertNotifications.ensureChannels(this)

        val alert = intent?.let { RequestAlert.fromIntent(it) }

        // Must call startForeground quickly after startForegroundService().
        val notification = RequestAlertNotifications.build(
            context = this,
            alert = alert ?: RequestAlert("none", "Parcel", "", "", "", ""),
            channelId = RequestAlertNotifications.CHANNEL_REQUESTS_SILENT
        )
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        if (alert == null || !Settings.canDrawOverlays(this)) {
            stopSelf()
            return START_NOT_STICKY
        }

        removeOverlay()
        showOverlay(alert)
        startSound()
        wakeScreen()

        handler.removeCallbacks(autoDismiss)
        handler.postDelayed(autoDismiss, AUTO_DISMISS_MS)
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacks(autoDismiss)
        removeOverlay()
        stopSound()
        wakeLock?.takeIf { it.isHeld }?.release()
        wakeLock = null
        super.onDestroy()
    }

    // ---------------------------------------------------------------- overlay

    private fun showOverlay(alert: RequestAlert) {
        val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        windowManager = wm

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP
        }

        val view = buildOverlayView(alert)
        overlayView = view
        wm.addView(view, params)
    }

    private fun removeOverlay() {
        overlayView?.let { view ->
            try {
                windowManager?.removeView(view)
            } catch (_: Exception) {
                // already removed
            }
        }
        overlayView = null
    }

    private fun buildOverlayView(alert: RequestAlert): View {
        val brand = Color.parseColor("#2F6BFF")
        val green = Color.parseColor("#1E9E5A")
        val dark = Color.parseColor("#111827")
        val grey = Color.parseColor("#6B7280")

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(18), dp(20), dp(18))
            background = GradientDrawable().apply {
                cornerRadius = dp(24).toFloat()
                setColor(Color.WHITE)
            }
            elevation = dp(12).toFloat()
        }

        // Header: title + earnings
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val titleColumn = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        titleColumn.addView(label("New delivery request", 12f, brand, bold = true))
        titleColumn.addView(label(alert.parcelType, 18f, dark, bold = true))
        header.addView(titleColumn, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        if (alert.earnings.isNotBlank()) {
            val earn = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.END
            }
            earn.addView(label(alert.earnings, 22f, green, bold = true))
            earn.addView(label("you earn", 11f, grey))
            header.addView(earn)
        }
        card.addView(header)

        // Route
        card.addView(label("●  ${alert.pickupArea}", 14f, dark).withTopMargin(14))
        card.addView(label("●  ${alert.dropArea}", 14f, dark).withTopMargin(6))
        if (alert.distanceKm.isNotBlank()) {
            card.addView(label("${alert.distanceKm} km trip", 12f, grey).withTopMargin(8))
        }

        // Buttons
        val buttons = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        val decline = button("Decline", dark, Color.parseColor("#F3F4F6")) { stopSelf() }
        val accept = button("Accept", Color.WHITE, brand) {
            RequestAlertNotifications
                .launchAppPendingIntent(this, alert.requestId.hashCode() + 2, alert.requestId)
                ?.send()
            stopSelf()
        }
        buttons.addView(decline, LinearLayout.LayoutParams(0, dp(48), 1f))
        buttons.addView(
            accept,
            LinearLayout.LayoutParams(0, dp(48), 1f).apply { marginStart = dp(10) }
        )
        card.addView(
            buttons,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(16) }
        )

        // Tapping the body (not the buttons) opens the app without accepting.
        card.setOnClickListener {
            RequestAlertNotifications
                .launchAppPendingIntent(this, alert.requestId.hashCode())
                ?.send()
            stopSelf()
        }

        return FrameLayout(this).apply {
            setPadding(dp(12), dp(40), dp(12), 0)
            addView(card)
        }
    }

    // ------------------------------------------------------------------ audio

    private fun startSound() {
        stopSound()
        try {
            player = MediaPlayer.create(
                applicationContext,
                R.raw.alert_request,
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
                AudioManager.AUDIO_SESSION_ID_GENERATE
            )?.apply {
                isLooping = true
                start()
            }
        } catch (_: Exception) {
            // never crash because of a sound
        }
    }

    private fun stopSound() {
        player?.let {
            try {
                if (it.isPlaying) it.stop()
            } catch (_: Exception) {
            }
            it.release()
        }
        player = null
    }

    @Suppress("DEPRECATION")
    private fun wakeScreen() {
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(
            PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "ghartak:request_alert"
        ).apply { acquire(10_000L) }
    }

    // ---------------------------------------------------------------- helpers

    private fun dp(value: Int): Int =
        TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value.toFloat(),
            resources.displayMetrics
        ).toInt()

    private fun label(text: String, sizeSp: Float, color: Int, bold: Boolean = false) =
        TextView(this).apply {
            this.text = text
            setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp)
            setTextColor(color)
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
            if (bold) setTypeface(typeface, Typeface.BOLD)
        }

    private fun TextView.withTopMargin(top: Int) = apply {
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = dp(top) }
    }

    private fun button(text: String, textColor: Int, bg: Int, onClick: () -> Unit) =
        TextView(this).apply {
            this.text = text
            gravity = Gravity.CENTER
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            setTextColor(textColor)
            setTypeface(typeface, Typeface.BOLD)
            background = GradientDrawable().apply {
                cornerRadius = dp(14).toFloat()
                setColor(bg)
            }
            setOnClickListener { onClick() }
        }

    companion object {
        private const val NOTIFICATION_ID = 7001
        private const val AUTO_DISMISS_MS = 30_000L

        fun intent(context: Context, alert: RequestAlert): Intent =
            alert.putInto(Intent(context, RequestOverlayService::class.java))
    }
}