package com.ghartak.app.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ghartak.app.domain.model.UserRole
import com.ghartak.app.domain.repository.AuthRepository
import com.ghartak.app.util.Result
import com.ghartak.app.util.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val uiState: StateFlow<UiState<Unit>> = _uiState.asStateFlow()

    fun register(name: String, phone: String, email: String?, password: String) {
        if (name.isBlank() || phone.isBlank() || password.length < 6) {
            _uiState.value = UiState.Error("Enter a valid name, phone, and a 6+ character password")
            return
        }
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            _uiState.value = when (val result = authRepository.register(name, phone, email, password, UserRole.CUSTOMER)) {
                is Result.Success -> UiState.Success(Unit)
                is Result.Error -> UiState.Error(result.message)
            }
        }
    }
}
