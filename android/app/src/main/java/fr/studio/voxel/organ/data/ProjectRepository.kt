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

import fr.studio.voxel.organ.network.getErrorMessageForCode

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
                Result.failure(Exception(getErrorMessageForCode(response.code(), "Erreur serveur")))
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
                Result.failure(Exception(getErrorMessageForCode(response.code(), "Erreur serveur")))
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

    suspend fun getTrashedProjects(): Result<List<Project>> {
        return try {
            val response = projectService.getTrashedProjects()
            if (response.isSuccessful) {
                Result.success(response.body() ?: emptyList())
            } else {
                Result.failure(Exception(getErrorMessageForCode(response.code(), "Impossible de charger la corbeille.")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun restoreProject(projectUuid: String): Result<Unit> {
        return try {
            val response = projectService.restoreProject(projectUuid)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(getErrorMessageForCode(response.code(), "Échec de la restauration du projet.")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteProjectPermanently(projectUuid: String): Result<Unit> {
        return try {
            val response = projectService.deleteProject(projectUuid, permanent = true)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(getErrorMessageForCode(response.code(), "Échec de la suppression définitive.")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getProjectPermissions(projectUuid: String): Result<fr.studio.voxel.organ.network.services.ProjectRoleResponse> {
        return try {
            val response = projectService.getProjectPermissions(projectUuid)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(getErrorMessageForCode(response.code(), "Erreur serveur")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getProject(projectUuid: String): Result<Project> {
        return try {
            val response = projectService.getProject(projectUuid)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(getErrorMessageForCode(response.code(), "Erreur serveur")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getProjectMembers(projectUuid: String): Result<List<fr.studio.voxel.organ.network.services.ProjectMember>> {
        return try {
            val response = projectService.getProjectMembers(projectUuid)
            if (response.isSuccessful) {
                Result.success(response.body() ?: emptyList())
            } else {
                Result.failure(Exception(getErrorMessageForCode(response.code(), "Erreur serveur")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getProjectInvitations(projectUuid: String): Result<List<fr.studio.voxel.organ.network.services.ProjectInvitation>> {
        return try {
            val response = projectService.getProjectInvitations(projectUuid)
            if (response.isSuccessful) {
                Result.success(response.body() ?: emptyList())
            } else {
                Result.failure(Exception(getErrorMessageForCode(response.code(), "Erreur serveur")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createProject(project: Project): Result<Project> {
        return try {
            val response = projectService.createProject(project)
            if (response.isSuccessful && response.body() != null) {
                val created = response.body()!!
                addOrUpdateProject(created)
                Result.success(created)
            } else {
                Result.failure(Exception(getErrorMessageForCode(response.code(), "Erreur serveur")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateProject(projectUuid: String, project: Project): Result<Project> {
        return try {
            val response = projectService.updateProject(projectUuid, project)
            if (response.isSuccessful && response.body() != null) {
                val updated = response.body()!!
                addOrUpdateProject(updated)
                Result.success(updated)
            } else {
                Result.failure(Exception(getErrorMessageForCode(response.code(), "Erreur serveur")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun removeMember(projectUuid: String, memberUuid: String, permanent: Boolean = false): Result<Unit> {
        return try {
            val response = projectService.removeMember(projectUuid, memberUuid, permanent)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(getErrorMessageForCode(response.code(), "Erreur serveur")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getTrashedProjectMembers(projectUuid: String): Result<List<fr.studio.voxel.organ.network.services.ProjectMember>> {
        return try {
            val response = projectService.getTrashedProjectMembers(projectUuid)
            if (response.isSuccessful) {
                Result.success(response.body() ?: emptyList())
            } else {
                Result.failure(Exception(getErrorMessageForCode(response.code(), "Erreur serveur")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun restoreMember(projectUuid: String, memberUuid: String): Result<Unit> {
        return try {
            val response = projectService.restoreMember(projectUuid, memberUuid)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(getErrorMessageForCode(response.code(), "Erreur serveur")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    suspend fun updateMemberRole(projectUuid: String, memberUuid: String, role: String): Result<Unit> {
        return try {
            val response = projectService.updateMemberRole(projectUuid, memberUuid, fr.studio.voxel.organ.network.services.UpdateRoleRequest(role))
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(getErrorMessageForCode(response.code(), "Erreur serveur")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun inviteMember(projectUuid: String, email: String, role: String?): Result<Unit> {
        return try {
            val response = projectService.inviteMember(projectUuid, fr.studio.voxel.organ.network.services.InvitationRequest(email, role))
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(getErrorMessageForCode(response.code(), "Erreur serveur")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteProject(projectUuid: String): Result<Unit> {
        return try {
            val response = projectService.deleteProject(projectUuid)
            if (response.isSuccessful) {
                removeProject(projectUuid)
                Result.success(Unit)
            } else {
                Result.failure(Exception(getErrorMessageForCode(response.code(), "Erreur serveur")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun clear() {
        projects = null
        priorityTasks = emptyList()
    }
}
