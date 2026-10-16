package com.ghartak.app.presentation.rider.alert

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import com.ghartak.app.R

/** Data carried by the FCM push and handed to the overlay / notification. */
data class RequestAlert(
    val requestId: String,
    val parcelType: String,
    val pickupArea: String,
    val dropArea: String,
    val earnings: String,
    val distanceKm: String
) {
    fun putInto(intent: Intent): Intent = intent.apply {
        putExtra("requestId", requestId)
        putExtra("parcelType", parcelType)
        putExtra("pickupArea", pickupArea)
        putExtra("dropArea", dropArea)
        putExtra("earnings", earnings)
        putExtra("distanceKm", distanceKm)
    }

    companion object {
        const val TYPE = "delivery_request"

        /** Extra sent to the launcher activity when the rider taps Accept. */
        const val EXTRA_ACCEPT_REQUEST_ID = "rider_accept_request_id"

        fun fromData(data: Map<String, String>): RequestAlert? {
            val id = data["requestId"] ?: return null
            return RequestAlert(
                requestId = id,
                parcelType = data["parcelType"].orEmpty().ifBlank { "Parcel" },
                pickupArea = data["pickupArea"].orEmpty(),
                dropArea = data["dropArea"].orEmpty(),
                earnings = data["earnings"].orEmpty(),
                distanceKm = data["distanceKm"].orEmpty()
            )
        }

        fun fromIntent(intent: Intent): RequestAlert? =
            fromData(
                listOf("requestId", "parcelType", "pickupArea", "dropArea", "earnings", "distanceKm")
                    .mapNotNull { key -> intent.getStringExtra(key)?.let { key to it } }
                    .toMap()
            )
    }
}

object RequestAlertNotifications {
    /** Loud channel with the custom sound (used when overlay permission is missing). */
    const val CHANNEL_REQUESTS = "rider_requests_v1"

    /** High-importance but silent channel (the overlay service plays its own sound). */
    const val CHANNEL_REQUESTS_SILENT = "rider_requests_silent_v1"

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)

        val soundUri = Uri.parse(
            "${ContentResolver.SCHEME_ANDROID_RESOURCE}://${context.packageName}/${R.raw.alert_request}"
        )
        val loud = NotificationChannel(
            CHANNEL_REQUESTS,
            "Delivery requests",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Alerts for new delivery requests"
            setSound(
                soundUri,
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            enableVibration(true)
        }

        val silent = NotificationChannel(
            CHANNEL_REQUESTS_SILENT,
            "Delivery request pop-up",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Shown together with the on-screen request pop-up"
            setSound(null, null)
            enableVibration(false)
        }

        manager.createNotificationChannel(loud)
        manager.createNotificationChannel(silent)
    }

    /** Intent that opens the app, optionally telling it to auto-accept [acceptRequestId]. */
    fun launchAppPendingIntent(
        context: Context,
        requestCode: Int,
        acceptRequestId: String? = null
    ): PendingIntent? {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                acceptRequestId?.let { putExtra(RequestAlert.EXTRA_ACCEPT_REQUEST_ID, it) }
            } ?: return null
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun build(context: Context, alert: RequestAlert, channelId: String) =
        NotificationCompat.Builder(context, channelId)
            // TODO: replace with a monochrome notification icon from your drawables.
            .setSmallIcon(context.applicationInfo.icon)
            .setContentTitle("New delivery request" + alert.earnings.takeIf { it.isNotBlank() }?.let { " · $it" }.orEmpty())
            .setContentText("${alert.pickupArea} → ${alert.dropArea}")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("${alert.parcelType} · ${alert.distanceKm} km\n${alert.pickupArea} → ${alert.dropArea}")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setTimeoutAfter(30_000)
            .setContentIntent(launchAppPendingIntent(context, alert.requestId.hashCode()))
            .addAction(
                0,
                "Accept",
                launchAppPendingIntent(context, alert.requestId.hashCode() + 1, alert.requestId)
            )
            .build()

    /** Fallback used when the overlay permission isn't granted. */
    fun showFallback(context: Context, alert: RequestAlert) {
        ensureChannels(context)
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.notify(alert.requestId.hashCode(), build(context, alert, CHANNEL_REQUESTS))
    }
}