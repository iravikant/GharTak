package com.ghartak.app.presentation.splash

import androidx.lifecycle.ViewModel
import com.ghartak.app.domain.repository.AuthRepository
import kotlinx.coroutines.flow.firstOrNull
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {
    suspend fun checkSession(): Boolean = authRepository.isLoggedIn().firstOrNull() ?: false
}
