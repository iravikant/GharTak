package com.ghartak.app.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class LoginRequestDto(val identifier: String, val password: String)

@Serializable
data class RegisterRequestDto(
    val name: String,
    val phone: String,
    val email: String?,
    val password: String,
    val role: String
)

@Serializable
data class AuthResponseDto(
    val token: String,
    val user: UserDto
)

@Serializable
data class UserDto(
    val id: String,
    val name: String,
    val phone: String,
    val email: String? = null,
    val role: String,
    val profilePhotoUrl: String? = null,
    val rating: Double? = null
)
