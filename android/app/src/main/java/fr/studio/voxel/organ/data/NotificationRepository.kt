package fr.studio.voxel.organ.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import fr.studio.voxel.organ.network.ApiClient
import fr.studio.voxel.organ.network.services.NotificationApiService
import fr.studio.voxel.organ.network.services.NotificationResponse
import fr.studio.voxel.organ.network.services.ProjectInvitationApiService
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

object NotificationRepository {
    private val notificationService = ApiClient.createService(NotificationApiService::class.java)
    private val invitationService = ApiClient.createService(ProjectInvitationApiService::class.java)

    var notifications by mutableStateOf<List<NotificationResponse>>(emptyList())
        private set

    val unreadNotificationsCount: Int
        get() = notifications.count { !it.isRead }

    private val sseClient = OkHttpClient.Builder()
        .readTimeout(0, java.util.concurrent.TimeUnit.MILLISECONDS) // infinite read timeout for SSE stream
        .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    private var sseJob: Job? = null
    private var activeSseCall: okhttp3.Call? = null
    private val gson = com.google.gson.Gson()

    suspend fun loadNotifications(): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val notifsRes = notificationService.getNotifications()
                val invitesRes = invitationService.getInvitations()

                if (notifsRes.isSuccessful && invitesRes.isSuccessful) {
                    val notifs = notifsRes.body() ?: emptyList()
                    val invites = invitesRes.body() ?: emptyList()

                    val mappedInvites = invites.map { inv ->
                        NotificationResponse(
                            uuid = inv.uuid,
                            type = "PROJECT_INVITATION",
                            message = "Vous avez été invité à rejoindre le projet \"${inv.projectName}\" par ${inv.invitedBy}",
                            isRead = false,
                            createdAt = inv.createdAt,
                            isRealInvite = true
                        )
                    }

                    val all = mappedInvites + notifs
                    withContext(Dispatchers.Main) {
                        sortAndSetNotifications(all)
                    }
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("Erreur lors de la récupération des notifications / invitations"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    private fun sortAndSetNotifications(list: List<NotificationResponse>) {
        val sorted = list.sortedWith(compareBy<NotificationResponse> {
            // isRealInvite first (true before false, i.e. false < true, so we invert)
            !it.isRealInvite
        }.thenBy {
            // isRead false first (unread before read)
            it.isRead
        }.thenByDescending {
            // newest first
            it.createdAt ?: ""
        })
        notifications = sorted
    }

    fun setupMercure() {
        disconnect()
        sseJob = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                val subscribeRes = try {
                    notificationService.getSubscribeUrl()
                } catch (e: Exception) {
                    null
                }

                if (subscribeRes == null || !subscribeRes.isSuccessful) {
                    delay(5000)
                    continue
                }

                val data = subscribeRes.body() ?: continue

                try {
                    val serverHost = try {
                        java.net.URI(ApiClient.getBaseUrl()).host ?: "10.0.2.2"
                    } catch (e: Exception) {
                        "10.0.2.2"
                    }
                    val httpUrl = okhttp3.HttpUrl.Builder()
                        .scheme("http")
                        .host(serverHost)
                        .port(9090)
                        .addPathSegment(".well-known")
                        .addPathSegment("mercure")
                        .addQueryParameter("topic", data.topic)
                        .addQueryParameter("authorization", data.token)
                        .build()

                    val request = Request.Builder()
                        .url(httpUrl)
                        .header("Accept", "text/event-stream")
                        .build()

                    val call = sseClient.newCall(request)
                    activeSseCall = call

                    val response = call.execute()
                    if (response.isSuccessful) {
                        val reader = response.body?.charStream()?.buffered()
                        var line: String? = null
                        while (isActive && reader?.readLine().also { line = it } != null) {
                            val currentLine = line ?: break
                            if (currentLine.startsWith("data:")) {
                                val jsonData = currentLine.substring(5).trim()
                                withContext(Dispatchers.Main) {
                                    handleSseMessage(jsonData)
                                }
                            }
                        }
                    }
                } catch (e: IOException) {
                    // Connection error or call cancelled
                } catch (e: Exception) {
                    // Other exceptions
                }

                activeSseCall = null
                delay(3000) // Wait 3s before reconnecting
            }
        }
    }

    fun disconnect() {
        sseJob?.cancel()
        sseJob = null
        activeSseCall?.cancel()
        activeSseCall = null
    }

    fun clear() {
        disconnect()
        notifications = emptyList()
    }


    private fun handleSseMessage(jsonData: String) {
        try {
            val notification = gson.fromJson(jsonData, NotificationResponse::class.java)
            if (notification.type == "PROJECT_INVITATION") {
                CoroutineScope(Dispatchers.Main).launch {
                    loadNotifications()
                }
            } else {
                val currentList = notifications.toMutableList()
                currentList.removeAll { it.uuid == notification.uuid }
                currentList.add(notification)
                sortAndSetNotifications(currentList)
            }
        } catch (e: Exception) {
            // Parsing error
        }
    }

    suspend fun markAsRead(uuid: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val res = notificationService.markAsRead(uuid)
                if (res.isSuccessful) {
                    withContext(Dispatchers.Main) {
                        notifications = notifications.map {
                            if (it.uuid == uuid) it.copy(isRead = true) else it
                        }
                    }
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("Erreur lors du marquage comme lu"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun markAllAsRead(): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val unread = notifications.filter { !it.isRead && !it.isRealInvite }
                unread.forEach {
                    notificationService.markAsRead(it.uuid)
                }
                withContext(Dispatchers.Main) {
                    notifications = notifications.map {
                        if (!it.isRealInvite) it.copy(isRead = true) else it
                    }
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun deleteNotification(uuid: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val res = notificationService.deleteNotification(uuid)
                if (res.isSuccessful) {
                    loadNotifications()
                } else {
                    Result.failure(Exception("Erreur lors de la suppression de la notification"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun acceptInvitation(uuid: String): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val res = invitationService.acceptInvitation(uuid)
                if (res.isSuccessful) {
                    val body = res.body()
                    if (body != null) {
                        fr.studio.voxel.organ.data.ProjectRepository.fetchProjects()
                        loadNotifications()
                        Result.success(body.projectUuid)
                    } else {
                        Result.failure(Exception("Réponse vide du serveur"))
                    }
                } else {
                    Result.failure(Exception("Erreur lors de l'acceptation de l'invitation"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun refuseInvitation(uuid: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val res = invitationService.refuseInvitation(uuid)
                if (res.isSuccessful) {
                    loadNotifications()
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("Erreur lors du refus de l'invitation"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
