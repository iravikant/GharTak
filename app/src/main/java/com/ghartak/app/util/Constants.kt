package com.ghartak.app.util

object Constants {
    const val PREFS_NAME = "ghartak_prefs"
    const val KEY_AUTH_TOKEN = "auth_token"
    const val KEY_USER_ID = "user_id"

    // Location update cadence (spec section 6/21): keep the customer-facing tracking screen
    // near-real-time without draining the rider's battery when idle.
    const val ACTIVE_DELIVERY_LOCATION_INTERVAL_MS = 4_000L
    const val IDLE_ONLINE_LOCATION_INTERVAL_MS = 30_000L

    const val OTP_LENGTH = 4
    const val DELIVERY_HISTORY_PAGE_SIZE = 20
}
