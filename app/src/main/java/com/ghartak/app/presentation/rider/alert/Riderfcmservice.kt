package com.ghartak.app.presentation.rider.alert

import android.provider.Settings
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * Receives data-only, high-priority FCM messages. Works when the app is in the
 * background or has been swiped away (but not after a "Force stop").
 *
 * If you already have a FirebaseMessagingService, merge [onMessageReceived]
 * into it instead of adding a second one.
 */
class RiderFcmService : FirebaseMessagingService() {

    override fun onMessageReceived(message: RemoteMessage) {
        val data = message.data
        if (data["type"] != RequestAlert.TYPE) return
        val alert = RequestAlert.fromData(data) ?: return

        if (Settings.canDrawOverlays(this)) {
            // High-priority FCM is allowed to start a foreground service from the background.
            ContextCompat.startForegroundService(
                this,
                RequestOverlayService.intent(this, alert)
            )
        } else {
            // No "display over other apps" permission: loud heads-up notification instead.
            RequestAlertNotifications.showFallback(this, alert)
        }
    }

    override fun onNewToken(token: String) {
        // TODO: send [token] to your backend so it can target this rider
        // (you may already do this elsewhere).
    }
}