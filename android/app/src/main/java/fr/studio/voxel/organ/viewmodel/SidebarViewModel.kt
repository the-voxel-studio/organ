package fr.studio.voxel.organ.domain.model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.studio.voxel.organ.data.ProjectRepository
import fr.studio.voxel.organ.data.UserRepository
import fr.studio.voxel.organ.network.services.Project
import fr.studio.voxel.organ.network.services.User
import kotlinx.coroutines.launch

class SidebarViewModel : ViewModel() {

    val projects: List<Project>?
        get() = ProjectRepository.projects

    val currentUser: User?
        get() = UserRepository.currentUser

    var isLoading by mutableStateOf(false)
        private set

    var error by mutableStateOf<String?>(null)
        private set

    var selectedProjectUuid by mutableStateOf<String?>(null)

    init {
        if (ProjectRepository.projects == null) {
            fetchProjects()
        }
    }

    fun fetchProjects() {
        viewModelScope.launch {
            isLoading = true
            error = null
            val result = ProjectRepository.fetchProjects()
            result.onFailure { e ->
                error = "Erreur lors du chargement des projets : ${e.localizedMessage}"
            }
            isLoading = false
        }
    }

    fun selectProject(uuid: String) {
        selectedProjectUuid = uuid
    }

    fun clearSelection() {
        selectedProjectUuid = null
    }
}
