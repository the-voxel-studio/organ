package fr.studio.voxel.organ.viewmodel

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.studio.voxel.organ.data.ProjectRepository
import fr.studio.voxel.organ.network.services.Project
import kotlinx.coroutines.launch

class TrashViewModel : ViewModel() {

    var trashedProjects by mutableStateOf<List<Project>>(emptyList())
        private set

    var isLoading by mutableStateOf(true)
    var errorMessage by mutableStateOf<String?>(null)

    fun loadTrashedProjects() {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            ProjectRepository.getTrashedProjects().onSuccess { list ->
                trashedProjects = list
            }.onFailure { e ->
                Log.e("TrashViewModel", "Error loading trashed projects", e)
                errorMessage = e.message ?: "Une erreur réseau est survenue."
            }
            isLoading = false
        }
    }

    fun restoreProject(projectUuid: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            ProjectRepository.restoreProject(projectUuid).onSuccess {
                trashedProjects = trashedProjects.filter { it.uuid != projectUuid }
                onSuccess()
            }.onFailure { e ->
                onError(e.message ?: "Erreur réseau.")
            }
        }
    }

    fun deleteProjectPermanently(projectUuid: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            ProjectRepository.deleteProjectPermanently(projectUuid).onSuccess {
                trashedProjects = trashedProjects.filter { it.uuid != projectUuid }
                onSuccess()
            }.onFailure { e ->
                onError(e.message ?: "Erreur réseau.")
            }
        }
    }
}
