package com.ghartak.app.domain.repository

import com.ghartak.app.domain.model.Address
import com.ghartak.app.domain.model.Delivery
import com.ghartak.app.domain.model.ParcelType
import com.ghartak.app.domain.model.RiderLocationUpdate
import com.ghartak.app.util.Result
import kotlinx.coroutines.flow.Flow

interface DeliveryRepository {
    suspend fun createDelivery(
        pickup: Address,
        drop: Address,
        receiverName: String,
        receiverPhone: String,
        parcelType: ParcelType,
        parcelWeightKg: Double?,
        parcelDescription: String?,
        parcelImageUrl: String?
    ): Result<Delivery>

    suspend fun cancelDelivery(deliveryId: String): Result<Unit>

    suspend fun getDelivery(deliveryId: String): Result<Delivery>

    /** Polls / streams delivery + rider location for the live tracking screen. */
    fun observeTracking(deliveryId: String): Flow<Result<Pair<Delivery, RiderLocationUpdate?>>>

    suspend fun getDeliveryHistory(page: Int, pageSize: Int): Result<List<Delivery>>

    suspend fun verifyDeliveryOtp(deliveryId: String, otp: String): Result<Delivery>
}
