package fr.studio.voxel.organ.viewmodel

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.studio.voxel.organ.network.ApiClient
import fr.studio.voxel.organ.network.services.*
import kotlinx.coroutines.launch

class OrganTrashViewModel : ViewModel() {

    private val organService = ApiClient.createService(OrganApiService::class.java)
    private val projectService = ApiClient.createService(ProjectApiService::class.java)
    private val taskService = ApiClient.createService(TaskApiService::class.java)
    private val linkService = ApiClient.createService(OrganLinkApiService::class.java)

    var organTitle by mutableStateOf("")
        private set
    var highlightColor by mutableStateOf("#FF7DD4")
        private set
    var permissions by mutableStateOf<List<String>>(emptyList())
        private set
    var isProjectAdmin by mutableStateOf(false)
        private set

    // Lists of trashed items
    var trashedTasks by mutableStateOf<List<Task>>(emptyList())
    var trashedRoles by mutableStateOf<List<OrganRoleResponse>>(emptyList())
    var trashedMembers by mutableStateOf<List<TrashedRoleMemberResponse>>(emptyList())
    var trashedLinks by mutableStateOf<List<TrashedOrganLinkSummary>>(emptyList())

    var isLoading by mutableStateOf(true)
    var errorMessage by mutableStateOf<String?>(null)
    var accessDenied by mutableStateOf(false)

    fun hasPermission(permissionName: String): Boolean {
        return isProjectAdmin || permissions.contains("ALL") || permissions.contains(permissionName)
    }

    fun canManageTasks(): Boolean {
        return hasPermission("TASK_DELETE_ALL") || hasPermission("TASK_DELETE_OWN") || hasPermission("ORGAN_EDIT")
    }

    fun canManageRoles(): Boolean {
        return hasPermission("ORGAN_MANAGE_ROLES") || hasPermission("ORGAN_EDIT")
    }

    fun canManageLinks(): Boolean {
        return hasPermission("ORGAN_LINK_MANAGE") || hasPermission("ORGAN_EDIT")
    }

