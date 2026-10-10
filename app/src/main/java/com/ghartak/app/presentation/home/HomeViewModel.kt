package com.ghartak.app.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ghartak.app.domain.repository.AuthRepository
import com.ghartak.app.domain.repository.DeliveryRepository
import com.ghartak.app.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val deliveryRepository: DeliveryRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            authRepository.currentUser().collect { user ->
                _uiState.update { it.copy(userName = user?.name.orEmpty()) }
            }
        }
        loadHome()
    }

    fun loadHome() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            when (val result = deliveryRepository.getDeliveryHistory(pageSize = 10)) {
                is Result.Success -> _uiState.update {
                    it.copy(
                        isLoading = false,
                        activeDelivery = result.data.firstOrNull { d -> !d.status.isTerminal },
                        recentDeliveries = result.data.filter { d -> d.status.isTerminal }
                    )
                }
                is Result.Error -> _uiState.update {
                    it.copy(isLoading = false, errorMessage = result.message)
                }
            }
        }
    }
}
