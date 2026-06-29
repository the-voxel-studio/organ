package fr.studio.voxel.organ.network

import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

class PersistentCookieJar(private val tokenStorage: TokenStorage) : CookieJar {

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        cookies.forEach { cookie ->
            if (cookie.name == "BEARER") {
                tokenStorage.saveBearerToken(cookie.value)
            } else if (cookie.name == "refresh_token") {
                tokenStorage.saveRefreshToken(cookie.value)
            }
        }
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val cookies = mutableListOf<Cookie>()
        
        tokenStorage.getBearerToken()?.let {
            cookies.add(
                Cookie.Builder()
                    .name("BEARER")
                    .value(it)
                    .domain(url.host)
                    .path("/")
                    .httpOnly()
                    .build()
            )
        }

        tokenStorage.getRefreshToken()?.let {
            cookies.add(
                Cookie.Builder()
                    .name("refresh_token")
                    .value(it)
                    .domain(url.host)
                    .path("/")
                    .httpOnly()
                    .build()
            )
        }

        return cookies
    }
}