    fun loadTrash(projectUuid: String, organUuid: String) {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            accessDenied = false

            try {
                // 1. Get project role permissions to check if ADMIN or MANAGER
                val projRoleRes = projectService.getProjectPermissions(projectUuid)
                if (projRoleRes.isSuccessful) {
                    val role = projRoleRes.body()?.role ?: "MEMBER"
                    isProjectAdmin = role == "ADMIN" || role == "MANAGER"
                }

                // 2. Get organ metadata (title & highlightColor)
                val organRes = organService.getOrgan(projectUuid, organUuid)
                if (organRes.isSuccessful) {
                    val organ = organRes.body()
                    if (organ != null) {
                        organTitle = organ.title
                        highlightColor = if (organ.highlightColor.isNotBlank()) organ.highlightColor else "#FF7DD4"
                    }
                } else {
                    errorMessage = "Organ non trouvé ou accès refusé."
                    isLoading = false
                    return@launch
                }

                // 3. Get organ permissions
                val permRes = organService.getOrganPermissions(projectUuid, organUuid)
                if (permRes.isSuccessful) {
                    permissions = permRes.body()?.permissions ?: emptyList()
                }

                // Check security: must have ORGAN_VIEW or be project admin
                if (!hasPermission("ORGAN_VIEW")) {
                    accessDenied = true
                    isLoading = false
                    return@launch
                }

                // 4. Load trashed contents
                loadTrashedContent(projectUuid, organUuid)

            } catch (e: Exception) {
                Log.e("ORGAN_TRASH_VM", "Error loading organ trash", e)
                errorMessage = "Une erreur réseau est survenue."
            } finally {
                isLoading = false
            }
        }
    }

    private suspend fun loadTrashedContent(projectUuid: String, organUuid: String) {
        // Load tasks
        if (canManageTasks()) {
            val tasksRes = taskService.getTrashedTasks(projectUuid, organUuid)
            if (tasksRes.isSuccessful) {
                trashedTasks = tasksRes.body() ?: emptyList()
            }
        }

        // Load roles & members
        if (canManageRoles()) {
            val rolesRes = organService.getTrashedRoles(projectUuid, organUuid)
            if (rolesRes.isSuccessful) {
                trashedRoles = rolesRes.body() ?: emptyList()
            }

            val membersRes = organService.getTrashedMembers(projectUuid, organUuid)
            if (membersRes.isSuccessful) {
                trashedMembers = membersRes.body() ?: emptyList()
            }
        }

        // Load links
        if (canManageLinks()) {
            val linksRes = linkService.getTrashedLinks(projectUuid, organUuid)
            if (linksRes.isSuccessful) {
                trashedLinks = linksRes.body() ?: emptyList()
            }
        }
    }

    fun restoreTask(projectUuid: String, organUuid: String, taskUuid: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val res = taskService.restoreTask(projectUuid, organUuid, taskUuid)
                if (res.isSuccessful) {
                    trashedTasks = trashedTasks.filter { it.uuid != taskUuid }
                    onSuccess()
                } else {
                    onError(res.message() ?: "Impossible de restaurer la tâche.")
                }
            } catch (e: Exception) {
                onError("Erreur réseau.")
            }
        }
    }

    fun restoreRole(projectUuid: String, organUuid: String, roleUuid: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val res = organService.restoreRole(projectUuid, organUuid, roleUuid)
                if (res.isSuccessful) {
                    trashedRoles = trashedRoles.filter { it.uuid != roleUuid }
                    onSuccess()
                } else {
                    onError(res.message() ?: "Impossible de restaurer le rôle.")
                }
            } catch (e: Exception) {
                onError("Erreur réseau.")
            }
        }
    }

    fun restoreMember(projectUuid: String, organUuid: String, uorId: Int, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val res = organService.restoreMember(projectUuid, organUuid, uorId)
                if (res.isSuccessful) {
                    trashedMembers = trashedMembers.filter { it.uuid != uorId }
                    onSuccess()
                } else {
                    onError(res.message() ?: "Impossible de restaurer le membre.")
                }
            } catch (e: Exception) {
                onError("Erreur réseau.")
            }
        }
    }

    fun restoreLink(projectUuid: String, organUuid: String, linkUuid: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val res = linkService.restoreLink(projectUuid, organUuid, linkUuid)
                if (res.isSuccessful) {
                    trashedLinks = trashedLinks.filter { it.uuid != linkUuid }
                    onSuccess()
                } else {
                    onError(res.message() ?: "Impossible de restaurer le lien.")
                }
            } catch (e: Exception) {
                onError("Erreur réseau.")
            }
        }
    }

    fun deletePermanentlyTask(projectUuid: String, organUuid: String, taskUuid: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val res = taskService.deleteTask(projectUuid, organUuid, taskUuid, permanent = true)
                if (res.isSuccessful) {
                    trashedTasks = trashedTasks.filter { it.uuid != taskUuid }
                    onSuccess()
                } else {
                    onError(res.message() ?: "Impossible de supprimer définitivement la tâche.")
                }
            } catch (e: Exception) {
                onError("Erreur réseau.")
            }
        }
    }

    fun deletePermanentlyRole(projectUuid: String, organUuid: String, roleUuid: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val res = organService.deleteOrganRole(projectUuid, organUuid, roleUuid, permanent = true)
                if (res.isSuccessful) {
                    trashedRoles = trashedRoles.filter { it.uuid != roleUuid }
                    onSuccess()
                } else {
                    onError(res.message() ?: "Impossible de supprimer définitivement le rôle.")
                }
            } catch (e: Exception) {
                onError("Erreur réseau.")
            }
        }
    }

    fun deletePermanentlyMember(projectUuid: String, organUuid: String, uorId: Int, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val member = trashedMembers.find { it.uuid == uorId }
                if (member != null) {
                    val res = organService.unassignRole(projectUuid, organUuid, member.role.uuid, member.user.uuid, permanent = true)
                    if (res.isSuccessful) {
                        trashedMembers = trashedMembers.filter { it.uuid != uorId }
                        onSuccess()
                    } else {
                        onError(res.message() ?: "Impossible de retirer définitivement le rôle du collaborateur.")
                    }
                } else {
                    onError("Membre introuvable.")
                }
            } catch (e: Exception) {
                onError("Erreur réseau.")
            }
        }
    }

    fun deletePermanentlyLink(projectUuid: String, organUuid: String, linkUuid: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val res = linkService.deleteLink(projectUuid, organUuid, linkUuid, permanent = true)
                if (res.isSuccessful) {
                    trashedLinks = trashedLinks.filter { it.uuid != linkUuid }
                    onSuccess()
                } else {
                    onError(res.message() ?: "Impossible de supprimer définitivement le lien.")
                }
            } catch (e: Exception) {
                onError("Erreur réseau.")
            }
        }
    }
}
