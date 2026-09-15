package com.ghartak.app.domain.repository

import com.ghartak.app.domain.model.User
import com.ghartak.app.domain.model.UserRole
import com.ghartak.app.util.Result
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    suspend fun login(phoneOrEmail: String, password: String): Result<User>
    suspend fun register(name: String, phone: String, email: String?, password: String, role: UserRole): Result<User>
    suspend fun logout()
    fun currentUser(): Flow<User?>
    fun isLoggedIn(): Flow<Boolean>
}
