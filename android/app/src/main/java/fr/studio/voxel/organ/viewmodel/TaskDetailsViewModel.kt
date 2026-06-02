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
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

class TaskDetailsViewModel : ViewModel() {

    private val taskService = ApiClient.createService(TaskApiService::class.java)
    private val projectService = ApiClient.createService(ProjectApiService::class.java)
    private val tagService = ApiClient.createService(TagApiService::class.java)

    var projectUuid by mutableStateOf<String?>(null)
        private set
    var organUuid by mutableStateOf<String?>(null)
        private set
    var taskUuid by mutableStateOf<String?>(null)
        private set

    // Task data and states
    var taskData by mutableStateOf<Task?>(null)
        private set
    var isLoading by mutableStateOf(false)
        private set
    var isSaving by mutableStateOf(false)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set
    var isSuccess by mutableStateOf(false)

    // Form inputs
    var title by mutableStateOf("")
    var description by mutableStateOf("")
    var status by mutableStateOf("TODO")
    var priority by mutableStateOf(1)
    var statusMessage by mutableStateOf("")
    var startDate by mutableStateOf("")
    var expiresAt by mutableStateOf("")
    var managerUuid by mutableStateOf("")
    var estimatedHours by mutableStateOf("")

    // Sub-resources
    var projectMembers by mutableStateOf<List<ProjectMember>>(emptyList())
        private set
    var assignees by mutableStateOf<List<User>>(emptyList())
        private set
    var comments by mutableStateOf<List<TaskCommentResponse>>(emptyList())
        private set
    var attachments by mutableStateOf<List<TaskAttachmentResponse>>(emptyList())
        private set
    var links by mutableStateOf<List<TaskLinkSummary>>(emptyList())
        private set
    var dependencies by mutableStateOf<List<TaskDependencyResponse>>(emptyList())
        private set
    var timeline by mutableStateOf<List<TaskTimelineItem>>(emptyList())
        private set

    // Trashed Sub-resources (for the corbeille/trash tab)
    var trashedComments by mutableStateOf<List<TaskCommentResponse>>(emptyList())
        private set
    var trashedAttachments by mutableStateOf<List<TaskAttachmentResponse>>(emptyList())
        private set
    var isTrashOpen by mutableStateOf(false)

    // Selection lists
    var allProjectTags by mutableStateOf<List<TagResponse>>(emptyList())
        private set
    var organTasks by mutableStateOf<List<Task>>(emptyList())
        private set

    // Permissions
    var currentPermissions by mutableStateOf<TaskPermissionsResponse?>(null)
        private set

    fun initTask(projUuid: String, orgUuid: String, tUuid: String) {
        if (projectUuid == projUuid && organUuid == orgUuid && taskUuid == tUuid && taskData != null) return
        projectUuid = projUuid
        organUuid = orgUuid
        taskUuid = tUuid
        isSuccess = false
        loadTaskDetails()
    }

