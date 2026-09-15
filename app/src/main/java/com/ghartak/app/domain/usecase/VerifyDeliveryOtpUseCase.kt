package com.ghartak.app.domain.usecase

import com.ghartak.app.domain.model.Delivery
import com.ghartak.app.domain.repository.DeliveryRepository
import com.ghartak.app.util.Result
import javax.inject.Inject

class VerifyDeliveryOtpUseCase @Inject constructor(
    private val deliveryRepository: DeliveryRepository
) {
    suspend operator fun invoke(deliveryId: String, otp: String): Result<Delivery> {
        if (otp.length != 4 && otp.length != 6) {
            return Result.Error("Enter the OTP shared with the receiver")
        }
        return deliveryRepository.verifyDeliveryOtp(deliveryId, otp)
    }
}
