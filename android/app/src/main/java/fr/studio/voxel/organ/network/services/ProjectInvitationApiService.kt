package fr.studio.voxel.organ.network.services

import retrofit2.Response
import retrofit2.http.*

interface ProjectInvitationApiService {
    @GET("/api/invitations")
    suspend fun getInvitations(): Response<List<UserInvitationResponse>>

    @POST("/api/invitations/{uuid}/accept")
    suspend fun acceptInvitation(@Path("uuid") uuid: String): Response<AcceptInvitationResponse>

    @POST("/api/invitations/{uuid}/refuse")
    suspend fun refuseInvitation(@Path("uuid") uuid: String): Response<MessageResponse>
}

data class UserInvitationResponse(
    val uuid: String,
    val projectName: String,
    val invitedBy: String,
    val role: String,
    val createdAt: String, // ISO DateTime
    val expiresAt: String // ISO DateTime
)

data class AcceptInvitationResponse(
    val message: String,
    val projectUuid: String
)