    fun loadTaskDetails() {
        val pUuid = projectUuid ?: return
        val oUuid = organUuid ?: return
        val tUuid = taskUuid ?: return

        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            try {
                // Fetch members & project tags
                val membersRes = projectService.getProjectMembers(pUuid)
                if (membersRes.isSuccessful) {
                    projectMembers = membersRes.body() ?: emptyList()
                }

                val tagsRes = tagService.getTags(pUuid)
                if (tagsRes.isSuccessful) {
                    allProjectTags = tagsRes.body() ?: emptyList()
                }

                // Fetch other organ tasks for dependencies
                val tasksRes = taskService.getTasks(pUuid, oUuid)
                if (tasksRes.isSuccessful) {
                    organTasks = (tasksRes.body() ?: emptyList()).filter { it.uuid != tUuid }
                }

                // Fetch task details
                val taskRes = taskService.getTask(pUuid, oUuid, tUuid)
                if (taskRes.isSuccessful) {
                    val task = taskRes.body()
                    if (task != null) {
                        taskData = task
                        title = task.title
                        description = task.description ?: ""
                        status = task.status
                        priority = task.priority
                        statusMessage = task.statusMessage ?: ""
                        estimatedHours = task.estimatedHours ?: ""
                        startDate = task.startDate?.take(16)?.replace("T", " ") ?: ""
                        expiresAt = task.expiresAt?.take(16)?.replace("T", " ") ?: ""
                        managerUuid = task.manager?.uuid ?: ""
                        assignees = task.assignees ?: emptyList()
                        links = task.links ?: emptyList()
                    }
                }

                // Fetch permissions
                val permsRes = taskService.getTaskPermissions(pUuid, oUuid, tUuid)
                if (permsRes.isSuccessful) {
                    currentPermissions = permsRes.body()
                }

                // Fetch timeline, comments, attachments, dependencies
                loadComments()
                loadAttachments()
                loadDependencies()
                loadTimeline()
                if (isTrashOpen) {
                    loadTrashData()
                }
            } catch (e: Exception) {
                errorMessage = "Erreur réseau : ${e.localizedMessage}"
                Log.e("TASK_DETAILS_VM", "loadTaskDetails error", e)
            } finally {
                isLoading = false
            }
        }
    }

    // Sub-resources reloading
    fun loadComments() {
        val pUuid = projectUuid ?: return
        val oUuid = organUuid ?: return
        val tUuid = taskUuid ?: return
        viewModelScope.launch {
            try {
                val res = taskService.getComments(pUuid, oUuid, tUuid)
                if (res.isSuccessful) {
                    comments = res.body() ?: emptyList()
                }
            } catch (e: Exception) {
                Log.e("TASK_DETAILS_VM", "loadComments error", e)
            }
        }
    }

    fun loadAttachments() {
        val pUuid = projectUuid ?: return
        val oUuid = organUuid ?: return
        val tUuid = taskUuid ?: return
        viewModelScope.launch {
            try {
                val res = taskService.getAttachments(pUuid, oUuid, tUuid)
                if (res.isSuccessful) {
                    attachments = res.body() ?: emptyList()
                }
            } catch (e: Exception) {
                Log.e("TASK_DETAILS_VM", "loadAttachments error", e)
            }
        }
    }

    fun loadDependencies() {
        val pUuid = projectUuid ?: return
        val oUuid = organUuid ?: return
        val tUuid = taskUuid ?: return
        viewModelScope.launch {
            try {
                val res = taskService.getDependencies(pUuid, oUuid, tUuid)
                if (res.isSuccessful) {
                    dependencies = res.body() ?: emptyList()
                }
            } catch (e: Exception) {
                Log.e("TASK_DETAILS_VM", "loadDependencies error", e)
            }
        }
    }

    fun loadTimeline() {
        val pUuid = projectUuid ?: return
        val oUuid = organUuid ?: return
        val tUuid = taskUuid ?: return
        viewModelScope.launch {
            try {
                val res = taskService.getTaskTimeline(pUuid, oUuid, tUuid, 0, 30)
                if (res.isSuccessful) {
                    timeline = (res.body() ?: emptyList()).filter { it.actionType?.uppercase() != "CONSULTATION" }
                }
            } catch (e: Exception) {
                Log.e("TASK_DETAILS_VM", "loadTimeline error", e)
            }
        }
    }

    fun loadTrashData() {
        val pUuid = projectUuid ?: return
        val oUuid = organUuid ?: return
        val tUuid = taskUuid ?: return
        viewModelScope.launch {
            try {
                val cRes = taskService.getTrashedComments(pUuid, oUuid, tUuid)
                if (cRes.isSuccessful) {
                    trashedComments = cRes.body() ?: emptyList()
                }
                val aRes = taskService.getTrashedAttachments(pUuid, oUuid, tUuid)
                if (aRes.isSuccessful) {
                    trashedAttachments = aRes.body() ?: emptyList()
                }
            } catch (e: Exception) {
                Log.e("TASK_DETAILS_VM", "loadTrashData error", e)
            }
        }
    }

    fun toggleTrash() {
        isTrashOpen = !isTrashOpen
        if (isTrashOpen) {
            loadTrashData()
        }
    }

    // Permissions check
    fun hasPerm(permBaseName: String, isOwner: Boolean = false): Boolean {
        val perms = currentPermissions?.permissions ?: return false
        if (currentPermissions?.isProjectAdmin == true || perms.contains("ALL")) return true
        if (perms.contains(permBaseName)) return true
        if (perms.contains("${permBaseName}_ALL")) return true
        if (isOwner && perms.contains("${permBaseName}_OWN")) return true
        return false
    }

    fun isTaskOwner(): Boolean {
        val ownership = currentPermissions?.taskOwnership ?: return false
        return ownership.isManager || ownership.isCreator
    }

    fun canEditField(fieldName: String): Boolean {
        if (taskData?.deletedAt != null) return false
        var check = fieldName
        if (fieldName == "startDate" || fieldName == "expiresAt") check = "expiresAt"
        if (fieldName == "managerUuid") check = "manager"
        return currentPermissions?.editableFields?.contains(check) ?: false
    }

    // Save, Delete, Restore
    fun saveTask() {
        val pUuid = projectUuid ?: return
        val oUuid = organUuid ?: return
        val tUuid = taskUuid ?: return

        if (title.isBlank()) {
            errorMessage = "Le titre est requis."
            return
        }

        viewModelScope.launch {
            isSaving = true
            errorMessage = null
            try {
                val formattedStart = if (startDate.isNotBlank()) startDate.trim().replace(" ", "T") + ":00" else null
                val formattedExpires = if (expiresAt.isNotBlank()) expiresAt.trim().replace(" ", "T") + ":00" else null

                val updatedTask = Task(
                    id = taskData?.id,
                    uuid = tUuid,
                    organId = taskData?.organId,
                    createdBy = taskData?.createdBy,
                    manager = if (managerUuid.isNotBlank()) projectMembers.find { it.user.uuid == managerUuid }?.user?.let {
                        User(it.uuid, it.email, it.firstName, it.lastName)
                    } else null,
                    title = title,
                    description = if (description.isNotBlank()) description else null,
                    priority = priority,
                    status = status,
                    estimatedHours = if (estimatedHours.isNotBlank()) estimatedHours else null,
                    startDate = formattedStart,
                    expiresAt = formattedExpires,
                    createdAt = taskData?.createdAt,
                    updatedAt = taskData?.updatedAt,
                    validedAt = taskData?.validedAt,
                    deletedAt = taskData?.deletedAt,
                    projectName = taskData?.projectName,
                    organName = taskData?.organName,
                    projectUuid = pUuid,
                    organUuid = oUuid,
                    statusMessage = if (statusMessage.isNotBlank() && status != taskData?.status) statusMessage else null
                )

                val response = taskService.updateTask(pUuid, oUuid, tUuid, updatedTask)
                if (response.isSuccessful) {
                    isSuccess = true
                } else {
                    errorMessage = "Erreur lors de la sauvegarde : ${response.code()}"
                }
            } catch (e: Exception) {
                errorMessage = "Erreur réseau : ${e.localizedMessage}"
                Log.e("TASK_DETAILS_VM", "saveTask error", e)
            } finally {
                isSaving = false
            }
        }
    }

    fun deleteTask(permanent: Boolean = false) {
        val pUuid = projectUuid ?: return
        val oUuid = organUuid ?: return
        val tUuid = taskUuid ?: return

        viewModelScope.launch {
            isSaving = true
            errorMessage = null
            try {
                val response = taskService.deleteTask(pUuid, oUuid, tUuid, permanent)
                if (response.isSuccessful) {
                    isSuccess = true
                } else {
                    errorMessage = "Erreur lors de la suppression : ${response.code()}"
                }
            } catch (e: Exception) {
                errorMessage = "Erreur réseau : ${e.localizedMessage}"
                Log.e("TASK_DETAILS_VM", "deleteTask error", e)
            } finally {
                isSaving = false
            }
        }
    }

    fun restoreTask() {
        val pUuid = projectUuid ?: return
        val oUuid = organUuid ?: return
        val tUuid = taskUuid ?: return

        viewModelScope.launch {
            isSaving = true
            errorMessage = null
            try {
                val response = taskService.restoreTask(pUuid, oUuid, tUuid)
                if (response.isSuccessful) {
                    loadTaskDetails()
                } else {
                    errorMessage = "Erreur de restauration : ${response.code()}"
                }
            } catch (e: Exception) {
                errorMessage = "Erreur réseau : ${e.localizedMessage}"
                Log.e("TASK_DETAILS_VM", "restoreTask error", e)
            } finally {
                isSaving = false
            }
        }
    }

    // Sub-resources Actions
    fun addAssignee(userUuid: String) {
        val pUuid = projectUuid ?: return
        val oUuid = organUuid ?: return
        val tUuid = taskUuid ?: return
        viewModelScope.launch {
            try {
                val response = taskService.addAssignee(pUuid, oUuid, tUuid, AssigneeRequest(userUuid))
                if (response.isSuccessful) {
                    loadTaskDetails()
                }
            } catch (e: Exception) {
                Log.e("TASK_DETAILS_VM", "addAssignee error", e)
            }
        }
    }

    fun removeAssignee(userUuid: String) {
        val pUuid = projectUuid ?: return
        val oUuid = organUuid ?: return
        val tUuid = taskUuid ?: return
        viewModelScope.launch {
            try {
                val response = taskService.removeAssignee(pUuid, oUuid, tUuid, userUuid)
                if (response.isSuccessful) {
                    loadTaskDetails()
                }
            } catch (e: Exception) {
                Log.e("TASK_DETAILS_VM", "removeAssignee error", e)
            }
        }
    }

    fun addComment(content: String) {
        val pUuid = projectUuid ?: return
        val oUuid = organUuid ?: return
        val tUuid = taskUuid ?: return
        if (content.isBlank()) return
        viewModelScope.launch {
            try {
                val response = taskService.createComment(pUuid, oUuid, tUuid, CreateCommentRequest(content))
                if (response.isSuccessful) {
                    loadComments()
                    loadTimeline()
                }
            } catch (e: Exception) {
                Log.e("TASK_DETAILS_VM", "addComment error", e)
            }
        }
    }

    fun deleteComment(commentUuid: String, permanent: Boolean = false) {
        val pUuid = projectUuid ?: return
        val oUuid = organUuid ?: return
        val tUuid = taskUuid ?: return
        viewModelScope.launch {
            try {
                val response = taskService.deleteComment(pUuid, oUuid, tUuid, commentUuid, permanent)
                if (response.isSuccessful) {
                    loadComments()
                    loadTimeline()
                    if (isTrashOpen) {
                        loadTrashData()
                    }
                }
            } catch (e: Exception) {
                Log.e("TASK_DETAILS_VM", "deleteComment error", e)
            }
        }
    }

    fun restoreComment(commentUuid: String) {
        val pUuid = projectUuid ?: return
        val oUuid = organUuid ?: return
        val tUuid = taskUuid ?: return
        viewModelScope.launch {
            try {
                val response = taskService.restoreComment(pUuid, oUuid, tUuid, commentUuid)
                if (response.isSuccessful) {
                    loadComments()
                    loadTimeline()
                    loadTrashData()
                }
            } catch (e: Exception) {
                Log.e("TASK_DETAILS_VM", "restoreComment error", e)
            }
        }
    }

    fun uploadAttachment(fileName: String, mimeType: String, fileBytes: ByteArray) {
        val pUuid = projectUuid ?: return
        val oUuid = organUuid ?: return
        val tUuid = taskUuid ?: return
        viewModelScope.launch {
            try {
                val requestBody = fileBytes.toRequestBody(mimeType.toMediaTypeOrNull())
                val body = MultipartBody.Part.createFormData("file", fileName, requestBody)
                val response = taskService.uploadAttachment(pUuid, oUuid, tUuid, body)
                if (response.isSuccessful) {
                    loadAttachments()
                    loadTimeline()
                }
            } catch (e: Exception) {
                Log.e("TASK_DETAILS_VM", "uploadAttachment error", e)
            }
        }
    }

    fun deleteAttachment(attachmentUuid: String, permanent: Boolean = false) {
        val pUuid = projectUuid ?: return
        val oUuid = organUuid ?: return
        val tUuid = taskUuid ?: return
        viewModelScope.launch {
            try {
                val response = taskService.deleteAttachment(pUuid, oUuid, tUuid, attachmentUuid, permanent)
                if (response.isSuccessful) {
                    loadAttachments()
                    loadTimeline()
                    if (isTrashOpen) {
                        loadTrashData()
                    }
                }
            } catch (e: Exception) {
                Log.e("TASK_DETAILS_VM", "deleteAttachment error", e)
            }
        }
    }

    fun restoreAttachment(attachmentUuid: String) {
        val pUuid = projectUuid ?: return
        val oUuid = organUuid ?: return
        val tUuid = taskUuid ?: return
        viewModelScope.launch {
            try {
                val response = taskService.restoreAttachment(pUuid, oUuid, tUuid, attachmentUuid)
                if (response.isSuccessful) {
                    loadAttachments()
                    loadTimeline()
                    loadTrashData()
                }
            } catch (e: Exception) {
                Log.e("TASK_DETAILS_VM", "restoreAttachment error", e)
            }
        }
    }

    fun addLink(url: String, description: String?) {
        val pUuid = projectUuid ?: return
        val oUuid = organUuid ?: return
        val tUuid = taskUuid ?: return
        if (url.isBlank()) return
        viewModelScope.launch {
            try {
                val response = taskService.createLink(pUuid, oUuid, tUuid, AddLinkRequest(url, description))
                if (response.isSuccessful) {
                    loadTaskDetails()
                    loadTimeline()
                }
            } catch (e: Exception) {
                Log.e("TASK_DETAILS_VM", "addLink error", e)
            }
        }
    }

    fun deleteLink(linkUuid: String) {
        val pUuid = projectUuid ?: return
        val oUuid = organUuid ?: return
        val tUuid = taskUuid ?: return
        viewModelScope.launch {
            try {
                val response = taskService.deleteLink(pUuid, oUuid, tUuid, linkUuid)
                if (response.isSuccessful) {
                    loadTaskDetails()
                    loadTimeline()
                }
            } catch (e: Exception) {
                Log.e("TASK_DETAILS_VM", "deleteLink error", e)
            }
        }
    }

    fun addTag(tagUuid: String) {
        val pUuid = projectUuid ?: return
        val oUuid = organUuid ?: return
        val tUuid = taskUuid ?: return
        viewModelScope.launch {
            try {
                val response = taskService.addTaskTag(pUuid, oUuid, tUuid, AddTagRequest(tagUuid))
                if (response.isSuccessful) {
                    loadTaskDetails()
                    loadTimeline()
                }
            } catch (e: Exception) {
                Log.e("TASK_DETAILS_VM", "addTag error", e)
            }
        }
    }

    fun removeTag(tagUuid: String) {
        val pUuid = projectUuid ?: return
        val oUuid = organUuid ?: return
        val tUuid = taskUuid ?: return
        viewModelScope.launch {
            try {
                val response = taskService.removeTaskTag(pUuid, oUuid, tUuid, tagUuid)
                if (response.isSuccessful) {
                    loadTaskDetails()
                    loadTimeline()
                }
            } catch (e: Exception) {
                Log.e("TASK_DETAILS_VM", "removeTag error", e)
            }
        }
    }

    fun addDependency(dependsOnUuid: String) {
        val pUuid = projectUuid ?: return
        val oUuid = organUuid ?: return
        val tUuid = taskUuid ?: return
        viewModelScope.launch {
            try {
                val response = taskService.addDependency(pUuid, oUuid, tUuid, AddDependencyRequest(dependsOnUuid))
                if (response.isSuccessful) {
                    loadDependencies()
                    loadTimeline()
                }
            } catch (e: Exception) {
                Log.e("TASK_DETAILS_VM", "addDependency error", e)
            }
        }
    }

    fun removeDependency(dependsOnUuid: String, permanent: Boolean = false) {
        val pUuid = projectUuid ?: return
        val oUuid = organUuid ?: return
        val tUuid = taskUuid ?: return
        viewModelScope.launch {
            try {
                val response = taskService.removeDependency(pUuid, oUuid, tUuid, dependsOnUuid, permanent)
                if (response.isSuccessful) {
                    loadDependencies()
                    loadTimeline()
                }
            } catch (e: Exception) {
                Log.e("TASK_DETAILS_VM", "removeDependency error", e)
            }
        }
    }
}
