package fr.studio.voxel.organ.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import fr.studio.voxel.organ.network.ApiClient
import fr.studio.voxel.organ.network.services.DashboardApiService
import fr.studio.voxel.organ.network.services.DashboardResponse
import fr.studio.voxel.organ.network.services.Organ
import fr.studio.voxel.organ.network.services.Project
import fr.studio.voxel.organ.network.services.ProjectApiService
import fr.studio.voxel.organ.network.services.Task

object ProjectRepository {
    private val projectService = ApiClient.createService(ProjectApiService::class.java)
    private val dashboardService = ApiClient.createService(DashboardApiService::class.java)

    var projects by mutableStateOf<List<Project>?>(null)
        private set

    var priorityTasks by mutableStateOf<List<Task>>(emptyList())
        private set

    suspend fun fetchDashboard(): Result<DashboardResponse> {
        return try {
            val response = dashboardService.getDashboardData()
            if (response.isSuccessful) {
                val data = response.body() ?: DashboardResponse(emptyList(), emptyList())
                projects = data.projects
                priorityTasks = data.tasks
                Result.success(data)
            } else {
                Result.failure(Exception("Erreur serveur : ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchProjects(): Result<List<Project>> {
        return try {
            val response = projectService.getProjects()
            if (response.isSuccessful) {
                val list = response.body() ?: emptyList()
                projects = list
                Result.success(list)
            } else {
                Result.failure(Exception("Erreur serveur : ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun addOrUpdateProject(project: Project) {
        val currentList = projects ?: emptyList()
        val exists = currentList.any { it.uuid == project.uuid }
        projects = if (exists) {
            currentList.map { if (it.uuid == project.uuid) project else it }
        } else {
            currentList + project
        }
    }

    fun removeProject(projectUuid: String) {
        projects = projects?.filter { it.uuid != projectUuid }
    }

    fun addOrUpdateOrgan(projectUuid: String, organ: Organ) {
        projects = projects?.map { project ->
            if (project.uuid == projectUuid) {
                val organsList = project.organs ?: emptyList()
                val exists = organsList.any { it.uuid == organ.uuid }
                val updatedOrgans = if (exists) {
                    organsList.map { if (it.uuid == organ.uuid) organ else it }
                } else {
                    organsList + organ
                }
                project.copy(organs = updatedOrgans)
            } else {
                project
            }
        }
    }

    fun removeOrgan(projectUuid: String, organUuid: String) {
        projects = projects?.map { project ->
            if (project.uuid == projectUuid) {
                project.copy(organs = project.organs?.filter { it.uuid != organUuid })
            } else {
                project
            }
        }
    }

    fun addOrUpdatePriorityTask(task: Task) {
        val currentTasks = priorityTasks
        val exists = currentTasks.any { it.uuid == task.uuid }
        priorityTasks = if (exists) {
            currentTasks.map { if (it.uuid == task.uuid) task else it }
        } else {
            currentTasks + task
        }
    }

    fun removePriorityTask(taskUuid: String) {
        priorityTasks = priorityTasks.filter { it.uuid != taskUuid }
    }

    fun clear() {
        projects = null
        priorityTasks = emptyList()
    }
}
