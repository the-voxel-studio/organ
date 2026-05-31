package fr.studio.voxel.organ.network.services

import retrofit2.Response
import retrofit2.http.*

interface NotificationApiService {
    @GET("/api/notifications")
    suspend fun getNotifications(): Response<List<NotificationResponse>>

    @GET("/api/notifications/subscribe")
    suspend fun getSubscribeUrl(): Response<SubscribeUrlResponse>

    @GET("/api/notifications/trash")
    suspend fun getTrashedNotifications(): Response<List<NotificationResponse>>

    @POST("/api/notifications/{uuid}/restore")
    suspend fun restoreNotification(@Path("uuid") uuid: String): Response<MessageResponse>

    @PATCH("/api/notifications/{uuid}/read")
    suspend fun markAsRead(@Path("uuid") uuid: String): Response<MessageResponse>

    @DELETE("/api/notifications/{uuid}")
    suspend fun deleteNotification(@Path("uuid") uuid: String): Response<Unit>
}

data class NotificationResponse(
    val uuid: String,
    val type: String,
    val message: String,
    val isRead: Boolean = false,
    val taskUuid: String? = null,
    val createdAt: String? = null,
    val deletedAt: String? = null,
    val isRealInvite: Boolean = false
)

data class SubscribeUrlResponse(
    val hubUrl: String,
    val topic: String,
    val token: String
)
