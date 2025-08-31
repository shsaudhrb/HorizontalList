package com.ntg.lmd.utils

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import java.util.Locale

object LocaleHelper {
    fun applyLanguage(
        ctx: Context,
        langCode: String,
        recreateActivity: Boolean = true,
    ): Context {
        val locale = java.util.Locale(langCode)
        java.util.Locale.setDefault(locale)

        val config = android.content.res.Configuration(ctx.resources.configuration)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            config.setLocales(android.os.LocaleList(locale))
        } else {
            @Suppress("DEPRECATION")
            config.locale = locale
        }

        // return the wrapped context
        val localized = ctx.createConfigurationContext(config)

        if (recreateActivity && ctx is android.app.Activity) ctx.recreate()

        return localized
    }
}

