package com.ghartak.app.domain.usecase

import com.ghartak.app.domain.model.Address
import com.ghartak.app.domain.model.Delivery
import com.ghartak.app.domain.model.ParcelType
import com.ghartak.app.domain.model.PaymentMethod
import com.ghartak.app.domain.model.PriceQuote
import com.ghartak.app.domain.model.PricingRules
import com.ghartak.app.domain.model.WeightBand
import com.ghartak.app.domain.repository.DeliveryRepository
import com.ghartak.app.util.Result
import com.ghartak.app.util.Validators
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
        weight: WeightBand,
        description: String,
        specialInstructions: String,
        photoUri: String?,
        paymentMethod: PaymentMethod,
        quote: PriceQuote
    ): Result<Delivery> {
        Validators.nameError(receiverName)?.let { return Result.Error("Receiver: $it") }
        Validators.mobileError(receiverPhone)?.let { return Result.Error("Receiver: $it") }
        if (weight.maxKg > PricingRules.maxWeightKg(parcelType)) {
            return Result.Error("That weight is too heavy for ${parcelType.displayName}. Choose a larger parcel type.")
        }
        if (!paymentMethod.isAvailable) {
            return Result.Error("${paymentMethod.displayName} isn't available yet. Please choose cash on delivery.")
        }

        return deliveryRepository.createDelivery(
            pickup = pickup,
            drop = drop,
            receiverName = receiverName.trim(),
            receiverPhone = receiverPhone.trim(),
            parcelType = parcelType,
            parcelWeightKg = weight.maxKg,
            parcelDescription = description.trim().ifBlank { null },
            specialInstructions = specialInstructions.trim().ifBlank { null },
            parcelImageUri = photoUri,
            paymentMethod = paymentMethod,
            deliveryFee = quote.deliveryFee,
            platformFee = quote.platformFee,
            distanceKm = quote.distanceKm
        )
    }
}
