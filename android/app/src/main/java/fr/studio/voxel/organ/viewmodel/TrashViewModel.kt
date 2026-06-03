package fr.studio.voxel.organ.viewmodel

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.studio.voxel.organ.network.ApiClient
import fr.studio.voxel.organ.network.services.Project
import fr.studio.voxel.organ.network.services.ProjectApiService
import kotlinx.coroutines.launch

class TrashViewModel : ViewModel() {
    private val projectService = ApiClient.createService(ProjectApiService::class.java)

    var trashedProjects by mutableStateOf<List<Project>>(emptyList())
        private set

    var isLoading by mutableStateOf(true)
    var errorMessage by mutableStateOf<String?>(null)

    fun loadTrashedProjects() {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            try {
                val response = projectService.getTrashedProjects()
                if (response.isSuccessful) {
                    trashedProjects = response.body() ?: emptyList()
                } else {
                    errorMessage = "Impossible de charger la corbeille."
                }
            } catch (e: Exception) {
                Log.e("TrashViewModel", "Error loading trashed projects", e)
                errorMessage = "Une erreur réseau est survenue."
            } finally {
                isLoading = false
            }
        }
    }

    fun restoreProject(projectUuid: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val res = projectService.restoreProject(projectUuid)
                if (res.isSuccessful) {
                    trashedProjects = trashedProjects.filter { it.uuid != projectUuid }
                    onSuccess()
                } else {
                    onError(res.message().ifBlank { "Échec de la restauration du projet." })
                }
            } catch (e: Exception) {
                onError("Erreur réseau.")
            }
        }
    }

    fun deleteProjectPermanently(projectUuid: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val res = projectService.deleteProject(projectUuid, permanent = true)
                if (res.isSuccessful) {
                    trashedProjects = trashedProjects.filter { it.uuid != projectUuid }
                    onSuccess()
                } else {
                    onError(res.message().ifBlank { "Échec de la suppression définitive." })
                }
            } catch (e: Exception) {
                onError("Erreur réseau.")
            }
        }
    }
}
