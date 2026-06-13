package fr.studio.voxel.organ.viewmodel

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.studio.voxel.organ.data.ProjectStatsRepository
import fr.studio.voxel.organ.data.TagRepository
import fr.studio.voxel.organ.network.services.ProjectDetailedViewResponse
import fr.studio.voxel.organ.network.services.TagResponse
import kotlinx.coroutines.launch
import fr.studio.voxel.organ.network.getErrorMessageForCode

class ProjectDetailsViewModel : ViewModel() {

    var projectUuid by mutableStateOf<String?>(null)
        private set

    var projectData by mutableStateOf<ProjectDetailedViewResponse?>(null)
        private set

    var isLoading by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    var isAccessDenied by mutableStateOf(false)
        private set

    // Tags states
    var showTagsPanel by mutableStateOf(false)
    var tags by mutableStateOf<List<TagResponse>>(emptyList())
    var isTagsLoading by mutableStateOf(false)
        private set
    var isSavingTag by mutableStateOf(false)
        private set
    var tagErrorMessage by mutableStateOf<String?>(null)

    fun initProject(uuid: String) {
        if (projectUuid == uuid && projectData != null) return // Already loaded
        projectUuid = uuid
        loadProjectDetails(uuid)
    }

    fun loadProjectDetails(uuid: String) {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            isAccessDenied = false
            try {
                val response = ProjectStatsRepository.getDetailedProject(uuid)
                if (response.isSuccessful) {
                    projectData = response.body()
                    if (showTagsPanel) {
                        loadTags()
                    }
                } else {
                    val errorBody = response.errorBody()?.string() ?: ""
                    if (response.code() == 403 || errorBody.contains("Access denied", ignoreCase = true)) {
                        isAccessDenied = true
                    } else {
                        errorMessage = getErrorMessageForCode(response.code())
                    }
                }
            } catch (e: Exception) {
                errorMessage = "Erreur réseau: ${e.localizedMessage}"
                Log.e("PROJECT_DETAILS_VM", "loadProjectDetails error", e)
            } finally {
                isLoading = false
            }
        }
    }

    fun toggleTagsPanel() {
        showTagsPanel = !showTagsPanel
        if (showTagsPanel) {
            loadTags()
        }
    }

    fun loadTags() {
        val uuid = projectUuid ?: return
        viewModelScope.launch {
            isTagsLoading = true
            tagErrorMessage = null
            TagRepository.getTags(uuid)
                .onSuccess { list ->
                    tags = list
                }.onFailure { e ->
                    tagErrorMessage = e.message ?: "Erreur réseau"
                    Log.e("PROJECT_DETAILS_VM", "loadTags error", e)
                }
            isTagsLoading = false
        }
    }

    fun createTag(name: String, color: String) {
        val uuid = projectUuid ?: return
        viewModelScope.launch {
            isSavingTag = true
            tagErrorMessage = null
            TagRepository.createTag(uuid, name, color)
                .onSuccess {
                    loadTags()
                }.onFailure { e ->
                    tagErrorMessage = e.message ?: "Erreur réseau"
                    Log.e("PROJECT_DETAILS_VM", "createTag error", e)
                }
            isSavingTag = false
        }
    }

    fun updateTag(tagUuid: String, name: String, color: String) {
        val uuid = projectUuid ?: return
        viewModelScope.launch {
            isSavingTag = true
            tagErrorMessage = null
            TagRepository.updateTag(uuid, tagUuid, name, color)
                .onSuccess {
                    tags = tags.map { tag ->
                        if (tag.uuid == tagUuid) {
                            tag.copy(name = name, color = color)
                        } else {
                            tag
                        }
                    }
                    loadTags()
                }.onFailure { e ->
                    tagErrorMessage = e.message ?: "Erreur réseau"
                    Log.e("PROJECT_DETAILS_VM", "updateTag error", e)
                }
            isSavingTag = false
        }
    }

    fun deleteTag(tagUuid: String) {
        val uuid = projectUuid ?: return
        viewModelScope.launch {
            isTagsLoading = true
            tagErrorMessage = null
            TagRepository.deleteTag(uuid, tagUuid, permanent = false)
                .onSuccess {
                    loadTags()
                }.onFailure { e ->
                    tagErrorMessage = e.message ?: "Erreur réseau"
                    Log.e("PROJECT_DETAILS_VM", "deleteTag error", e)
                }
            isTagsLoading = false
        }
    }
}
