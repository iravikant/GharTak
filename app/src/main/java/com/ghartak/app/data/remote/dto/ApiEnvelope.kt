package com.ghartak.app.data.remote.dto

import kotlinx.serialization.Serializable

/** Matches the backend's consistent JSON envelope (spec section 16). */
@Serializable
data class ApiEnvelope<T>(
    val success: Boolean,
    val message: String? = null,
    val data: T? = null,
    val errorCode: String? = null
)
