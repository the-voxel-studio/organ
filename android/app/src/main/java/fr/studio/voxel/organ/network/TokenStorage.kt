package fr.studio.voxel.organ.network

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class TokenStorage(context: Context) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences = EncryptedSharedPreferences.create(
        context,
        "secure_tokens",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveBearerToken(token: String?) {
        sharedPreferences.edit().putString(KEY_BEARER, token).apply()
    }

    fun getBearerToken(): String? {
        return sharedPreferences.getString(KEY_BEARER, null)
    }

    fun saveRefreshToken(token: String?) {
        sharedPreferences.edit().putString(KEY_REFRESH, token).apply()
    }

    fun getRefreshToken(): String? {
        return sharedPreferences.getString(KEY_REFRESH, null)
    }

    fun clear() {
        sharedPreferences.edit().clear().apply()
    }

    companion object {
        private const val KEY_BEARER = "bearer_token"
        private const val KEY_REFRESH = "refresh_token"
    }
}
