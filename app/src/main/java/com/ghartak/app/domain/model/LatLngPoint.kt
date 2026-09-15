package com.ghartak.app.domain.model

data class LatLngPoint(val lat: Double, val lng: Double)

data class Address(
    val point: LatLngPoint,
    val formattedAddress: String,
    val label: String? = null // e.g. "Home", "Office" for saved addresses
)
