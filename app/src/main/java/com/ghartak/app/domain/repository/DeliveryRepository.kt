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

    /** Real-time delivery + rider-location stream for the live tracking screen (Firestore snapshot listeners). */
    fun observeTracking(deliveryId: String): Flow<Result<Pair<Delivery, RiderLocationUpdate?>>>

    /**
     * Cursor-paginated delivery history, newest first. Pass the last item from the previous
     * page as [lastVisible]; omit it for the first page.
     */
    suspend fun getDeliveryHistory(pageSize: Int, lastVisible: Delivery? = null): Result<List<Delivery>>

    suspend fun verifyDeliveryOtp(deliveryId: String, otp: String): Result<Delivery>

    /** Uploads a parcel/delivery-proof photo to Firebase Storage and returns its download URL. */
    suspend fun uploadParcelImage(localFileUri: String, deliveryId: String): Result<String>
}
