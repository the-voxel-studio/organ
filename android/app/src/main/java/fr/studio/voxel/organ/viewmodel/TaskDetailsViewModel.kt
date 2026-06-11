package fr.studio.voxel.organ.viewmodel

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.studio.voxel.organ.viewmodel.handler.*
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
    private val organService = ApiClient.createService(OrganApiService::class.java)

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
    val commentHandler = TaskCommentHandler(taskService, viewModelScope)
    val attachmentHandler = TaskAttachmentHandler(taskService, viewModelScope)
    val dependencyHandler = TaskDependencyHandler(taskService, viewModelScope)
    val assigneeHandler = TaskAssigneeHandler(taskService, viewModelScope)
    val tagHandler = TaskTagHandler(taskService, tagService, viewModelScope)
    val linkHandler = TaskLinkHandler(taskService, viewModelScope)

    val comments: List<TaskCommentResponse> get() = commentHandler.comments
    val trashedComments: List<TaskCommentResponse> get() = commentHandler.trashedComments

    val attachments: List<TaskAttachmentResponse> get() = attachmentHandler.attachments
    val trashedAttachments: List<TaskAttachmentResponse> get() = attachmentHandler.trashedAttachments

    val dependencies: List<TaskDependencyResponse> get() = dependencyHandler.dependencies
    val assignees: List<User> get() = assigneeHandler.assignees
    val links: List<TaskLinkSummary> get() = linkHandler.links
    val allProjectTags: List<TagResponse> get() = tagHandler.allProjectTags

    var timeline by mutableStateOf<List<TaskTimelineItem>>(emptyList())
        private set

    var isTrashOpen by mutableStateOf(false)

    // Selection lists
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
                // Fetch project members
                val membersRes = projectService.getProjectMembers(pUuid)
                var allMembers = emptyList<ProjectMember>()
                if (membersRes.isSuccessful) {
                    allMembers = membersRes.body() ?: emptyList()
                }

                // Fetch organ roles to get members belonging to this organ
                val rolesRes = organService.getOrganRoles(pUuid, oUuid)
                if (rolesRes.isSuccessful) {
                    val rolesList = rolesRes.body() ?: emptyList()
                    val organMemberUuids = rolesList.flatMap { role ->
                        role.members?.map { it.uuid } ?: emptyList()
                    }.toSet()

                    // Only keep members who are in the organ
                    projectMembers = allMembers.filter { pm ->
                        organMemberUuids.contains(pm.user.uuid)
                    }
                } else {
                    projectMembers = emptyList()
                }

                tagHandler.loadTags(pUuid)

                if (tUuid == "new") {
                    // Reset form fields to defaults for new task
                    taskData = null
                    title = ""
                    description = ""
                    status = "TODO"
                    priority = 1
                    statusMessage = ""
                    estimatedHours = ""
                    startDate = ""
                    expiresAt = ""
                    managerUuid = ""
                    assigneeHandler.assignees = emptyList()
                    linkHandler.links = emptyList()
                    commentHandler.comments = emptyList()
                    attachmentHandler.attachments = emptyList()
                    dependencyHandler.dependencies = emptyList()
                    timeline = emptyList()
                    commentHandler.trashedComments = emptyList()
                    attachmentHandler.trashedAttachments = emptyList()
                    currentPermissions = null
                    return@launch
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
                        assigneeHandler.assignees = task.assignees ?: emptyList()
                        linkHandler.links = task.links ?: emptyList()
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
        commentHandler.loadComments(pUuid, oUuid, tUuid)
    }

    fun loadAttachments() {
        val pUuid = projectUuid ?: return
        val oUuid = organUuid ?: return
        val tUuid = taskUuid ?: return
        attachmentHandler.loadAttachments(pUuid, oUuid, tUuid)
    }

    fun loadDependencies() {
        val pUuid = projectUuid ?: return
        val oUuid = organUuid ?: return
        val tUuid = taskUuid ?: return
        dependencyHandler.loadDependencies(pUuid, oUuid, tUuid)
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
        commentHandler.loadTrashData(pUuid, oUuid, tUuid)
        attachmentHandler.loadTrashData(pUuid, oUuid, tUuid)
    }

    fun toggleTrash() {
        isTrashOpen = !isTrashOpen
        if (isTrashOpen) {
            loadTrashData()
        }
    }

    // Permissions check
    fun hasPerm(permBaseName: String, isOwner: Boolean = false): Boolean {
        if (taskUuid == "new") {
            return permBaseName == "TASK_CREATE" || permBaseName == "TASK_EDIT"
        }
        val perms = currentPermissions?.permissions ?: return false
        if (currentPermissions?.isProjectAdmin == true || perms.contains("ALL")) return true
        if (perms.contains(permBaseName)) return true
        if (perms.contains("${permBaseName}_ALL")) return true
        if (isOwner && perms.contains("${permBaseName}_OWN")) return true
        return false
    }

    fun isTaskOwner(): Boolean {
        if (taskUuid == "new") return true
        val ownership = currentPermissions?.taskOwnership ?: return false
        return ownership.isManager || ownership.isCreator
    }

    fun canEditField(fieldName: String): Boolean {
        if (taskUuid == "new") return true
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

        if (estimatedHours.isNotBlank()) {
            val hours = estimatedHours.toDoubleOrNull()
            if (hours == null) {
                errorMessage = "Le temps estimé doit être un nombre valide."
                return
            }
            if (hours < 0) {
                errorMessage = "Le temps estimé ne peut pas être négatif."
                return
            }
            if (hours > 99999999.99) {
                errorMessage = "Le temps estimé ne peut pas dépasser 99 999 999.99 heures."
                return
            }
        }

        viewModelScope.launch {
            isSaving = true
            errorMessage = null
            try {
                val formattedStart = if (startDate.isNotBlank()) startDate.trim().replace(" ", "T") + ":00" else null
                val formattedExpires = if (expiresAt.isNotBlank()) expiresAt.trim().replace(" ", "T") + ":00" else null

                val updatedTask = Task(
                    id = taskData?.id,
                    uuid = if (tUuid == "new") "" else tUuid,
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

                val response = if (tUuid == "new") {
                    taskService.createTask(pUuid, oUuid, updatedTask)
                } else {
                    taskService.updateTask(pUuid, oUuid, tUuid, updatedTask)
                }

                if (response.isSuccessful) {
                    val savedTask = response.body()
                    if (tUuid == "new" && savedTask != null) {
                        taskUuid = savedTask.uuid
                        loadTaskDetails()
                    } else {
                        isSuccess = true
                    }
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

    fun addAssignee(userUuid: String) {
        val pUuid = projectUuid ?: return
        val oUuid = organUuid ?: return
        val tUuid = taskUuid ?: return
        assigneeHandler.addAssignee(pUuid, oUuid, tUuid, userUuid) {
            loadTaskDetails()
        }
    }

    fun removeAssignee(userUuid: String) {
        val pUuid = projectUuid ?: return
        val oUuid = organUuid ?: return
        val tUuid = taskUuid ?: return
        assigneeHandler.removeAssignee(pUuid, oUuid, tUuid, userUuid) {
            loadTaskDetails()
        }
    }

    fun addComment(content: String) {
        val pUuid = projectUuid ?: return
        val oUuid = organUuid ?: return
        val tUuid = taskUuid ?: return
        commentHandler.addComment(pUuid, oUuid, tUuid, content) { loadTimeline() }
    }

    fun deleteComment(commentUuid: String, permanent: Boolean = false) {
        val pUuid = projectUuid ?: return
        val oUuid = organUuid ?: return
        val tUuid = taskUuid ?: return
        commentHandler.deleteComment(pUuid, oUuid, tUuid, commentUuid, permanent, isTrashOpen) { loadTimeline() }
    }

    fun restoreComment(commentUuid: String) {
        val pUuid = projectUuid ?: return
        val oUuid = organUuid ?: return
        val tUuid = taskUuid ?: return
        commentHandler.restoreComment(pUuid, oUuid, tUuid, commentUuid) { loadTimeline() }
    }

    fun uploadAttachment(fileName: String, mimeType: String, fileBytes: ByteArray) {
        val pUuid = projectUuid ?: return
        val oUuid = organUuid ?: return
        val tUuid = taskUuid ?: return
        attachmentHandler.uploadAttachment(pUuid, oUuid, tUuid, fileName, mimeType, fileBytes) { loadTimeline() }
    }

    fun deleteAttachment(attachmentUuid: String, permanent: Boolean = false) {
        val pUuid = projectUuid ?: return
        val oUuid = organUuid ?: return
        val tUuid = taskUuid ?: return
        attachmentHandler.deleteAttachment(pUuid, oUuid, tUuid, attachmentUuid, permanent, isTrashOpen) { loadTimeline() }
    }

    fun restoreAttachment(attachmentUuid: String) {
        val pUuid = projectUuid ?: return
        val oUuid = organUuid ?: return
        val tUuid = taskUuid ?: return
        attachmentHandler.restoreAttachment(pUuid, oUuid, tUuid, attachmentUuid) { loadTimeline() }
    }

    fun addLink(url: String, description: String?) {
        val pUuid = projectUuid ?: return
        val oUuid = organUuid ?: return
        val tUuid = taskUuid ?: return
        linkHandler.addLink(pUuid, oUuid, tUuid, url, description) {
            loadTaskDetails()
            loadTimeline()
        }
    }

    fun deleteLink(linkUuid: String) {
        val pUuid = projectUuid ?: return
        val oUuid = organUuid ?: return
        val tUuid = taskUuid ?: return
        linkHandler.deleteLink(pUuid, oUuid, tUuid, linkUuid) {
            loadTaskDetails()
            loadTimeline()
        }
    }

    fun addTag(tagUuid: String) {
        val pUuid = projectUuid ?: return
        val oUuid = organUuid ?: return
        val tUuid = taskUuid ?: return
        tagHandler.addTag(pUuid, oUuid, tUuid, tagUuid) {
            loadTaskDetails()
            loadTimeline()
        }
    }

    fun removeTag(tagUuid: String) {
        val pUuid = projectUuid ?: return
        val oUuid = organUuid ?: return
        val tUuid = taskUuid ?: return
        tagHandler.removeTag(pUuid, oUuid, tUuid, tagUuid) {
            loadTaskDetails()
            loadTimeline()
        }
    }

    fun addDependency(dependsOnUuid: String) {
        val pUuid = projectUuid ?: return
        val oUuid = organUuid ?: return
        val tUuid = taskUuid ?: return
        dependencyHandler.addDependency(pUuid, oUuid, tUuid, dependsOnUuid) { loadTimeline() }
    }

    fun removeDependency(dependsOnUuid: String, permanent: Boolean = false) {
        val pUuid = projectUuid ?: return
        val oUuid = organUuid ?: return
        val tUuid = taskUuid ?: return
        dependencyHandler.removeDependency(pUuid, oUuid, tUuid, dependsOnUuid, permanent) { loadTimeline() }
    }
}
