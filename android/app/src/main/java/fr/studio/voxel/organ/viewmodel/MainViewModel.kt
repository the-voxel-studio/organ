package fr.studio.voxel.organ.viewmodel

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import fr.studio.voxel.organ.MainActivity
import fr.studio.voxel.organ.network.ApiClient
import fr.studio.voxel.organ.network.services.OrganApiService
import fr.studio.voxel.organ.network.services.Project
import fr.studio.voxel.organ.network.services.ProjectApiService
import fr.studio.voxel.organ.network.services.TaskApiService
import fr.studio.voxel.organ.network.services.UserApiService
import fr.studio.voxel.organ.network.services.Task
import fr.studio.voxel.organ.network.services.User
import kotlinx.coroutines.launch

@Composable
fun sharedMainViewModel(): MainViewModel{
    val context = LocalContext.current
    return viewModel(viewModelStoreOwner = context as MainActivity)
}

class MainViewModel : ViewModel() {

    private val userService = ApiClient.createService(UserApiService::class.java)
    private val taskService = ApiClient.createService(TaskApiService::class.java)
    private val projectService = ApiClient.createService(ProjectApiService::class.java)
    private val organService = ApiClient.createService(OrganApiService::class.java)

    var currentUser by mutableStateOf<User?>(null)
    var priorityTask by mutableStateOf<List<Task>>(emptyList())

    var projects by mutableStateOf<List<Project>?>(null)
        private set

    var isLoading by mutableStateOf(false)
        private set

    var error by mutableStateOf<String?>(null)
        private set

    init{
        loadDashboard()
    }

    suspend fun fetchProjects() {
        error = null
        try {
            val response = projectService.getProjects()

            if (response.isSuccessful) {
                val projectsList = response.body() ?: emptyList()

                val fullProjects = projectsList.map{ project ->
                    val organRes = organService.getOrgans(project.uuid)
                    if(organRes.isSuccessful){
                        project.copy(organs = organRes.body())
                    }else project
                }
                projects = fullProjects

            } else {
                error = "Erreur serveur : ${response.code()}"
            }
        } catch (e: Exception) {
            error = "Problème réseau : ${e.localizedMessage}"
        }
    }

    suspend fun fetchAllTasks() {
        val allTasks = mutableStateListOf<Task>()
        val currentProjects = projects ?: return
        val userUuid = currentUser?.uuid ?: return

        try {
            for (project in currentProjects) {
                project.organs?.forEach { organ ->
                    val response = taskService.getTasks(project.uuid, organ.uuid)
                    if (response.isSuccessful) {
                        val organTasks = response.body() ?: emptyList()
                        val myTasks = organTasks.filter{ task ->
                            task.createdBy?.uuid == userUuid || task.manager?.uuid == userUuid
                        }.map { task ->
                            task.copy(
                                projectName = project.title,
                                organName = organ.title
                            )
                        }
                        allTasks.addAll(myTasks)
                    } else{
                        println("DEBUG: Erreur ${response.code()} sur le projet ${project.title}")
                        if (response.code() == 403) { error = "Accès refusé aux tâches de certains projets" }
                    }
                }
            }
            priorityTask = allTasks.sortedBy { it.priority }.take(5)
        } catch (e: Exception) {
            error = "Erreur lors de la récupération des tâches : ${e.localizedMessage}"
        }
    }

    fun loadDashboard(){
        viewModelScope.launch {
            isLoading = true
            try{
                fetchProjects()

                val userRes = userService.getCurrentUser()
                if (userRes.isSuccessful) currentUser = userRes.body()

                if(!projects.isNullOrEmpty()){
                    fetchAllTasks()
                } else{
                    println("DEBUG: Aucun projet trouvé, donc aucune tâche à chercher.")

                }

            }catch (e : Exception){
                error = "Erreur de chargement: ${e.localizedMessage}"
            } finally{
                isLoading = false
            }
        }
    }

    //Future fonction à ajouter à l'ajout de projet
    fun refresh(){
        viewModelScope.launch {
            fetchProjects()
            fetchAllTasks()
        }
    }

    fun clearData(){
        projects = null
        error = null
        currentUser = null
        priorityTask = emptyList()
    }

    var users by mutableStateOf(listOf<User>())
        private set

    //=====A l'avenir les projets viendront de la BDD=====


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