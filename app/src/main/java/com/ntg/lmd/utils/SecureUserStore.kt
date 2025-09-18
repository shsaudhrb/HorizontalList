package com.ntg.lmd.utils

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.ntg.lmd.authentication.data.datasource.model.User

class SecureUserStore(
    ctx: Context,
) {
    private val masterKey =
        MasterKey.Builder(ctx).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()

    private val sp =
        EncryptedSharedPreferences.create(
            ctx,
            "secure_user_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )

    fun getUserId(): String? = sp.getString(KEY_USER_ID, null)

    fun getUserEmail(): String? = sp.getString(KEY_USER_EMAIL, null)

    fun getUserFullName(): String? = sp.getString(KEY_USER_NAME, null)

    fun saveUser(
        id: String?,
        email: String?,
        fullName: String?,
    ) {
        sp
            .edit()
            .putString(KEY_USER_ID, id)
            .putString(KEY_USER_EMAIL, email)
            .putString(KEY_USER_NAME, fullName)
            .apply()
        onUserChanged?.invoke(id)
    }

    fun clear() {
        sp.edit().clear().apply()
        onUserChanged?.invoke(null)
    }

    fun getUser(): User? {
        val id = getUserId()
        val email = getUserEmail()
        val name = getUserFullName()
        return if (id != null && email != null && name != null) {
            User(id = id, email = email, fullName = name)
        } else {
            null
        }
    }

    @Volatile
    var onUserChanged: ((String?) -> Unit)? = null

    private companion object {
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_NAME = "user_full_name"
    }
}
