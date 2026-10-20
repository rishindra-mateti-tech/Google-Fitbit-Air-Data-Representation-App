package com.vayu.health.data

import android.content.Context

class HealthSourcePreferences(context: Context?) {
    companion object {
        const val PREFS_NAME = "vayu_source_prefs"
        const val KEY_SELECTED_SOURCE = "selected_source"
        const val KEY_SOURCE_MODE_VERSION = "source_mode_version"
        const val KEY_FIRST_RUN_COMPLETED = "first_run_completed"
        const val ALL_SOURCES_TOKEN = "__ALL_SOURCES__"
        const val DEFAULT_FITBIT_PACKAGE = "com.fitbit.FitbitMobile"
        const val ALL_SOURCES_DEFAULT_VERSION = 2

        const val KEY_GOOGLE_HEALTH_ENABLED = "google_health_enabled"
        const val KEY_GOOGLE_HEALTH_CLIENT_ID = "google_health_client_id"
        private const val KEY_GOOGLE_HEALTH_CLIENT_SECRET = "google_health_client_secret"
        const val KEY_GOOGLE_HEALTH_REFRESH_TOKEN = "google_health_refresh_token"
        const val KEY_GOOGLE_HEALTH_ACCESS_TOKEN = "google_health_access_token"
        const val KEY_GOOGLE_HEALTH_TOKEN_EXPIRY = "google_health_token_expiry"
        const val KEY_SLEEP_TARGET_MINUTES = "sleep_target_minutes"

        const val DEFAULT_CLIENT_ID = ""
        const val DEFAULT_REFRESH_TOKEN = ""
        const val DEFAULT_SLEEP_TARGET_MINS = 480
    }

    private val prefs = context?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    init {
        // Security rule: Never store OAuth client secret in SharedPreferences
        if (prefs?.contains(KEY_GOOGLE_HEALTH_CLIENT_SECRET) == true) {
            prefs.edit().remove(KEY_GOOGLE_HEALTH_CLIENT_SECRET).apply()
        }
    }

    fun getSelectedSource(): String? {
        // V2 is an honest Health Connect mirror: show records from every connected
        // source by default and keep the source visible on every reading.
        if ((prefs?.getInt(KEY_SOURCE_MODE_VERSION, 0) ?: 0) < ALL_SOURCES_DEFAULT_VERSION) {
            prefs?.edit()
                ?.putString(KEY_SELECTED_SOURCE, ALL_SOURCES_TOKEN)
                ?.putInt(KEY_SOURCE_MODE_VERSION, ALL_SOURCES_DEFAULT_VERSION)
                ?.apply()
            return null
        }
        val saved = prefs?.getString(KEY_SELECTED_SOURCE, null) ?: return null
        return if (saved == ALL_SOURCES_TOKEN) null else saved
    }

    fun setSelectedSource(source: String?) {
        prefs?.edit()
            ?.putString(KEY_SELECTED_SOURCE, source ?: ALL_SOURCES_TOKEN)
            ?.putInt(KEY_SOURCE_MODE_VERSION, ALL_SOURCES_DEFAULT_VERSION)
            ?.apply()
    }

    fun isFirstRun(): Boolean {
        return !(prefs?.getBoolean(KEY_FIRST_RUN_COMPLETED, false) ?: false)
    }

    fun setFirstRunCompleted() {
        prefs?.edit()?.putBoolean(KEY_FIRST_RUN_COMPLETED, true)?.apply()
    }

    // Google Health API & Cloud Sync Settings (Disabled by default per privacy/security policy)
    fun isGoogleHealthEnabled(): Boolean {
        return prefs?.getBoolean(KEY_GOOGLE_HEALTH_ENABLED, false) ?: false
    }

    fun setGoogleHealthEnabled(enabled: Boolean) {
        prefs?.edit()?.putBoolean(KEY_GOOGLE_HEALTH_ENABLED, enabled)?.apply()
    }

    fun getGoogleHealthClientId(): String {
        return prefs?.getString(KEY_GOOGLE_HEALTH_CLIENT_ID, null)?.takeIf { it.isNotBlank() } ?: DEFAULT_CLIENT_ID
    }

    fun setGoogleHealthClientId(clientId: String) {
        prefs?.edit()?.putString(KEY_GOOGLE_HEALTH_CLIENT_ID, clientId)?.apply()
    }

    fun getGoogleHealthRefreshToken(): String? {
        val saved = prefs?.getString(KEY_GOOGLE_HEALTH_REFRESH_TOKEN, null)?.takeIf { it.isNotBlank() }
        return saved ?: DEFAULT_REFRESH_TOKEN
    }

    fun setGoogleHealthRefreshToken(refreshToken: String?) {
        prefs?.edit()?.putString(KEY_GOOGLE_HEALTH_REFRESH_TOKEN, refreshToken)?.apply()
    }

    fun getGoogleHealthAccessToken(): String? {
        val expiry = prefs?.getLong(KEY_GOOGLE_HEALTH_TOKEN_EXPIRY, 0L) ?: 0L
        if (System.currentTimeMillis() >= expiry) {
            return null // Token expired
        }
        return prefs?.getString(KEY_GOOGLE_HEALTH_ACCESS_TOKEN, null)?.takeIf { it.isNotBlank() }
    }

    fun disconnectGoogleHealth() {
        prefs?.edit()?.apply {
            putBoolean(KEY_GOOGLE_HEALTH_ENABLED, false)
            remove(KEY_GOOGLE_HEALTH_CLIENT_ID)
            remove(KEY_GOOGLE_HEALTH_REFRESH_TOKEN)
            remove(KEY_GOOGLE_HEALTH_ACCESS_TOKEN)
            remove(KEY_GOOGLE_HEALTH_TOKEN_EXPIRY)
            apply()
        }
    }

    fun saveGoogleHealthTokens(accessToken: String, refreshToken: String?, expiresInSeconds: Long) {
        val expiryTimestamp = System.currentTimeMillis() + (expiresInSeconds * 1000L) - 60000L // 1 minute safety buffer
        prefs?.edit()?.apply {
            putString(KEY_GOOGLE_HEALTH_ACCESS_TOKEN, accessToken)
            if (!refreshToken.isNullOrBlank()) {
                putString(KEY_GOOGLE_HEALTH_REFRESH_TOKEN, refreshToken)
            }
            putLong(KEY_GOOGLE_HEALTH_TOKEN_EXPIRY, expiryTimestamp)
            apply()
        }
    }

    fun getSleepTargetMinutes(): Int {
        return prefs?.getInt(KEY_SLEEP_TARGET_MINUTES, DEFAULT_SLEEP_TARGET_MINS) ?: DEFAULT_SLEEP_TARGET_MINS
    }

    fun setSleepTargetMinutes(minutes: Int) {
        prefs?.edit()?.putInt(KEY_SLEEP_TARGET_MINUTES, minutes)?.apply()
    }
}
