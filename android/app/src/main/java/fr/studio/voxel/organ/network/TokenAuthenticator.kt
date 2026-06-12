package fr.studio.voxel.organ.network

import okhttp3.*
import okhttp3.logging.HttpLoggingInterceptor

class TokenAuthenticator(
    private val tokenStorage: TokenStorage,
    private val baseUrl: String
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        // We only care about 401 Unauthorized
        if (response.code != 401) return null

        // Avoid infinite loop if refresh itself fails
        if (response.request.url.toString().contains("/api/auth/refresh")) {
            tokenStorage.clear()
            // Here trigger a logout/navigation to Login screen
            return null
        }

        synchronized(this) {
            val currentBearer = tokenStorage.getBearerToken()
            val requestCookieHeader = response.request.header("Cookie")
            val requestBearer = requestCookieHeader?.let { header ->
                header.split(";")
                    .map { it.trim() }
                    .firstOrNull { it.startsWith("BEARER=") }
                    ?.substringAfter("BEARER=")
            }

            // If the token in tokenStorage is already different from the one we sent,
            // another concurrent request has already completed the refresh successfully.
            if (requestBearer != currentBearer && currentBearer != null) {
                return response.request.newBuilder()
                    .removeHeader("Cookie") // force OkHttp to load new cookies
                    .build()
            }

            val refreshToken = tokenStorage.getRefreshToken() ?: return null

            // Synchronously call the refresh endpoint
            val refreshRequest = Request.Builder()
                .url("${baseUrl}/api/auth/refresh")
                .post(RequestBody.create(null, ""))
                .header("Cookie", "refresh_token=$refreshToken")
                .build()

            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }

            val client = OkHttpClient.Builder()
                .addInterceptor(logging)
                .build()

            return try {
                val refreshResponse = client.newCall(refreshRequest).execute()
                if (refreshResponse.isSuccessful) {
                    // CookieJar on the MAIN client will handle saving the new cookies 
                    // IF we used the main client, but we don't.
                    // So we must manually extract and save if we use a clean client.
                    
                    val cookies = Cookie.parseAll(refreshRequest.url, refreshResponse.headers)
                    cookies.forEach { cookie ->
                        if (cookie.name == "BEARER") {
                            tokenStorage.saveBearerToken(cookie.value)
                        } else if (cookie.name == "refresh_token") {
                            tokenStorage.saveRefreshToken(cookie.value)
                        }
                    }

                    // Retry original request
                    response.request.newBuilder()
                        .removeHeader("Cookie") // force OkHttp to load new cookies
                        .build()
                } else {
                    tokenStorage.clear()
                    // Trigger a logout/navigation to Login screen
                    null
                }
            } catch (e: Exception) {
                tokenStorage.clear()
                // Trigger a logout/navigation to Login screen
                null
            }
        }
    }
}
