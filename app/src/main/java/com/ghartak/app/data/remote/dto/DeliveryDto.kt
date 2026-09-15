package com.ghartak.app.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class AddressDto(
    val lat: Double,
    val lng: Double,
    val formattedAddress: String,
    val label: String? = null
)

@Serializable
data class CreateDeliveryRequestDto(
    val pickup: AddressDto,
    val drop: AddressDto,
    val receiverName: String,
    val receiverPhone: String,
    val parcelType: String,
    val parcelWeightKg: Double?,
    val parcelDescription: String?,
    val parcelImageUrl: String?
)

@Serializable
data class DeliveryDto(
    val id: String,
    val customerId: String,
    val riderId: String? = null,
    val pickup: AddressDto,
    val drop: AddressDto,
    val receiverName: String,
    val receiverPhone: String,
    val parcelType: String,
    val parcelWeightKg: Double? = null,
    val parcelDescription: String? = null,
    val parcelImageUrl: String? = null,
    val deliveryFee: Double,
    val platformFee: Double,
    val totalAmount: Double,
    val status: String,
    val paymentStatus: String,
    val createdAt: Long,
    val acceptedAt: Long? = null,
    val pickedUpAt: Long? = null,
    val deliveredAt: Long? = null
)

@Serializable
data class RiderLocationDto(
    val riderId: String,
    val deliveryId: String? = null,
    val lat: Double,
    val lng: Double,
    val bearing: Float? = null,
    val timestamp: Long
)

@Serializable
data class TrackingResponseDto(
    val delivery: DeliveryDto,
    val riderLocation: RiderLocationDto? = null
)

@Serializable
data class VerifyOtpRequestDto(val otp: String)
