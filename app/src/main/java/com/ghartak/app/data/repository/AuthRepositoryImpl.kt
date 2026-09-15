package com.ghartak.app.data.repository

import com.ghartak.app.data.local.SessionManager
import com.ghartak.app.data.model.toDomain
import com.ghartak.app.data.remote.ApiService
import com.ghartak.app.data.remote.dto.LoginRequestDto
import com.ghartak.app.data.remote.dto.RegisterRequestDto
import com.ghartak.app.domain.model.User
import com.ghartak.app.domain.model.UserRole
import com.ghartak.app.domain.repository.AuthRepository
import com.ghartak.app.util.Result
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    private val sessionManager: SessionManager
) : AuthRepository {

    override suspend fun login(phoneOrEmail: String, password: String): Result<User> {
        return try {
            val response = apiService.login(LoginRequestDto(phoneOrEmail, password))
            val body = response.data
            if (response.success && body != null) {
                val user = body.user.toDomain()
                sessionManager.saveSession(body.token, user)
                Result.Success(user)
            } else {
                Result.Error(response.message ?: "Login failed", response.errorCode)
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Unable to reach the server")
        }
    }

    override suspend fun register(
        name: String,
        phone: String,
        email: String?,
        password: String,
        role: UserRole
    ): Result<User> {
        return try {
            val response = apiService.register(
                RegisterRequestDto(name, phone, email, password, role.name.lowercase())
            )
            val body = response.data
            if (response.success && body != null) {
                val user = body.user.toDomain()
                sessionManager.saveSession(body.token, user)
                Result.Success(user)
            } else {
                Result.Error(response.message ?: "Registration failed", response.errorCode)
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Unable to reach the server")
        }
    }

    override suspend fun logout() {
        runCatching { apiService.logout() }
        sessionManager.clearSession()
    }

    override fun currentUser(): Flow<User?> = sessionManager.currentUser()

    override fun isLoggedIn(): Flow<Boolean> = sessionManager.isLoggedIn()
}
