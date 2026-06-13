package fr.studio.voxel.organ.viewmodel

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.studio.voxel.organ.data.OrganLinkRepository
import fr.studio.voxel.organ.data.OrganRepository
import fr.studio.voxel.organ.data.ProjectRepository
import fr.studio.voxel.organ.data.TaskRepository
import fr.studio.voxel.organ.network.services.*
import kotlinx.coroutines.launch

class OrganTrashViewModel : ViewModel() {

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
                ProjectRepository.getProjectPermissions(projectUuid)
                    .onSuccess { projRole ->
                        isProjectAdmin = projRole.role == "ADMIN" || projRole.role == "MANAGER"
                    }

                // 2. Get organ metadata (title & highlightColor)
                val organRes = OrganRepository.getOrgan(projectUuid, organUuid)
                organRes.onSuccess { organ ->
                    organTitle = organ.title
                    highlightColor = if (organ.highlightColor.isNotBlank()) organ.highlightColor else "#FF7DD4"
                }.onFailure {
                    errorMessage = "Organ non trouvé ou accès refusé."
                    isLoading = false
                    return@launch
                }

                // 3. Get organ permissions
                OrganRepository.getOrganPermissions(projectUuid, organUuid)
                    .onSuccess { perm ->
                        permissions = perm.permissions
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
            TaskRepository.getTrashedTasks(projectUuid, organUuid)
                .onSuccess { tasksList ->
                    trashedTasks = tasksList
                }
        }

        // Load roles & members
        if (canManageRoles()) {
            OrganRepository.getTrashedRoles(projectUuid, organUuid)
                .onSuccess { rolesList ->
                    trashedRoles = rolesList
                }

            OrganRepository.getTrashedMembers(projectUuid, organUuid)
                .onSuccess { membersList ->
                    trashedMembers = membersList
                }
        }

        // Load links
        if (canManageLinks()) {
            OrganLinkRepository.getTrashedLinks(projectUuid, organUuid)
                .onSuccess { linksList ->
                    trashedLinks = linksList
                }
        }
    }

    fun restoreTask(projectUuid: String, organUuid: String, taskUuid: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            TaskRepository.restoreTask(projectUuid, organUuid, taskUuid)
                .onSuccess {
                    trashedTasks = trashedTasks.filter { it.uuid != taskUuid }
                    onSuccess()
                }.onFailure { e ->
                    onError(e.message ?: "Impossible de restaurer la tâche.")
                }
        }
    }

    fun restoreRole(projectUuid: String, organUuid: String, roleUuid: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            OrganRepository.restoreRole(projectUuid, organUuid, roleUuid)
                .onSuccess {
                    trashedRoles = trashedRoles.filter { it.uuid != roleUuid }
                    onSuccess()
                }.onFailure { e ->
                    onError(e.message ?: "Impossible de restaurer le rôle.")
                }
        }
    }

    fun restoreMember(projectUuid: String, organUuid: String, uorId: Int, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            OrganRepository.restoreMember(projectUuid, organUuid, uorId)
                .onSuccess {
                    trashedMembers = trashedMembers.filter { it.uuid != uorId }
                    onSuccess()
                }.onFailure { e ->
                    onError(e.message ?: "Impossible de restaurer le membre.")
                }
        }
    }

    fun restoreLink(projectUuid: String, organUuid: String, linkUuid: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            OrganLinkRepository.restoreLink(projectUuid, organUuid, linkUuid)
                .onSuccess {
                    trashedLinks = trashedLinks.filter { it.uuid != linkUuid }
                    onSuccess()
                }.onFailure { e ->
                    onError(e.message ?: "Impossible de restaurer le lien.")
                }
        }
    }

    fun deletePermanentlyTask(projectUuid: String, organUuid: String, taskUuid: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            TaskRepository.deleteTask(projectUuid, organUuid, taskUuid, permanent = true)
                .onSuccess {
                    trashedTasks = trashedTasks.filter { it.uuid != taskUuid }
                    onSuccess()
                }.onFailure { e ->
                    onError(e.message ?: "Impossible de supprimer définitivement la tâche.")
                }
        }
    }

    fun deletePermanentlyRole(projectUuid: String, organUuid: String, roleUuid: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            OrganRepository.deleteOrganRole(projectUuid, organUuid, roleUuid, permanent = true)
                .onSuccess {
                    trashedRoles = trashedRoles.filter { it.uuid != roleUuid }
                    onSuccess()
                }.onFailure { e ->
                    onError(e.message ?: "Impossible de supprimer définitivement le rôle.")
                }
        }
    }

    fun deletePermanentlyMember(projectUuid: String, organUuid: String, uorId: Int, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val member = trashedMembers.find { it.uuid == uorId }
                if (member != null) {
                    OrganRepository.unassignRole(projectUuid, organUuid, member.role.uuid, member.user.uuid, permanent = true)
                        .onSuccess {
                            trashedMembers = trashedMembers.filter { it.uuid != uorId }
                            onSuccess()
                        }.onFailure { e ->
                            onError(e.message ?: "Impossible de retirer définitivement le rôle du collaborateur.")
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
            OrganLinkRepository.deleteLink(projectUuid, organUuid, linkUuid, permanent = true)
                .onSuccess {
                    trashedLinks = trashedLinks.filter { it.uuid != linkUuid }
                    onSuccess()
                }.onFailure { e ->
                    onError(e.message ?: "Impossible de supprimer définitivement le lien.")
                }
        }
    }
}
