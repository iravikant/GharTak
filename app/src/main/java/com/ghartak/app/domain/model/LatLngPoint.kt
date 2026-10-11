package com.ghartak.app.domain.model

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class LatLngPoint(val lat: Double, val lng: Double) {

    /** Great-circle (Haversine) distance in kilometres. */
    fun distanceKmTo(other: LatLngPoint): Double {
        val earthRadiusKm = 6371.0
        val dLat = Math.toRadians(other.lat - lat)
        val dLng = Math.toRadians(other.lng - lng)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat)) * cos(Math.toRadians(other.lat)) *
            sin(dLng / 2) * sin(dLng / 2)
        return earthRadiusKm * 2 * atan2(sqrt(a), sqrt(1 - a))
    }
}

data class Address(
    val point: LatLngPoint,
    val formattedAddress: String,
    val label: String? = null // e.g. "Home", "Office" for saved addresses
)
