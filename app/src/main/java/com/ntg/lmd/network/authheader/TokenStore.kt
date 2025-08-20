package com.ntg.lmd.network.authheader

import android.content.Context
import androidx.core.content.edit

class TokenStore(
    context: Context,
) {
    private val sp =
        context.applicationContext
            .getSharedPreferences("auth", Context.MODE_PRIVATE)

    fun getAccessToken(): String? = sp.getString("access", null)

    fun getRefreshToken(): String? = sp.getString("refresh", null)

    fun saveTokens(
        access: String?,
        refresh: String?,
    ) = sp.edit {
        putString("access", access)
        putString("refresh", refresh)
    }
}
