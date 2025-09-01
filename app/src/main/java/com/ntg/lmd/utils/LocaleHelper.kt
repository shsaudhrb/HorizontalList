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
        val locale = Locale.Builder().setLanguage(langCode).build()

        val config =Configuration(ctx.resources.configuration)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocales(LocaleList(locale))
        } else {
            @Suppress("DEPRECATION")
            config.locale = locale
        }

        val localized = ctx.createConfigurationContext(config)

        if (recreateActivity && ctx is Activity) ctx.recreate()

        return localized
    }
}

