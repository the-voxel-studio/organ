package fr.studio.voxel.organ.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.studio.voxel.organ.data.ProjectRepository
import fr.studio.voxel.organ.network.services.Project
import kotlinx.coroutines.launch

class ProjectViewModel : ViewModel() {

    var projects by mutableStateOf<List<Project>>(emptyList())
        private set

    fun loadProjects() {
        viewModelScope.launch {
            try {
                ProjectRepository.fetchProjects().onSuccess { list ->
                    projects = list
                }.onFailure {
                    // Gérer l'erreur
                }
            } catch (e: Exception) {
                // Gérer l'erreur
            }
        }
    }
}