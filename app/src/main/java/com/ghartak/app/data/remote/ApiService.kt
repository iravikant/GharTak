package com.ghartak.app.data.remote

import com.ghartak.app.data.remote.dto.ApiEnvelope
import com.ghartak.app.data.remote.dto.AuthResponseDto
import com.ghartak.app.data.remote.dto.CreateDeliveryRequestDto
import com.ghartak.app.data.remote.dto.DeliveryDto
import com.ghartak.app.data.remote.dto.LoginRequestDto
import com.ghartak.app.data.remote.dto.RegisterRequestDto
import com.ghartak.app.data.remote.dto.TrackingResponseDto
import com.ghartak.app.data.remote.dto.VerifyOtpRequestDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * Maps 1:1 to the PHP REST API surface in spec section 16. Kept flat and role-agnostic here;
 * a future rider-app module would reuse this same interface.
 */
interface ApiService {

    @POST("api/auth/login")
    suspend fun login(@Body body: LoginRequestDto): ApiEnvelope<AuthResponseDto>

    @POST("api/auth/register")
    suspend fun register(@Body body: RegisterRequestDto): ApiEnvelope<AuthResponseDto>

    @POST("api/auth/logout")
    suspend fun logout(): ApiEnvelope<Unit>

    @POST("api/deliveries")
    suspend fun createDelivery(@Body body: CreateDeliveryRequestDto): ApiEnvelope<DeliveryDto>

    @GET("api/deliveries")
    suspend fun getDeliveryHistory(
        @Query("page") page: Int,
        @Query("pageSize") pageSize: Int
    ): ApiEnvelope<List<DeliveryDto>>

    @GET("api/deliveries/{id}")
    suspend fun getDelivery(@Path("id") deliveryId: String): ApiEnvelope<DeliveryDto>

    @POST("api/deliveries/{id}/cancel")
    suspend fun cancelDelivery(@Path("id") deliveryId: String): ApiEnvelope<Unit>

    @GET("api/deliveries/{id}/tracking")
    suspend fun getTracking(@Path("id") deliveryId: String): ApiEnvelope<TrackingResponseDto>

    @POST("api/deliveries/{id}/verify-otp")
    suspend fun verifyOtp(
        @Path("id") deliveryId: String,
        @Body body: VerifyOtpRequestDto
    ): ApiEnvelope<DeliveryDto>

    @GET("api/health")
    suspend fun healthCheck(): ApiEnvelope<Unit>
}
