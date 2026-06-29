package fr.studio.voxel.organ.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.studio.voxel.organ.data.NotificationRepository
import fr.studio.voxel.organ.network.services.NotificationResponse
import kotlinx.coroutines.launch

class NotificationViewModel : ViewModel() {

    val notifications: List<NotificationResponse>
        get() = NotificationRepository.notifications

    val unreadCount: Int
        get() = NotificationRepository.unreadNotificationsCount

    var isLoading by mutableStateOf(false)
        private set

    var error by mutableStateOf<String?>(null)
        private set

    init {
        loadNotifications()
    }

    fun loadNotifications() {
        viewModelScope.launch {
            isLoading = true
            error = null
            val result = NotificationRepository.loadNotifications()
            result.onFailure { e ->
                error = e.localizedMessage ?: "Erreur lors du chargement des notifications"
            }
            isLoading = false
        }
    }

    fun markAsRead(uuid: String) {
        viewModelScope.launch {
            NotificationRepository.markAsRead(uuid)
        }
    }

    fun markAllAsRead() {
        viewModelScope.launch {
            isLoading = true
            val result = NotificationRepository.markAllAsRead()
            result.onFailure { e ->
                error = e.localizedMessage ?: "Erreur lors du marquage des notifications"
            }
            isLoading = false
        }
    }

    fun deleteNotification(uuid: String) {
        viewModelScope.launch {
            isLoading = true
            val result = NotificationRepository.deleteNotification(uuid)
            result.onFailure { e ->
                error = e.localizedMessage ?: "Erreur lors de la suppression"
            }
            isLoading = false
        }
    }

    fun acceptInvitation(uuid: String, onSuccess: (String) -> Unit = {}) {
        viewModelScope.launch {
            isLoading = true
            val result = NotificationRepository.acceptInvitation(uuid)
            result.onSuccess { projectUuid ->
                onSuccess(projectUuid)
            }.onFailure { e ->
                error = e.localizedMessage ?: "Erreur lors de l'acceptation de l'invitation"
            }
            isLoading = false
        }
    }

    fun refuseInvitation(uuid: String) {
        viewModelScope.launch {
            isLoading = true
            val result = NotificationRepository.refuseInvitation(uuid)
            result.onFailure { e ->
                error = e.localizedMessage ?: "Erreur lors du refus de l'invitation"
            }
            isLoading = false
        }
    }
}
