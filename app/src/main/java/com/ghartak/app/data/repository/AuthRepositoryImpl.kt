package com.ghartak.app.data.repository

import android.app.Activity
import com.ghartak.app.data.auth.GoogleSignInHelper
import com.ghartak.app.data.firebase.AuthFailure
import com.ghartak.app.data.firebase.FirestoreSchema
import com.ghartak.app.data.firebase.toAuthError
import com.ghartak.app.data.local.SessionManager
import com.ghartak.app.data.model.toDomain
import com.ghartak.app.data.model.toUserMap
import com.ghartak.app.domain.model.OtpPurpose
import com.ghartak.app.domain.model.OtpRequestState
import com.ghartak.app.domain.model.User
import com.ghartak.app.domain.model.UserRole
import com.ghartak.app.domain.repository.AuthRepository
import com.ghartak.app.util.Result
import com.ghartak.app.util.Validators
import com.google.firebase.FirebaseException
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val sessionManager: SessionManager
) : AuthRepository {

    private val users get() = firestore.collection(FirestoreSchema.Collections.USERS)

    /** Kept so "Resend OTP" can ask Firebase to resend instead of starting a fresh verification. */
    @Volatile
    private var resendToken: PhoneAuthProvider.ForceResendingToken? = null

    // ------------------------------------------------------------------ email + password

    override suspend fun signInWithEmail(email: String, password: String): Result<User> =
        runAuth(AuthFailure.SIGN_IN) {
            val firebaseUser = firebaseAuth.signInWithEmailAndPassword(email, password).await().user
                ?: return@runAuth Result.Error("Couldn't log you in. Please try again.")
            Result.Success(ensureProfile(firebaseUser))
        }

    override suspend fun signUpWithEmail(
        name: String,
        mobile: String,
        email: String,
        password: String
    ): Result<User> = runAuth(AuthFailure.SIGN_UP) {
        val firebaseUser = firebaseAuth.createUserWithEmailAndPassword(email, password).await().user
            ?: return@runAuth Result.Error("Couldn't create your account. Please try again.")

        // Best-effort extras: a failure here must not undo an account that was already created.
        runCatching {
            firebaseUser.updateProfile(
                UserProfileChangeRequest.Builder().setDisplayName(name).build()
            ).await()
        }
        runCatching { firebaseUser.sendEmailVerification().await() }

        Result.Success(
            ensureProfile(firebaseUser, nameOverride = name, phoneOverride = Validators.toE164(mobile))
        )
    }

    override suspend fun sendPasswordReset(email: String): Result<Unit> {
        return try {
            firebaseAuth.sendPasswordResetEmail(email).await()
            Result.Success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: FirebaseAuthInvalidUserException) {
            // Same response whether or not the account exists, so this can't be used to probe emails.
            Result.Success(Unit)
        } catch (e: Exception) {
            e.toAuthError(AuthFailure.RESET)
        }
    }

    // ------------------------------------------------------------------ google

    override suspend fun signInWithGoogle(activity: Activity): Result<User> =
        runAuth(AuthFailure.GOOGLE) {
            val idToken = GoogleSignInHelper.getIdToken(activity)
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val firebaseUser = firebaseAuth.signInWithCredential(credential).await().user
                ?: return@runAuth Result.Error("Google sign-in failed. Please try again.")
            Result.Success(ensureProfile(firebaseUser))
        }

    // ------------------------------------------------------------------ mobile OTP

    override fun requestOtp(
        e164Phone: String,
        activity: Activity,
        purpose: OtpPurpose,
        forceResend: Boolean
    ): Flow<OtpRequestState> = callbackFlow {
        // Captured so the non-suspend SDK callback below can launch work on this flow's scope.
        val producerScope = this

        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
                resendToken = token
                trySend(OtpRequestState.CodeSent(verificationId))
            }

            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                // The SMS was read automatically (or the number is a Firebase test number).
                producerScope.launch {
                    val result = when (purpose) {
                        OtpPurpose.LOGIN -> signInWithPhoneCredential(credential)
                        OtpPurpose.LINK -> linkPhoneCredential(credential)
                    }
                    trySend(
                        when (result) {
                            is Result.Success -> OtpRequestState.AutoVerified(result.data)
                            is Result.Error -> OtpRequestState.Error(result.message)
                        }
                    )
                }
            }

            override fun onVerificationFailed(exception: FirebaseException) {
                trySend(OtpRequestState.Error(exception.toAuthError(AuthFailure.PHONE_SEND).message))
            }
        }

        val builder = PhoneAuthOptions.newBuilder(firebaseAuth)
            .setPhoneNumber(e164Phone)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)
        if (forceResend) resendToken?.let { builder.setForceResendingToken(it) }

        PhoneAuthProvider.verifyPhoneNumber(builder.build())

        // Stay open after CodeSent so automatic SMS retrieval can still arrive.
        awaitClose { }
    }

    override suspend fun verifyOtpSignIn(verificationId: String, code: String): Result<User> =
        signInWithPhoneCredential(PhoneAuthProvider.getCredential(verificationId, code))

    override suspend fun verifyOtpAndLink(verificationId: String, code: String): Result<User> =
        linkPhoneCredential(PhoneAuthProvider.getCredential(verificationId, code))

    private suspend fun signInWithPhoneCredential(credential: AuthCredential): Result<User> =
        runAuth(AuthFailure.PHONE_VERIFY) {
            val firebaseUser = firebaseAuth.signInWithCredential(credential).await().user
                ?: return@runAuth Result.Error("Couldn't verify the code. Please try again.")
            Result.Success(ensureProfile(firebaseUser))
        }

    private suspend fun linkPhoneCredential(credential: AuthCredential): Result<User> =
        runAuth(AuthFailure.PHONE_VERIFY) {
            val current = firebaseAuth.currentUser
                ?: return@runAuth Result.Error("Your session expired. Please log in again.")
            val linked = current.linkWithCredential(credential).await().user ?: current

            val profile = ensureProfile(linked)
            val verifiedPhone = linked.phoneNumber ?: profile.phone
            users.document(linked.uid)
                .set(mapOf("phone" to verifiedPhone, "phoneVerified" to true), SetOptions.merge())
                .await()

            val updated = profile.copy(phone = verifiedPhone)
            sessionManager.cacheUser(updated)
            Result.Success(updated)
        }

    // ------------------------------------------------------------------ session

    override suspend fun logout() {
        firebaseAuth.signOut()
        sessionManager.clearUser()
    }

    override fun currentUser(): Flow<User?> = sessionManager.currentUser()

    override fun isLoggedIn(): Flow<Boolean> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth -> trySend(auth.currentUser != null) }
        firebaseAuth.addAuthStateListener(listener)
        awaitClose { firebaseAuth.removeAuthStateListener(listener) }
    }

    // ------------------------------------------------------------------ helpers

    /**
     * Returns the user's Firestore profile, creating it on first sign-in from whatever the auth
     * provider knows (display name, email, phone, photo). Also refreshes the local cache.
     */
    private suspend fun ensureProfile(
        firebaseUser: FirebaseUser,
        nameOverride: String? = null,
        phoneOverride: String? = null
    ): User {
        val ref = users.document(firebaseUser.uid)
        val snapshot = ref.get().await()

        val user = if (snapshot.exists()) {
            snapshot.toDomain()
        } else {
            val newUser = User(
                id = firebaseUser.uid,
                name = nameOverride?.takeIf { it.isNotBlank() }
                    ?: firebaseUser.displayName?.takeIf { it.isNotBlank() }
                    ?: "GharTak User",
                phone = phoneOverride ?: firebaseUser.phoneNumber.orEmpty(),
                email = firebaseUser.email,
                role = UserRole.CUSTOMER,
                profilePhotoUrl = firebaseUser.photoUrl?.toString()
            )
            ref.set(newUser.toUserMap()).await()
            newUser
        }

        sessionManager.cacheUser(user)
        return user
    }

    /** Runs [block], mapping any failure to a user-facing message and letting cancellation through. */
    private suspend inline fun <T> runAuth(
        kind: AuthFailure,
        block: () -> Result<T>
    ): Result<T> {
        return try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            e.toAuthError(kind)
        }
    }
}
