package com.ghartak.app.presentation.home

import com.ghartak.app.domain.model.Delivery

data class HomeUiState(
    val userName: String = "",
    val isLoading: Boolean = true,
    val activeDelivery: Delivery? = null,
    val recentDeliveries: List<Delivery> = emptyList(),
    val errorMessage: String? = null
)
