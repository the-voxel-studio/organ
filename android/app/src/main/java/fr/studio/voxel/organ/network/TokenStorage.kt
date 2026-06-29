package fr.studio.voxel.organ.network

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class TokenStorage(context: Context) {

    private val sharedPreferences: SharedPreferences = try {
        createEncryptedPrefs(context)
    } catch (e: Exception) {
        Log.e("TokenStorage", "Failed to initialize EncryptedSharedPreferences, trying to clear corrupted keys...", e)
        try {
            // Delete the corrupted shared preferences file
            context.deleteSharedPreferences("secure_tokens")
            createEncryptedPrefs(context)
        } catch (ex: Exception) {
            Log.e("TokenStorage", "Critical keystore error. Falling back to standard SharedPreferences.", ex)
            context.getSharedPreferences("secure_tokens_fallback", Context.MODE_PRIVATE)
        }
    }

    private fun createEncryptedPrefs(context: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        return EncryptedSharedPreferences.create(
            context,
            "secure_tokens",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

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
