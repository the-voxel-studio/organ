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

class DashboardViewModel : ViewModel() {
    private val projectService = ApiClient.createService(ProjectApiService::class.java)

    var projects by mutableStateOf<List<Project>?>(null)
        private set

    var isLoading by mutableStateOf(false)
        private set

    var error by mutableStateOf<String?>(null)
        private set

    init{
        fetchProjects()
    }

    fun fetchProjects(){
        viewModelScope.launch {
            isLoading = true
            error = null
            try{
                val response = projectService.getProjects()
                if (response.isSuccessful){
                    projects = response.body()
                }else{
                    error = "Erreur lors de la récupération : ${response.code()}"
                }
            }catch (e: Exception){
                error = "Problème réseau : ${e.localizedMessage}"
            }finally {
                isLoading = false
            }
        }
    }
}