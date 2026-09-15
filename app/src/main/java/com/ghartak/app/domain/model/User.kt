package com.ghartak.app.domain.model

enum class UserRole { CUSTOMER, RIDER }

data class User(
    val id: String,
    val name: String,
    val phone: String,
    val email: String?,
    val role: UserRole,
    val profilePhotoUrl: String? = null,
    val rating: Double? = null
)
