package com.ghartak.app.data.model

import com.ghartak.app.domain.model.Address
import com.ghartak.app.domain.model.Delivery
import com.ghartak.app.domain.model.DeliveryStatus
import com.ghartak.app.domain.model.LatLngPoint
import com.ghartak.app.domain.model.ParcelType
import com.ghartak.app.domain.model.PaymentStatus
import com.ghartak.app.domain.model.RiderLocationUpdate
import com.ghartak.app.domain.model.User
import com.ghartak.app.domain.model.UserRole
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue

/**
 * Hand-rolled Firestore <-> domain mapping (rather than Firestore's @DocumentId/data-class
 * auto-mapping) so every field has an explicit fallback — a malformed or partially-written
 * document degrades gracefully instead of throwing during deserialization.
 */

private fun DocumentSnapshot.str(field: String): String = getString(field).orEmpty()
private fun DocumentSnapshot.strOrNull(field: String): String? = getString(field)
private fun DocumentSnapshot.dbl(field: String): Double = getDouble(field) ?: 0.0
private fun DocumentSnapshot.dblOrNull(field: String): Double? = getDouble(field)
private fun DocumentSnapshot.millis(field: String): Long = getTimestamp(field)?.toDate()?.time ?: 0L
private fun DocumentSnapshot.millisOrNull(field: String): Long? = getTimestamp(field)?.toDate()?.time

// ---------- User ----------

fun User.toUserMap(): Map<String, Any?> = mapOf(
    "name" to name,
    "phone" to phone,
    "email" to email,
    "role" to role.name,
    "profilePhotoUrl" to profilePhotoUrl,
    "rating" to rating
)

fun DocumentSnapshot.toDomain(): User = User(
    id = id,
    name = str("name"),
    phone = str("phone"),
    email = strOrNull("email"),
    role = runCatching { UserRole.valueOf(str("role").ifBlank { "CUSTOMER" }) }.getOrDefault(UserRole.CUSTOMER),
    profilePhotoUrl = strOrNull("profilePhotoUrl"),
    rating = dblOrNull("rating")
)

// ---------- Address (embedded map, not its own collection) ----------

private fun Address.toMap(): Map<String, Any?> = mapOf(
    "lat" to point.lat,
    "lng" to point.lng,
    "formattedAddress" to formattedAddress,
    "label" to label
)

@Suppress("UNCHECKED_CAST")
private fun Map<String, Any?>?.toAddress(): Address {
    val map = this.orEmpty()
    return Address(
        point = LatLngPoint(
            lat = (map["lat"] as? Number)?.toDouble() ?: 0.0,
            lng = (map["lng"] as? Number)?.toDouble() ?: 0.0
        ),
        formattedAddress = map["formattedAddress"] as? String ?: "",
        label = map["label"] as? String
    )
}

// ---------- Delivery ----------

/** Fields for a new delivery; createdAt/status/paymentStatus/otp are set by the repository. */
fun buildDeliveryCreateMap(
    customerId: String,
    pickup: Address,
    drop: Address,
    receiverName: String,
    receiverPhone: String,
    parcelType: ParcelType,
    parcelWeightKg: Double?,
    parcelDescription: String?,
    parcelImageUrl: String?,
    deliveryFee: Double,
    platformFee: Double,
    otp: String
): Map<String, Any?> = mapOf(
    "customerId" to customerId,
    "riderId" to null,
    "pickup" to pickup.toMap(),
    "drop" to drop.toMap(),
    "receiverName" to receiverName,
    "receiverPhone" to receiverPhone,
    "parcelType" to parcelType.name,
    "parcelWeightKg" to parcelWeightKg,
    "parcelDescription" to parcelDescription,
    "parcelImageUrl" to parcelImageUrl,
    "deliveryFee" to deliveryFee,
    "platformFee" to platformFee,
    "totalAmount" to deliveryFee + platformFee,
    "status" to DeliveryStatus.SEARCHING_RIDER.name,
    "paymentStatus" to PaymentStatus.PENDING.name,
    "otp" to otp,
    "createdAt" to FieldValue.serverTimestamp()
)

@Suppress("UNCHECKED_CAST")
fun DocumentSnapshot.toDelivery(): Delivery = Delivery(
    id = id,
    customerId = str("customerId"),
    riderId = strOrNull("riderId"),
    pickup = (get("pickup") as? Map<String, Any?>).toAddress(),
    drop = (get("drop") as? Map<String, Any?>).toAddress(),
    receiverName = str("receiverName"),
    receiverPhone = str("receiverPhone"),
    parcelType = runCatching { ParcelType.valueOf(str("parcelType").ifBlank { "OTHER" }) }.getOrDefault(ParcelType.OTHER),
    parcelWeightKg = dblOrNull("parcelWeightKg"),
    parcelDescription = strOrNull("parcelDescription"),
    parcelImageUrl = strOrNull("parcelImageUrl"),
    deliveryFee = dbl("deliveryFee"),
    platformFee = dbl("platformFee"),
    totalAmount = dbl("totalAmount"),
    status = runCatching { DeliveryStatus.valueOf(str("status").ifBlank { "CREATED" }) }.getOrDefault(DeliveryStatus.CREATED),
    paymentStatus = runCatching { PaymentStatus.valueOf(str("paymentStatus").ifBlank { "PENDING" }) }.getOrDefault(PaymentStatus.PENDING),
    createdAt = millis("createdAt"),
    acceptedAt = millisOrNull("acceptedAt"),
    pickedUpAt = millisOrNull("pickedUpAt"),
    deliveredAt = millisOrNull("deliveredAt")
)

// ---------- Rider location ----------

fun RiderLocationUpdate.toMap(): Map<String, Any?> = mapOf(
    "deliveryId" to deliveryId,
    "lat" to point.lat,
    "lng" to point.lng,
    "bearing" to bearing,
    "timestamp" to Timestamp(java.util.Date(timestamp))
)

fun DocumentSnapshot.toRiderLocation(): RiderLocationUpdate? {
    if (!exists()) return null
    return RiderLocationUpdate(
        riderId = id,
        deliveryId = strOrNull("deliveryId"),
        point = LatLngPoint(dbl("lat"), dbl("lng")),
        bearing = getDouble("bearing")?.toFloat(),
        timestamp = millis("timestamp")
    )
}
