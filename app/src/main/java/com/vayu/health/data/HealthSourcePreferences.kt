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
    }

    private val prefs = context?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

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
}
