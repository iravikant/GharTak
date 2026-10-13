package com.ghartak.app.util

object Constants {
    /** Shown on the Help screen — replace with your real support address before launch. */
    const val SUPPORT_EMAIL = "support@yelo.example"

    /**
     * DEV ONLY. false = new riders start as PENDING and an admin approves them (set approvalStatus to
     * APPROVED in the Firestore console). true = riders are approved instantly — only works if you also
     * relax the riders create rule in firebase/firestore.rules (see the comment there).
     */
    const val AUTO_APPROVE_RIDERS = false

    /** Result.Error.code used when the user dismissed a system sign-in sheet (not a real failure). */
    const val ERROR_CODE_CANCELLED = "CANCELLED"
    const val RESEND_OTP_SECONDS = 30

    // Rider foreground-service location update cadence (spec section 6/21): keeps the
    // customer-facing tracking screen responsive without draining the rider's battery when idle.
    // Firestore pushes these updates to listeners instantly once written — this interval only
    // governs how often the rider app itself writes a new location.
    const val ACTIVE_DELIVERY_LOCATION_INTERVAL_MS = 4_000L
    const val IDLE_ONLINE_LOCATION_INTERVAL_MS = 30_000L

    const val DELIVERY_OTP_LENGTH = 4
    const val LOGIN_OTP_LENGTH = 6
    const val DELIVERY_HISTORY_PAGE_SIZE = 20
}
