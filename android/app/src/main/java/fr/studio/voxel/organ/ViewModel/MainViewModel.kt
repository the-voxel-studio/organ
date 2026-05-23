package fr.studio.voxel.organ.ViewModel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.studio.voxel.organ.ui.components.DashboardComponents.ProjectVisual
import fr.studio.voxel.organ.R
import fr.studio.voxel.organ.network.ApiClient
import fr.studio.voxel.organ.network.services.Project
import fr.studio.voxel.organ.network.services.ProjectApiService
import fr.studio.voxel.organ.ui.components.DashboardComponents.ProjectStateSticker
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {

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

                if(response.isSuccessful){
                    projects = response.body() ?: emptyList()
                }else{
                    error = "Erreur serveur : ${response.code()}"
                }
            } catch (e: Exception){
                error = "Problème réseau : ${e.localizedMessage}"
            }finally {
                isLoading = false
            }
        }
    }

    //Future fonction à ajouter à l'ajout de projet
    fun refresh(){
        fetchProjects()
    }

    var users by mutableStateOf(listOf<User>())
        private set

    //=====A l'avenir les projets viendront de la BDD=====

    var tasks by mutableStateOf(
        listOf(
            Task(
                id = 1,
                name = "Tache Alpha",
                progress = 4,
                deadline = "26/02/18",
                projectId = 1,
                organId = 1
            ),
            Task(
                id = 2,
                name = "Tache Bêta",
                progress = 2,
                deadline = "04/08/22",
                projectId = 1,
                organId = 2
            ),
            Task(
                id = 2,
                name = "Tache Omega",
                progress = 8,
                deadline = "10/11/28",
                projectId = 2,
                organId = 3
            )
        )
    )
        private set

    // Pour les tests, fonction de réinitialisation
    /*fun clearProjects() {
        projects = emptyList() // L'utilisateur existe mais n'a aucun projet
    }

    fun addProject(project: Project) {
        projects = projects?.plus(project)
    }

    fun addOrgan(projectId: Int, organ: Organ) {
        projects = projects?.map {
            if (it.id == projectId) {
                it.copy(organs = it.organs + organ)
            } else it
        }
    }

    fun addUserToOrgan(projectId: Int, organId: Int, userId: Int) {
        projects = projects?.map { project ->
            if (project.id == projectId) {
                project.copy(
                    organs = project.organs.map { organ ->
                        if (organ.id == organId) {
                            organ.copy(memberIds = organ.memberIds + userId)
                        } else organ
                    }
                )
            } else project
        }
    }*/
}