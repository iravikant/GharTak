package com.ghartak.app.domain.repository

import android.app.Activity
import com.ghartak.app.domain.model.OtpPurpose
import com.ghartak.app.domain.model.OtpRequestState
import com.ghartak.app.domain.model.User
import com.ghartak.app.util.Result
import kotlinx.coroutines.flow.Flow

interface AuthRepository {

    // ---- Email + password ----
    suspend fun signInWithEmail(email: String, password: String): Result<User>

    /** Creates the account and its Firestore profile (name, mobile, email). */
    suspend fun signUpWithEmail(name: String, mobile: String, email: String, password: String): Result<User>

    suspend fun sendPasswordReset(email: String): Result<Unit>

    // ---- Google ----
    suspend fun signInWithGoogle(activity: Activity): Result<User>

    // ---- Mobile OTP ----
    /**
     * Starts Firebase Phone Auth for [e164Phone]. The flow stays open after CodeSent so that
     * Android's automatic SMS retrieval (AutoVerified) can still arrive; cancel the collector
     * when leaving the OTP step. [forceResend] reuses the last resend token.
     */
    fun requestOtp(
        e164Phone: String,
        activity: Activity,
        purpose: OtpPurpose,
        forceResend: Boolean = false
    ): Flow<OtpRequestState>

    /** Completes a LOGIN OTP: signs in, creating a profile for a brand-new number. */
    suspend fun verifyOtpSignIn(verificationId: String, code: String): Result<User>

    /** Completes a LINK OTP: attaches the verified phone to the signed-in account. */
    suspend fun verifyOtpAndLink(verificationId: String, code: String): Result<User>

    // ---- Session ----
    suspend fun logout()
    fun currentUser(): Flow<User?>
    fun isLoggedIn(): Flow<Boolean>
}
