package com.ghartak.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Local cache of deliveries for offline history viewing (spec section 24 — "cache frequently
 * used data"). The server remains the source of truth; this is read-through cache only.
 */
@Entity(tableName = "deliveries")
data class DeliveryEntity(
    @PrimaryKey val id: String,
    val customerId: String,
    val riderId: String?,
    val pickupLat: Double,
    val pickupLng: Double,
    val pickupAddress: String,
    val dropLat: Double,
    val dropLng: Double,
    val dropAddress: String,
    val receiverName: String,
    val receiverPhone: String,
    val parcelType: String,
    val parcelWeightKg: Double?,
    val parcelDescription: String?,
    val deliveryFee: Double,
    val platformFee: Double,
    val totalAmount: Double,
    val status: String,
    val paymentStatus: String,
    val createdAt: Long,
    val updatedAt: Long
)
