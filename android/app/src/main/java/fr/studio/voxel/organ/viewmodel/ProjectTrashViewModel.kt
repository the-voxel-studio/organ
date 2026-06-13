package fr.studio.voxel.organ.viewmodel

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.studio.voxel.organ.data.OrganRepository
import fr.studio.voxel.organ.data.ProjectRepository
import fr.studio.voxel.organ.data.ProjectStatsRepository
import fr.studio.voxel.organ.data.TagRepository
import fr.studio.voxel.organ.network.services.*
import kotlinx.coroutines.launch

class ProjectTrashViewModel : ViewModel() {

    var projectTitle by mutableStateOf("")
        private set
    var highlightColor by mutableStateOf("#FF7EB6")
        private set
    var userRole by mutableStateOf("MEMBER")
        private set
    var isProjectAdmin by mutableStateOf(false)
        private set

    // Lists of trashed items
    var trashedOrgans by mutableStateOf<List<Organ>>(emptyList())
        private set
    var trashedMembers by mutableStateOf<List<ProjectMember>>(emptyList())
        private set
    var trashedTags by mutableStateOf<List<TagResponse>>(emptyList())
        private set

    var isLoading by mutableStateOf(true)
    var errorMessage by mutableStateOf<String?>(null)
    var accessDenied by mutableStateOf(false)

    // Bulk selection for members
    var selectedMembers by mutableStateOf<Set<String>>(emptySet())
        private set

    fun toggleMemberSelection(memberUuid: String) {
        selectedMembers = if (selectedMembers.contains(memberUuid)) {
            selectedMembers - memberUuid
        } else {
            selectedMembers + memberUuid
        }
    }

    fun clearMemberSelection() {
        selectedMembers = emptySet()
    }

    fun selectAllMembers() {
        selectedMembers = trashedMembers.map { it.uuid }.toSet()
    }

    fun loadTrash(projectUuid: String) {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            accessDenied = false
            clearMemberSelection()

            try {
                // 1. Get detailed project info to read role, title, and color
                val detailedRes = ProjectStatsRepository.getDetailedProject(projectUuid)
                if (detailedRes.isSuccessful) {
                    val detailedData = detailedRes.body()
                    if (detailedData != null) {
                        projectTitle = detailedData.project.title
                        highlightColor = detailedData.project.color ?: "#FF7EB6"
                        userRole = detailedData.project.role ?: "MEMBER"
                        isProjectAdmin = userRole == "ADMIN" || userRole == "MANAGER"

                        // Check authorization: only ADMIN or MANAGER allowed in trash
                        if (!isProjectAdmin) {
                            accessDenied = true
                            isLoading = false
                            return@launch
                        }

                        // 2. Load trashed items
                        loadTrashedContent(projectUuid)
                    } else {
                        errorMessage = "Données du projet introuvables."
                    }
                } else {
                    if (detailedRes.code() == 403) {
                        accessDenied = true
                    } else {
                        errorMessage = "Projet non trouvé ou accès refusé."
                    }
                }
            } catch (e: Exception) {
                Log.e("PROJECT_TRASH_VM", "Error loading project trash", e)
                errorMessage = "Une erreur réseau est survenue."
            } finally {
                isLoading = false
            }
        }
    }

    private suspend fun loadTrashedContent(projectUuid: String) {
        // Load Organs
        OrganRepository.getTrashedOrgans(projectUuid)
            .onSuccess { list ->
                trashedOrgans = list
            }

        // Load Members
        ProjectRepository.getTrashedProjectMembers(projectUuid)
            .onSuccess { list ->
                trashedMembers = list
            }

        // Load Tags
        TagRepository.getTrashedTags(projectUuid)
            .onSuccess { list ->
                trashedTags = list
            }
    }

    // Actions: ORGANS
    fun restoreOrgan(projectUuid: String, organUuid: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            OrganRepository.restoreOrgan(projectUuid, organUuid)
                .onSuccess {
                    trashedOrgans = trashedOrgans.filter { it.uuid != organUuid }
                    onSuccess()
                }.onFailure { e ->
                    onError(e.message ?: "Échec de la restauration de l'Organ.")
                }
        }
    }

    fun deleteOrganPermanently(projectUuid: String, organUuid: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            OrganRepository.deleteOrgan(projectUuid, organUuid, permanent = true)
                .onSuccess {
                    trashedOrgans = trashedOrgans.filter { it.uuid != organUuid }
                    onSuccess()
                }.onFailure { e ->
                    onError(e.message ?: "Échec de la suppression définitive.")
                }
        }
    }

    // Actions: MEMBERS
    fun restoreMember(projectUuid: String, memberUuid: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            ProjectRepository.restoreMember(projectUuid, memberUuid)
                .onSuccess {
                    trashedMembers = trashedMembers.filter { it.uuid != memberUuid }
                    selectedMembers = selectedMembers - memberUuid
                    onSuccess()
                }.onFailure { e ->
                    onError(e.message ?: "Échec de la restauration du membre.")
                }
        }
    }

    fun deleteMemberPermanently(projectUuid: String, memberUuid: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            ProjectRepository.removeMember(projectUuid, memberUuid, permanent = true)
                .onSuccess {
                    trashedMembers = trashedMembers.filter { it.uuid != memberUuid }
                    selectedMembers = selectedMembers - memberUuid
                    onSuccess()
                }.onFailure { e ->
                    onError(e.message ?: "Échec de la suppression définitive.")
                }
        }
    }

    fun restoreSelectedMembers(projectUuid: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val uuidsToRestore = selectedMembers.toList()
        if (uuidsToRestore.isEmpty()) return

        viewModelScope.launch {
            var successCount = 0
            var failCount = 0
            for (uuid in uuidsToRestore) {
                ProjectRepository.restoreMember(projectUuid, uuid)
                    .onSuccess {
                        successCount++
                        trashedMembers = trashedMembers.filter { it.uuid != uuid }
                    }.onFailure {
                        failCount++
                    }
            }
            selectedMembers = emptySet()
            if (failCount > 0) {
                onError("Restauration terminée: $successCount membre(s) restauré(s), $failCount échec(s).")
            } else {
                onSuccess()
            }
        }
    }

    // Actions: TAGS
    fun restoreTag(projectUuid: String, tagUuid: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            TagRepository.restoreTag(projectUuid, tagUuid)
                .onSuccess {
                    trashedTags = trashedTags.filter { it.uuid != tagUuid }
                    onSuccess()
                }.onFailure { e ->
                    onError(e.message ?: "Échec de la restauration du tag.")
                }
        }
    }

    fun deleteTagPermanently(projectUuid: String, tagUuid: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            TagRepository.deleteTag(projectUuid, tagUuid, permanent = true)
                .onSuccess {
                    trashedTags = trashedTags.filter { it.uuid != tagUuid }
                    onSuccess()
                }.onFailure { e ->
                    onError(e.message ?: "Échec de la suppression définitive.")
                }
        }
    }
}
