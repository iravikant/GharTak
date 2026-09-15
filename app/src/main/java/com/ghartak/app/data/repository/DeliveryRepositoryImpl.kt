package com.ghartak.app.data.repository

import com.ghartak.app.data.local.dao.DeliveryDao
import com.ghartak.app.data.model.toDomain
import com.ghartak.app.data.model.toDto
import com.ghartak.app.data.model.toEntity
import com.ghartak.app.data.remote.ApiService
import com.ghartak.app.data.remote.dto.CreateDeliveryRequestDto
import com.ghartak.app.data.remote.dto.VerifyOtpRequestDto
import com.ghartak.app.domain.model.Address
import com.ghartak.app.domain.model.Delivery
import com.ghartak.app.domain.model.ParcelType
import com.ghartak.app.domain.model.RiderLocationUpdate
import com.ghartak.app.domain.repository.DeliveryRepository
import com.ghartak.app.util.Constants
import com.ghartak.app.util.Result
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeliveryRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    private val deliveryDao: DeliveryDao
) : DeliveryRepository {

    override suspend fun createDelivery(
        pickup: Address,
        drop: Address,
        receiverName: String,
        receiverPhone: String,
        parcelType: ParcelType,
        parcelWeightKg: Double?,
        parcelDescription: String?,
        parcelImageUrl: String?
    ): Result<Delivery> = try {
        val response = apiService.createDelivery(
            CreateDeliveryRequestDto(
                pickup = pickup.toDto(),
                drop = drop.toDto(),
                receiverName = receiverName,
                receiverPhone = receiverPhone,
                parcelType = parcelType.name,
                parcelWeightKg = parcelWeightKg,
                parcelDescription = parcelDescription,
                parcelImageUrl = parcelImageUrl
            )
        )
        val body = response.data
        if (response.success && body != null) {
            deliveryDao.upsert(body.toEntity())
            Result.Success(body.toDomain())
        } else {
            Result.Error(response.message ?: "Unable to create delivery", response.errorCode)
        }
    } catch (e: Exception) {
        Result.Error(e.message ?: "Unable to reach the server")
    }

    override suspend fun cancelDelivery(deliveryId: String): Result<Unit> = try {
        val response = apiService.cancelDelivery(deliveryId)
        if (response.success) Result.Success(Unit)
        else Result.Error(response.message ?: "Unable to cancel delivery", response.errorCode)
    } catch (e: Exception) {
        Result.Error(e.message ?: "Unable to reach the server")
    }

    override suspend fun getDelivery(deliveryId: String): Result<Delivery> = try {
        val response = apiService.getDelivery(deliveryId)
        val body = response.data
        if (response.success && body != null) {
            deliveryDao.upsert(body.toEntity())
            Result.Success(body.toDomain())
        } else {
            Result.Error(response.message ?: "Delivery not found", response.errorCode)
        }
    } catch (e: Exception) {
        Result.Error(e.message ?: "Unable to reach the server")
    }

    /**
     * Polls the tracking endpoint on the cadence from spec section 6/21. A real-time transport
     * (WebSocket/FCM data messages) can later replace this poll loop without touching call sites,
     * since ViewModels only ever see this Flow.
     */
    override fun observeTracking(deliveryId: String): Flow<Result<Pair<Delivery, RiderLocationUpdate?>>> = flow {
        while (true) {
            val result = try {
                val response = apiService.getTracking(deliveryId)
                val body = response.data
                if (response.success && body != null) {
                    val delivery = body.delivery.toDomain()
                    deliveryDao.upsert(body.delivery.toEntity())
                    Result.Success(delivery to body.riderLocation?.toDomain())
                } else {
                    Result.Error(response.message ?: "Unable to fetch tracking info", response.errorCode)
                }
            } catch (e: Exception) {
                Result.Error(e.message ?: "Unable to reach the server")
            }
            emit(result)

            val isTerminal = (result as? Result.Success)?.data?.first?.status?.isTerminal == true
            if (isTerminal) break
            delay(Constants.ACTIVE_DELIVERY_LOCATION_INTERVAL_MS)
        }
    }

    override suspend fun getDeliveryHistory(page: Int, pageSize: Int): Result<List<Delivery>> = try {
        val response = apiService.getDeliveryHistory(page, pageSize)
        val body = response.data
        if (response.success && body != null) {
            deliveryDao.upsertAll(body.map { it.toEntity() })
            Result.Success(body.map { it.toDomain() })
        } else {
            Result.Error(response.message ?: "Unable to load delivery history", response.errorCode)
        }
    } catch (e: Exception) {
        // Note: for full offline support, callers can fall back to deliveryDao.observeAll()
        // (spec section 24) when this returns an Error; kept as an explicit caller decision
        // rather than silently swapping in stale data here.
        Result.Error(e.message ?: "Unable to reach the server")
    }

    override suspend fun verifyDeliveryOtp(deliveryId: String, otp: String): Result<Delivery> = try {
        val response = apiService.verifyOtp(deliveryId, VerifyOtpRequestDto(otp))
        val body = response.data
        if (response.success && body != null) {
            deliveryDao.upsert(body.toEntity())
            Result.Success(body.toDomain())
        } else {
            Result.Error(response.message ?: "Invalid or expired OTP", response.errorCode)
        }
    } catch (e: Exception) {
        Result.Error(e.message ?: "Unable to reach the server")
    }
}
