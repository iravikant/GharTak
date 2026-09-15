package com.ghartak.app.domain.model

/**
 * Mirrors the backend delivery state machine (spec section 7). Order matters: it drives the
 * customer-facing timeline UI, so keep it in chronological sequence.
 */
enum class DeliveryStatus {
    CREATED,
    SEARCHING_RIDER,
    RIDER_ASSIGNED,
    RIDER_GOING_TO_PICKUP,
    ARRIVED_AT_PICKUP,
    PARCEL_PICKED_UP,
    OUT_FOR_DELIVERY,
    RIDER_NEAR_DESTINATION,
    DELIVERED,
    CANCELLED,
    FAILED;

    val isTerminal: Boolean
        get() = this == DELIVERED || this == CANCELLED || this == FAILED

    val isActiveTracking: Boolean
        get() = this in listOf(RIDER_GOING_TO_PICKUP, ARRIVED_AT_PICKUP, PARCEL_PICKED_UP, OUT_FOR_DELIVERY, RIDER_NEAR_DESTINATION)
}
