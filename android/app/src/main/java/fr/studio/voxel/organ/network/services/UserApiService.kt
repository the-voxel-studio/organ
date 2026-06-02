package fr.studio.voxel.organ.network.services

import retrofit2.Response
import retrofit2.http.*

interface UserApiService {
    @GET("/api/users/me")
    suspend fun getCurrentUser(): Response<User>

    @PUT("/api/users/me")
    suspend fun updateProfile(@Body req: UpdateUserRequest): Response<UpdateUserResponse>

    @GET("/api/users/me/connections")
    suspend fun getConnections(): Response<List<UserConnection>>

    @DELETE("/api/users/me/connections/{uuid}")
    suspend fun revokeConnection(@Path("uuid") uuid: String): Response<MessageResponse>

    @PUT("/api/users/me/password")
    suspend fun updatePassword(@Body req: UpdatePasswordRequest): Response<MessageResponse>

    @DELETE("/api/users/me")
    suspend fun deleteAccount(): Response<DeleteUserResponse>

    @POST("/api/users/me/link-google")
    suspend fun linkGoogleAccount(@Body req: LinkGoogleRequest): Response<LinkGoogleResponse>
}

data class User(
    val uuid : String,
    val email : String,
    val firstName : String,
    val lastName : String,
    val isVerified : Boolean = false,
    val createdAt : String = "",
    val authWithGoogle : Boolean = false
)

data class UpdateUserRequest(
    val firstName: String,
    val lastName: String,
    val email: String
)

data class UpdateUserResponse(
    val message: String,
    val refresh: Boolean,
    val user: UserProfileData
)

data class UserProfileData(
    val firstName: String,
    val lastName: String,
    val email: String
)

data class UserConnection(
    val uuid: String,
    val deviceName: String,
    val browserName: String,
    val location: String,
    val ipAddress: String,
    val lastUsedAt: String,
    val createdAt: String,
    val isCurrent: Boolean
)

data class UpdatePasswordRequest(
    val currentPassword: String,
    val newPassword: String
)

data class MessageResponse(
    val message: String
)

data class DeleteUserResponse(
    val message: String,
    val deletedAt: String
)

data class LinkGoogleRequest(
    val idToken: String? = null,
    val token: String? = null
)

data class LinkGoogleResponse(
    val message: String,
    val refresh: Boolean,
    val user: LinkGoogleUserData
)

data class LinkGoogleUserData(
    val email: String,
    val authWithGoogle: Boolean
)
