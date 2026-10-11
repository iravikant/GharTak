package com.ghartak.app.domain.repository

import com.ghartak.app.domain.model.Address
import com.ghartak.app.domain.model.Delivery
import com.ghartak.app.domain.model.ParcelType
import com.ghartak.app.domain.model.PaymentMethod
import com.ghartak.app.domain.model.RiderLocationUpdate
import com.ghartak.app.util.Result
import kotlinx.coroutines.flow.Flow

interface DeliveryRepository {
    /**
     * Creates the delivery. When [parcelImageUri] is given the photo is uploaded first; if the
     * upload fails the booking still goes through without a photo.
     */
    suspend fun createDelivery(
        pickup: Address,
        drop: Address,
        receiverName: String,
        receiverPhone: String,
        parcelType: ParcelType,
        parcelWeightKg: Double?,
        parcelDescription: String?,
        specialInstructions: String?,
        parcelImageUri: String?,
        paymentMethod: PaymentMethod,
        deliveryFee: Double,
        platformFee: Double,
        distanceKm: Double
    ): Result<Delivery>

    suspend fun cancelDelivery(deliveryId: String): Result<Unit>

    suspend fun getDelivery(deliveryId: String): Result<Delivery>

    /** Real-time delivery + rider-location stream (Firestore snapshot listeners). */
    fun observeTracking(deliveryId: String): Flow<Result<Pair<Delivery, RiderLocationUpdate?>>>

    /** Cursor-paginated history, newest first. Pass the last item of the previous page as [lastVisible]. */
    suspend fun getDeliveryHistory(pageSize: Int, lastVisible: Delivery? = null): Result<List<Delivery>>

    suspend fun verifyDeliveryOtp(deliveryId: String, otp: String): Result<Delivery>

    suspend fun uploadParcelImage(localFileUri: String, deliveryId: String): Result<String>
}
