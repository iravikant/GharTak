package com.ghartak.app.domain.usecase

import com.ghartak.app.domain.model.User
import com.ghartak.app.domain.repository.AuthRepository
import com.ghartak.app.util.Result
import javax.inject.Inject

class LoginUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(phoneOrEmail: String, password: String): Result<User> {
        if (phoneOrEmail.isBlank() || password.isBlank()) {
            return Result.Error("Phone/email and password are required")
        }
        return authRepository.login(phoneOrEmail.trim(), password)
    }
}
