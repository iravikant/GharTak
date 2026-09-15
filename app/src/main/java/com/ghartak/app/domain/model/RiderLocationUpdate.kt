package com.ghartak.app.domain.model

data class RiderLocationUpdate(
    val riderId: String,
    val deliveryId: String?,
    val point: LatLngPoint,
    val bearing: Float? = null,
    val timestamp: Long
)
