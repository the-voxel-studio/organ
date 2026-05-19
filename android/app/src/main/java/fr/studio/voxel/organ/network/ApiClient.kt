package fr.studio.voxel.organ.network

import android.content.Context
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {
    private const val BASE_URL = "http://10.0.2.2:8001" // Android Emulator's localhost

    private lateinit var retrofit: Retrofit
    private lateinit var tokenStorage: TokenStorage

    fun init(context: Context) {
        tokenStorage = TokenStorage(context)

        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val cookieJar = PersistentCookieJar(tokenStorage)
        val authenticator = TokenAuthenticator(tokenStorage, BASE_URL)

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(logging)
            .cookieJar(cookieJar)
            .authenticator(authenticator)
            .build()

        retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    fun <T> createService(serviceClass: Class<T>): T {
        if (!::retrofit.isInitialized) {
            throw IllegalStateException("ApiClient must be initialized with init(context) before use")
        }
        return retrofit.create(serviceClass)
    }

    fun getTokenStorage(): TokenStorage {
        if (!::tokenStorage.isInitialized) {
            throw IllegalStateException("ApiClient must be initialized with init(context) before use")
        }
        return tokenStorage
    }
}
