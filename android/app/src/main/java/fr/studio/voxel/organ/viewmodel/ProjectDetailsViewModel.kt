package fr.studio.voxel.organ.viewmodel

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.studio.voxel.organ.network.ApiClient
import fr.studio.voxel.organ.network.services.ProjectApiService
import fr.studio.voxel.organ.network.services.ProjectDetailedViewResponse
import fr.studio.voxel.organ.network.services.TagApiService
import fr.studio.voxel.organ.network.services.TagResponse
import fr.studio.voxel.organ.network.services.CreateTagRequest
import fr.studio.voxel.organ.network.services.UpdateTagRequest
import kotlinx.coroutines.launch

class ProjectDetailsViewModel : ViewModel() {

    private val projectService = ApiClient.createService(ProjectApiService::class.java)
    private val tagService = ApiClient.createService(TagApiService::class.java)

    var projectUuid by mutableStateOf<String?>(null)
        private set

    var projectData by mutableStateOf<ProjectDetailedViewResponse?>(null)
        private set

    var isLoading by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
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
            try {
                val response = projectService.getDetailedProject(uuid)
                if (response.isSuccessful) {
                    projectData = response.body()
                    if (showTagsPanel) {
                        loadTags()
                    }
                } else {
                    errorMessage = "Erreur lors du chargement: ${response.code()}"
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
            try {
                val response = tagService.getTags(uuid)
                if (response.isSuccessful) {
                    tags = response.body() ?: emptyList()
                } else {
                    tagErrorMessage = "Impossible de charger les tags: ${response.code()}"
                }
            } catch (e: Exception) {
                tagErrorMessage = "Erreur réseau: ${e.localizedMessage}"
                Log.e("PROJECT_DETAILS_VM", "loadTags error", e)
            } finally {
                isTagsLoading = false
            }
        }
    }

    fun createTag(name: String, color: String) {
        val uuid = projectUuid ?: return
        viewModelScope.launch {
            isSavingTag = true
            tagErrorMessage = null
            try {
                val response = tagService.createTag(uuid, CreateTagRequest(name, color))
                if (response.isSuccessful) {
                    loadTags()
                } else {
                    tagErrorMessage = "Erreur lors de la création du tag: ${response.code()}"
                }
            } catch (e: Exception) {
                tagErrorMessage = "Erreur réseau: ${e.localizedMessage}"
                Log.e("PROJECT_DETAILS_VM", "createTag error", e)
            } finally {
                isSavingTag = false
            }
        }
    }

    fun updateTag(tagUuid: String, name: String, color: String) {
        val uuid = projectUuid ?: return
        viewModelScope.launch {
            isSavingTag = true
            tagErrorMessage = null
            try {
                val response = tagService.updateTag(uuid, tagUuid, UpdateTagRequest(name, color))
                if (response.isSuccessful) {
                    tags = tags.map { tag ->
                        if (tag.uuid == tagUuid) {
                            tag.copy(name = name, color = color)
                        } else {
                            tag
                        }
                    }
                    loadTags()
                } else {
                    tagErrorMessage = "Erreur lors de la mise à jour: ${response.code()}"
                }
            } catch (e: Exception) {
                tagErrorMessage = "Erreur réseau: ${e.localizedMessage}"
                Log.e("PROJECT_DETAILS_VM", "updateTag error", e)
            } finally {
                isSavingTag = false
            }
        }
    }

    fun deleteTag(tagUuid: String) {
        val uuid = projectUuid ?: return
        viewModelScope.launch {
            isTagsLoading = true
            tagErrorMessage = null
            try {
                val response = tagService.deleteTag(uuid, tagUuid, permanent = true)
                if (response.isSuccessful) {
                    loadTags()
                } else {
                    tagErrorMessage = "Erreur lors de la suppression: ${response.code()}"
                }
            } catch (e: Exception) {
                tagErrorMessage = "Erreur réseau: ${e.localizedMessage}"
                Log.e("PROJECT_DETAILS_VM", "deleteTag error", e)
            } finally {
                isTagsLoading = false
            }
        }
    }
}
