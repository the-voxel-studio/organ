package fr.studio.voxel.organ.network.services

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApiService {
    @POST("/api/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<Any>

    @POST("/api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<Unit>

    @POST("/api/auth/login/google")
    suspend fun googleLogin(@Body request: GoogleLoginRequest): Response<Unit>

    @POST("/api/auth/refresh")
    suspend fun refresh(): Response<Unit>

    @POST("/api/auth/logout")
    suspend fun logout(): Response<Unit>
}

data class LoginRequest(
    val username: String,
    val password: String
)

data class RegisterRequest(
    val email: String,
    val firstName: String,
    val lastName: String,
    val password: String
)

data class GoogleLoginRequest(
    val idToken: String? = null,
    val token: String? = null
)
