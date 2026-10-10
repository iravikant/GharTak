package com.ghartak.app.presentation.splash

import androidx.lifecycle.ViewModel
import com.ghartak.app.data.local.SessionManager
import com.ghartak.app.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import javax.inject.Inject

enum class StartDestination { HOME, ONBOARDING, AUTH }

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    suspend fun resolveDestination(): StartDestination = when {
        authRepository.isLoggedIn().first() -> StartDestination.HOME
        sessionManager.hasSeenOnboarding().first() -> StartDestination.AUTH
        else -> StartDestination.ONBOARDING
    }
}
