package com.divami.traveltranslator.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.divami.traveltranslator.domain.model.Account
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

/** Persisted session + first-run flags. Backed by a Preferences DataStore. */
data class Session(
    val token: String?,
    val account: Account?,
    val hasSeenWelcome: Boolean,
    val hasOnboarded: Boolean,
    /** User's preferred default (target) language code, e.g. "hi". Null until chosen. */
    val defaultLanguage: String?,
) {
    val isAuthenticated: Boolean get() = !token.isNullOrBlank()
}

/**
 * Single source of truth for the auth token and onboarding flags. The token is
 * exposed both reactively ([session]) for the UI and synchronously
 * ([blockingToken]) for the OkHttp [AuthInterceptor], which runs off the main
 * thread and cannot suspend.
 */
@Singleton
class SessionStore @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    val session: Flow<Session> = dataStore.data.map { it.toSession() }

    /** Blocking read for the OkHttp interceptor. DataStore caches in memory, so
     *  after the first read this is effectively free and never touches disk. */
    fun blockingToken(): String? = runBlocking { dataStore.data.first()[KEY_TOKEN] }

    suspend fun currentSession(): Session = dataStore.data.first().toSession()

    suspend fun saveAuth(token: String, account: Account) {
        dataStore.edit { prefs ->
            prefs[KEY_TOKEN] = token
            prefs[KEY_USER_ID] = account.id
            prefs[KEY_IS_GUEST] = account.isGuest
            account.email?.let { prefs[KEY_EMAIL] = it } ?: prefs.remove(KEY_EMAIL)
            account.displayName?.let { prefs[KEY_NAME] = it } ?: prefs.remove(KEY_NAME)
            account.photoUrl?.let { prefs[KEY_PHOTO] = it } ?: prefs.remove(KEY_PHOTO)
        }
    }

    suspend fun setHasSeenWelcome(value: Boolean) =
        dataStore.editFlag(KEY_HAS_SEEN_WELCOME, value)

    suspend fun setHasOnboarded(value: Boolean) =
        dataStore.editFlag(KEY_HAS_ONBOARDED, value)

    /** Persists the user's preferred default (target) language locally so the
     *  chat can honor the onboarding/settings choice instantly and offline. */
    suspend fun setDefaultLanguage(code: String) {
        dataStore.edit { it[KEY_DEFAULT_LANGUAGE] = code }
    }

    /** Clears the token + profile but keeps first-run flags (logout ≠ reinstall). */
    suspend fun clearAuth() {
        dataStore.edit { prefs ->
            prefs.remove(KEY_TOKEN)
            prefs.remove(KEY_USER_ID)
            prefs.remove(KEY_IS_GUEST)
            prefs.remove(KEY_EMAIL)
            prefs.remove(KEY_NAME)
            prefs.remove(KEY_PHOTO)
        }
    }

    private fun Preferences.toSession(): Session {
        val token = this[KEY_TOKEN]
        val id = this[KEY_USER_ID]
        val account = if (id != null) {
            Account(
                id = id,
                email = this[KEY_EMAIL],
                displayName = this[KEY_NAME],
                photoUrl = this[KEY_PHOTO],
                isGuest = this[KEY_IS_GUEST] ?: false,
            )
        } else {
            null
        }
        return Session(
            token = token,
            account = account,
            hasSeenWelcome = this[KEY_HAS_SEEN_WELCOME] ?: false,
            hasOnboarded = this[KEY_HAS_ONBOARDED] ?: false,
            defaultLanguage = this[KEY_DEFAULT_LANGUAGE],
        )
    }

    private suspend fun DataStore<Preferences>.editFlag(key: Preferences.Key<Boolean>, value: Boolean) {
        edit { it[key] = value }
    }

    private companion object {
        val KEY_TOKEN = stringPreferencesKey("auth_token")
        val KEY_USER_ID = stringPreferencesKey("user_id")
        val KEY_EMAIL = stringPreferencesKey("user_email")
        val KEY_NAME = stringPreferencesKey("user_name")
        val KEY_PHOTO = stringPreferencesKey("user_photo")
        val KEY_IS_GUEST = booleanPreferencesKey("is_guest")
        val KEY_HAS_SEEN_WELCOME = booleanPreferencesKey("has_seen_welcome")
        val KEY_HAS_ONBOARDED = booleanPreferencesKey("has_onboarded")
        val KEY_DEFAULT_LANGUAGE = stringPreferencesKey("default_language")
    }
}
