package fr.studio.voxel.organ.network.services

import retrofit2.Response
import retrofit2.http.*

interface NotificationApiService {
    @GET("/api/notifications")
    suspend fun getNotifications(): Response<List<Any>>

    @GET("/api/notifications/subscribe")
    suspend fun getSubscribeUrl(): Response<Map<String, String>>

    @GET("/api/notifications/trash")
    suspend fun getTrashedNotifications(): Response<List<Any>>

    @POST("/api/notifications/{uuid}/restore")
    suspend fun restoreNotification(@Path("uuid") uuid: String): Response<Unit>

    @PATCH("/api/notifications/{uuid}/read")
    suspend fun markAsRead(@Path("uuid") uuid: String): Response<Unit>

    @DELETE("/api/notifications/{uuid}")
    suspend fun deleteNotification(@Path("uuid") uuid: String): Response<Unit>
}
