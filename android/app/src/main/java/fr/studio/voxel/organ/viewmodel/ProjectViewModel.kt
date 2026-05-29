package fr.studio.voxel.organ.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.studio.voxel.organ.network.ApiClient
import fr.studio.voxel.organ.network.services.Project
import fr.studio.voxel.organ.network.services.ProjectApiService
import kotlinx.coroutines.launch

class ProjectViewModel : ViewModel() {
    // 1. On récupère le service
    private val projectService = ApiClient.createService(ProjectApiService::class.java)

    // 2. On crée une liste qui sera observée par l'UI
    var projects by mutableStateOf<List<Project>>(emptyList())

    fun loadProjects() {
        viewModelScope.launch {
            try {
                val response = projectService.getProjects()
                if (response.isSuccessful) {
                    projects = response.body() ?: emptyList()
                }
            } catch (e: Exception) {
                // Gérer l'erreur ici (ex: afficher un message à l'utilisateur)
            }
        }
    }
}