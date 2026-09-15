package com.ghartak.app.data.model

import com.ghartak.app.data.local.entity.DeliveryEntity
import com.ghartak.app.data.remote.dto.AddressDto
import com.ghartak.app.data.remote.dto.DeliveryDto
import com.ghartak.app.data.remote.dto.RiderLocationDto
import com.ghartak.app.data.remote.dto.UserDto
import com.ghartak.app.domain.model.Address
import com.ghartak.app.domain.model.Delivery
import com.ghartak.app.domain.model.DeliveryStatus
import com.ghartak.app.domain.model.LatLngPoint
import com.ghartak.app.domain.model.ParcelType
import com.ghartak.app.domain.model.PaymentStatus
import com.ghartak.app.domain.model.RiderLocationUpdate
import com.ghartak.app.domain.model.User
import com.ghartak.app.domain.model.UserRole

fun UserDto.toDomain(): User = User(
    id = id,
    name = name,
    phone = phone,
    email = email,
    role = runCatching { UserRole.valueOf(role.uppercase()) }.getOrDefault(UserRole.CUSTOMER),
    profilePhotoUrl = profilePhotoUrl,
    rating = rating
)

fun Address.toDto(): AddressDto = AddressDto(
    lat = point.lat,
    lng = point.lng,
    formattedAddress = formattedAddress,
    label = label
)

fun AddressDto.toDomain(): Address = Address(
    point = LatLngPoint(lat, lng),
    formattedAddress = formattedAddress,
    label = label
)

fun DeliveryDto.toDomain(): Delivery = Delivery(
    id = id,
    customerId = customerId,
    riderId = riderId,
    pickup = pickup.toDomain(),
    drop = drop.toDomain(),
    receiverName = receiverName,
    receiverPhone = receiverPhone,
    parcelType = runCatching { ParcelType.valueOf(parcelType.uppercase()) }.getOrDefault(ParcelType.OTHER),
    parcelWeightKg = parcelWeightKg,
    parcelDescription = parcelDescription,
    parcelImageUrl = parcelImageUrl,
    deliveryFee = deliveryFee,
    platformFee = platformFee,
    totalAmount = totalAmount,
    status = runCatching { DeliveryStatus.valueOf(status.uppercase()) }.getOrDefault(DeliveryStatus.CREATED),
    paymentStatus = runCatching { PaymentStatus.valueOf(paymentStatus.uppercase()) }.getOrDefault(PaymentStatus.PENDING),
    createdAt = createdAt,
    acceptedAt = acceptedAt,
    pickedUpAt = pickedUpAt,
    deliveredAt = deliveredAt
)

fun DeliveryDto.toEntity(): DeliveryEntity = DeliveryEntity(
    id = id,
    customerId = customerId,
    riderId = riderId,
    pickupLat = pickup.lat,
    pickupLng = pickup.lng,
    pickupAddress = pickup.formattedAddress,
    dropLat = drop.lat,
    dropLng = drop.lng,
    dropAddress = drop.formattedAddress,
    receiverName = receiverName,
    receiverPhone = receiverPhone,
    parcelType = parcelType,
    parcelWeightKg = parcelWeightKg,
    parcelDescription = parcelDescription,
    deliveryFee = deliveryFee,
    platformFee = platformFee,
    totalAmount = totalAmount,
    status = status,
    paymentStatus = paymentStatus,
    createdAt = createdAt,
    updatedAt = System.currentTimeMillis()
)

fun DeliveryEntity.toDomain(): Delivery = Delivery(
    id = id,
    customerId = customerId,
    riderId = riderId,
    pickup = Address(LatLngPoint(pickupLat, pickupLng), pickupAddress),
    drop = Address(LatLngPoint(dropLat, dropLng), dropAddress),
    receiverName = receiverName,
    receiverPhone = receiverPhone,
    parcelType = runCatching { ParcelType.valueOf(parcelType.uppercase()) }.getOrDefault(ParcelType.OTHER),
    parcelWeightKg = parcelWeightKg,
    parcelDescription = parcelDescription,
    deliveryFee = deliveryFee,
    platformFee = platformFee,
    totalAmount = totalAmount,
    status = runCatching { DeliveryStatus.valueOf(status.uppercase()) }.getOrDefault(DeliveryStatus.CREATED),
    paymentStatus = runCatching { PaymentStatus.valueOf(paymentStatus.uppercase()) }.getOrDefault(PaymentStatus.PENDING),
    createdAt = createdAt
)

fun RiderLocationDto.toDomain(): RiderLocationUpdate = RiderLocationUpdate(
    riderId = riderId,
    deliveryId = deliveryId,
    point = LatLngPoint(lat, lng),
    bearing = bearing,
    timestamp = timestamp
)
