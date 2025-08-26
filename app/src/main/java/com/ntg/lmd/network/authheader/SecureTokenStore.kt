package com.ntg.lmd.network.authheader

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class SecureTokenStore(
    ctx: Context,
) {
    private val masterKey =
        MasterKey
            .Builder(ctx)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

    private val sp =
        EncryptedSharedPreferences.create(
            ctx,
            "secure_auth_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )

    fun getAccessToken(): String? = sp.getString("access", null)

    fun getRefreshToken(): String? = sp.getString("refresh", null)

    fun getAccessExpiryIso(): String? = sp.getString("access_exp", null)

    fun getRefreshExpiryIso(): String? = sp.getString("refresh_exp", null)

    fun saveFromPayload(
        access: String?,
        refresh: String?,
        expiresAt: String?,
        refreshExpiresAt: String?,
    ) {
        val newRefresh = refresh ?: getRefreshToken()
        sp
            .edit()
            .putString("access", access)
            .putString("refresh", newRefresh)
            .putString("access_exp", expiresAt)
            .putString("refresh_exp", refreshExpiresAt ?: getRefreshExpiryIso())
            .apply()
        onTokensChanged?.invoke(access, newRefresh)
    }

    fun saveTokens(
        access: String?,
        refresh: String?,
    ) {
        sp
            .edit()
            .putString("access", access)
            .putString("refresh", refresh)
            .apply()
        onTokensChanged?.invoke(access, refresh)
    }

    fun clear() {
        sp.edit().clear().apply()
        onTokensChanged?.invoke(null, null)
    }

    @Volatile
    var onTokensChanged: ((String?, String?) -> Unit)? = null
}
