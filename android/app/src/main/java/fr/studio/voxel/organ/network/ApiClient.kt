package fr.studio.voxel.organ.network

import android.content.Context
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    private const val BASE_URL = "http://192.168.1.3:8001" // Android Emulator's localhost
    const val GOOGLE_SERVER_CLIENT_ID = "805077826497-lu17a6mrre44jl5t4p20nfo9gqf9sddn.apps.googleusercontent.com"


    private lateinit var retrofit: Retrofit
    private lateinit var tokenStorage: TokenStorage
    private lateinit var okHttpClient: OkHttpClient

    fun init(context: Context) {
        tokenStorage = TokenStorage(context)

        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val cookieJar = PersistentCookieJar(tokenStorage)
        val authenticator = TokenAuthenticator(tokenStorage, BASE_URL)

        okHttpClient = OkHttpClient.Builder()
            .addInterceptor(logging)
            .addInterceptor(MockInterceptor)
            .cookieJar(cookieJar)
            .authenticator(authenticator)
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
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

    fun getBaseUrl(): String {
        return BASE_URL
    }

    fun getOkHttpClient(): OkHttpClient {
        if (!::okHttpClient.isInitialized) {
            throw IllegalStateException("ApiClient must be initialized with init(context) before use")
        }
        return okHttpClient
    }
}
