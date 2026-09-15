package com.ghartak.app.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ghartak.app.domain.usecase.LoginUseCase
import com.ghartak.app.util.Result
import com.ghartak.app.util.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val uiState: StateFlow<UiState<Unit>> = _uiState.asStateFlow()

    fun login(identifier: String, password: String) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            _uiState.value = when (val result = loginUseCase(identifier, password)) {
                is Result.Success -> UiState.Success(Unit)
                is Result.Error -> UiState.Error(result.message)
            }
        }
    }
}
