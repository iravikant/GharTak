package com.ghartak.app.domain.usecase

import com.ghartak.app.domain.model.Delivery
import com.ghartak.app.domain.model.RiderLocationUpdate
import com.ghartak.app.domain.repository.DeliveryRepository
import com.ghartak.app.util.Result
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveDeliveryTrackingUseCase @Inject constructor(
    private val deliveryRepository: DeliveryRepository
) {
    operator fun invoke(deliveryId: String): Flow<Result<Pair<Delivery, RiderLocationUpdate?>>> =
        deliveryRepository.observeTracking(deliveryId)
}
