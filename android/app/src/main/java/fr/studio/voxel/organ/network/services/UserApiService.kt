package fr.studio.voxel.organ.network.services

import retrofit2.Response
import retrofit2.http.*

interface UserApiService {
    @GET("/api/users/me")
    suspend fun getCurrentUser(): Response<User>

    @GET("/api/users/{id}")
    suspend fun getUser(@Path("id") id: Int): Response<User>
}
data class User(
    val id : Int,
    val uuid : String,
    val firstName : String,
    val lastName : String,
    val email : String,
    val password : String,
    val googleId : Int,
    val isVerified : Boolean,
    val jwtVersion : Int,
    val createdAt : String,
    val deletedAt : String
)
