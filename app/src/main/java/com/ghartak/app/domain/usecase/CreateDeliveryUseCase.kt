package com.ghartak.app.domain.usecase

import com.ghartak.app.domain.model.Address
import com.ghartak.app.domain.model.Delivery
import com.ghartak.app.domain.model.ParcelType
import com.ghartak.app.domain.repository.DeliveryRepository
import com.ghartak.app.util.Result
import javax.inject.Inject

class CreateDeliveryUseCase @Inject constructor(
    private val deliveryRepository: DeliveryRepository
) {
    suspend operator fun invoke(
        pickup: Address,
        drop: Address,
        receiverName: String,
        receiverPhone: String,
        parcelType: ParcelType,
        parcelWeightKg: Double?,
        parcelDescription: String?,
        parcelImageUrl: String?
    ): Result<Delivery> {
        if (receiverName.isBlank()) return Result.Error("Receiver name is required")
        if (receiverPhone.length < 8) return Result.Error("Enter a valid receiver phone number")

        return deliveryRepository.createDelivery(
            pickup = pickup,
            drop = drop,
            receiverName = receiverName.trim(),
            receiverPhone = receiverPhone.trim(),
            parcelType = parcelType,
            parcelWeightKg = parcelWeightKg,
            parcelDescription = parcelDescription,
            parcelImageUrl = parcelImageUrl
        )
    }
}
