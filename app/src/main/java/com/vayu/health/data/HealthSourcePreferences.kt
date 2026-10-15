package com.vayu.health.data

import android.content.Context

class HealthSourcePreferences(context: Context?) {
    companion object {
        const val PREFS_NAME = "vayu_source_prefs"
        const val KEY_SELECTED_SOURCE = "selected_source"
        const val KEY_FIRST_RUN_COMPLETED = "first_run_completed"
        const val ALL_SOURCES_TOKEN = "__ALL_SOURCES__"
        const val DEFAULT_FITBIT_PACKAGE = "com.fitbit.FitbitMobile"
    }

    private val prefs = context?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getSelectedSource(): String? {
        val saved = prefs?.getString(KEY_SELECTED_SOURCE, null) ?: return DEFAULT_FITBIT_PACKAGE
        return if (saved == ALL_SOURCES_TOKEN) null else saved
    }

    fun setSelectedSource(source: String?) {
        prefs?.edit()?.putString(KEY_SELECTED_SOURCE, source ?: ALL_SOURCES_TOKEN)?.apply()
    }

    fun isFirstRun(): Boolean {
        return !(prefs?.getBoolean(KEY_FIRST_RUN_COMPLETED, false) ?: false)
    }

    fun setFirstRunCompleted() {
        prefs?.edit()?.putBoolean(KEY_FIRST_RUN_COMPLETED, true)?.apply()
    }
}
