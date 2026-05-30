package fr.studio.voxel.organ.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.studio.voxel.organ.data.ProjectRepository
import fr.studio.voxel.organ.network.services.Project
import fr.studio.voxel.organ.network.services.Task
import kotlinx.coroutines.launch

class DashboardViewModel : ViewModel() {

    val projects: List<Project>?
        get() = ProjectRepository.projects

    val priorityTask: List<Task>
        get() = ProjectRepository.priorityTasks

    var isLoading by mutableStateOf(false)
        private set

    var error by mutableStateOf<String?>(null)
        private set

    init {
        loadDashboard()
    }

    fun loadDashboard() {
        viewModelScope.launch {
            isLoading = true
            error = null
            val result = ProjectRepository.fetchDashboard()
            result.onFailure { e ->
                error = "Problème de chargement : ${e.localizedMessage}"
            }
            isLoading = false
        }
    }

    fun refresh() {
        loadDashboard()
    }
}