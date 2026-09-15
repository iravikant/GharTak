package com.ghartak.app.data.local

import android.content.Context
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
 * Holds the auth token + minimal user profile. Kept separate from Room (which is for cached
 * domain data, not credentials/session).
 */
@Singleton
class SessionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val TOKEN = stringPreferencesKey("auth_token")
        val USER_ID = stringPreferencesKey("user_id")
        val USER_NAME = stringPreferencesKey("user_name")
        val USER_PHONE = stringPreferencesKey("user_phone")
        val USER_EMAIL = stringPreferencesKey("user_email")
        val USER_ROLE = stringPreferencesKey("user_role")
    }

    suspend fun saveSession(token: String, user: User) {
        context.dataStore.edit { prefs ->
            prefs[Keys.TOKEN] = token
            prefs[Keys.USER_ID] = user.id
            prefs[Keys.USER_NAME] = user.name
            prefs[Keys.USER_PHONE] = user.phone
            prefs[Keys.USER_EMAIL] = user.email.orEmpty()
            prefs[Keys.USER_ROLE] = user.role.name
        }
    }

    suspend fun clearSession() {
        context.dataStore.edit { it.clear() }
    }

    fun authToken(): Flow<String?> = context.dataStore.data.map { it[Keys.TOKEN] }

    fun currentUser(): Flow<User?> = context.dataStore.data.map { prefs ->
        val id = prefs[Keys.USER_ID] ?: return@map null
        User(
            id = id,
            name = prefs[Keys.USER_NAME].orEmpty(),
            phone = prefs[Keys.USER_PHONE].orEmpty(),
            email = prefs[Keys.USER_EMAIL]?.ifBlank { null },
            role = prefs[Keys.USER_ROLE]?.let { runCatching { UserRole.valueOf(it) }.getOrNull() } ?: UserRole.CUSTOMER
        )
    }

    fun isLoggedIn(): Flow<Boolean> = authToken().map { !it.isNullOrBlank() }
}
