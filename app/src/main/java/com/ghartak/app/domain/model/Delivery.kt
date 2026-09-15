package com.ghartak.app.domain.model

data class Delivery(
    val id: String,
    val customerId: String,
    val riderId: String?,
    val pickup: Address,
    val drop: Address,
    val receiverName: String,
    val receiverPhone: String,
    val parcelType: ParcelType,
    val parcelWeightKg: Double?,
    val parcelDescription: String?,
    val parcelImageUrl: String? = null,
    val deliveryFee: Double,
    val platformFee: Double,
    val totalAmount: Double,
    val status: DeliveryStatus,
    val paymentStatus: PaymentStatus,
    val createdAt: Long,
    val acceptedAt: Long? = null,
    val pickedUpAt: Long? = null,
    val deliveredAt: Long? = null
)

enum class PaymentStatus { PENDING, PROCESSING, SUCCESS, FAILED, REFUNDED }
