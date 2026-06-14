package fr.studio.voxel.organ.network

import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody

object MockInterceptor : Interceptor {
    private val mocks = mutableMapOf<String, Pair<Int, String>>() // Map of URL paths to Pair(HttpCode, JSONResponse)
    var isMockEnabled = false

    fun addMock(path: String, jsonResponse: String, code: Int = 200) {
        mocks[path] = Pair(code, jsonResponse)
    }

    fun clearMocks() {
        mocks.clear()
        isMockEnabled = false
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (isMockEnabled) {
            val path = request.url.encodedPath
            val mockData = mocks[path]
            if (mockData != null) {
                val (code, jsonResponse) = mockData
                return Response.Builder()
                    .request(request)
                    .protocol(Protocol.HTTP_1_1)
                    .code(code)
                    .message(if (code in 200..299) "OK" else "Error")
                    .body(jsonResponse.toResponseBody("application/json".toMediaTypeOrNull()))
                    .addHeader("content-type", "application/json")
                    .build()
            } else {
                // Return a safe empty mock by default for common endpoints to prevent test crashes
                val defaultJson = if (path.endsWith("comments") || 
                    path.endsWith("attachments") || 
                    path.endsWith("dependencies") || 
                    path.endsWith("timeline") || 
                    path.endsWith("members") || 
                    path.endsWith("roles") || 
                    path.endsWith("connections") || 
                    path.endsWith("invitations") || 
                    path.endsWith("tags")) "[]" else "{}"
                return Response.Builder()
                    .request(request)
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body(defaultJson.toResponseBody("application/json".toMediaTypeOrNull()))
                    .addHeader("content-type", "application/json")
                    .build()
            }
        }
        return chain.proceed(request)
    }
}
