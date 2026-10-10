package com.ghartak.app.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.ghartak.app.domain.model.User
import com.ghartak.app.domain.model.UserRole
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "session_prefs")

/**
 * Local cache of the signed-in user's profile, kept in sync whenever AuthRepository reads or
 * writes the Firestore "users/{uid}" doc. Firebase Auth itself remains the source of truth for
 * *whether* someone is signed in and owns all token/session handling; this cache exists purely
 * so the app can render a name/role instantly on launch without waiting on a Firestore read.
 */
@Singleton
class SessionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val USER_ID = stringPreferencesKey("user_id")
        val USER_NAME = stringPreferencesKey("user_name")
        val USER_PHONE = stringPreferencesKey("user_phone")
        val USER_EMAIL = stringPreferencesKey("user_email")
        val USER_ROLE = stringPreferencesKey("user_role")
        val USER_PHOTO_URL = stringPreferencesKey("user_photo_url")
        val USER_RATING = doublePreferencesKey("user_rating")
        val ONBOARDING_SEEN = booleanPreferencesKey("onboarding_seen")
    }

    suspend fun cacheUser(user: User) {
        context.dataStore.edit { prefs ->
            prefs[Keys.USER_ID] = user.id
            prefs[Keys.USER_NAME] = user.name
            prefs[Keys.USER_PHONE] = user.phone
            prefs[Keys.USER_EMAIL] = user.email.orEmpty()
            prefs[Keys.USER_ROLE] = user.role.name
            prefs[Keys.USER_PHOTO_URL] = user.profilePhotoUrl.orEmpty()
            prefs[Keys.USER_RATING] = user.rating ?: 0.0
        }
    }

    /** Clears the cached profile on logout but keeps device-level flags such as onboarding. */
    suspend fun clearUser() {
        context.dataStore.edit { prefs ->
            prefs.remove(Keys.USER_ID)
            prefs.remove(Keys.USER_NAME)
            prefs.remove(Keys.USER_PHONE)
            prefs.remove(Keys.USER_EMAIL)
            prefs.remove(Keys.USER_ROLE)
            prefs.remove(Keys.USER_PHOTO_URL)
            prefs.remove(Keys.USER_RATING)
        }
    }

    fun hasSeenOnboarding(): Flow<Boolean> =
        context.dataStore.data.map { it[Keys.ONBOARDING_SEEN] ?: false }

    suspend fun setOnboardingSeen() {
        context.dataStore.edit { it[Keys.ONBOARDING_SEEN] = true }
    }

    fun currentUser(): Flow<User?> = context.dataStore.data.map { prefs ->
        val id = prefs[Keys.USER_ID] ?: return@map null
        User(
            id = id,
            name = prefs[Keys.USER_NAME].orEmpty(),
            phone = prefs[Keys.USER_PHONE].orEmpty(),
            email = prefs[Keys.USER_EMAIL]?.ifBlank { null },
            role = prefs[Keys.USER_ROLE]?.let { runCatching { UserRole.valueOf(it) }.getOrNull() } ?: UserRole.CUSTOMER,
            profilePhotoUrl = prefs[Keys.USER_PHOTO_URL]?.ifBlank { null },
            rating = prefs[Keys.USER_RATING]?.takeIf { it > 0.0 }
        )
    }
}
