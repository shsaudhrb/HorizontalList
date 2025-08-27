package com.ntg.lmd.utils

import android.app.Activity
import android.content.Context
import android.os.LocaleList
import java.util.Locale

object LocaleHelper {
    fun applyLanguage(
        ctx: Context,
        langCode: String,
        recreateActivity: Boolean = true,
    ) {
        val locale = Locale(langCode)
        Locale.setDefault(locale)

        val res = ctx.resources
        val config = res.configuration
        config.setLocales(LocaleList(locale))
        res.updateConfiguration(config, res.displayMetrics)

        if (recreateActivity && ctx is Activity) ctx.recreate()
    }
}
