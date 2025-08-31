package com.ntg.lmd.settings.data

import android.content.Context
import android.content.Context.MODE_PRIVATE

class SettingsPreferenceDataSource(
    context: Context,
) {
    private val sp = context.getSharedPreferences("settings_prefs", MODE_PRIVATE)

    private companion object {
        const val KEY_LANG = "lang"
        const val KEY_NOTIF_WINDOW = "nw"
        const val DEFAULT_LANG = "en"
        const val DEFAULT_NOTIF_WINDOW_DAYS = 15
    }

    fun getLanguage(): String =
        sp.getString(KEY_LANG, DEFAULT_LANG) ?: DEFAULT_LANG

    fun setLanguage(code: String) {
        sp.edit().putString(KEY_LANG, code).apply()
    }

    fun getNotificationWindowDays(): Int =
        sp.getInt(KEY_NOTIF_WINDOW, DEFAULT_NOTIF_WINDOW_DAYS)

    fun setNotificationWindowDays(days: Int) {
        sp.edit().putInt(KEY_NOTIF_WINDOW, days).apply()
    }
}
