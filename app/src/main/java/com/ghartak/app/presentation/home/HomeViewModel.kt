package com.ghartak.app.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ghartak.app.domain.repository.DeliveryRepository
import com.ghartak.app.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val deliveryRepository: DeliveryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadHome()
    }

    fun loadHome() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            when (val result = deliveryRepository.getDeliveryHistory(page = 1, pageSize = 10)) {
                is Result.Success -> {
                    val active = result.data.firstOrNull { !it.status.isTerminal }
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        activeDelivery = active,
                        recentDeliveries = result.data.filter { it.status.isTerminal }
                    )
                }
                is Result.Error -> _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = result.message
                )
            }
        }
    }
}
