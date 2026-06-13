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
import fr.studio.voxel.organ.data.UserRepository
import fr.studio.voxel.organ.network.services.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.isActive
import fr.studio.voxel.organ.network.getErrorMessageForCode

class OrganDetailsViewModel : ViewModel() {

    var projectUuid by mutableStateOf<String?>(null)
        private set
    var organUuid by mutableStateOf<String?>(null)
        private set

    // Data States
    var organData by mutableStateOf<Organ?>(null)
        private set
    var projectTitle by mutableStateOf("")
        private set
    var projectColor by mutableStateOf("#FF7DD4")
        private set
    var isProjectAdmin by mutableStateOf(false)
        private set
    var permissions by mutableStateOf<List<String>>(emptyList())
        private set
    private val _allTasks = mutableStateOf<List<Task>>(emptyList())
    var allTasks: List<Task>
        get() = _allTasks.value
        private set(value) {
            _allTasks.value = value
            updateFilteredTasks()
        }

    var isLoading by mutableStateOf(false)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set
    var isAccessDenied by mutableStateOf(false)
        private set

    // Links states
    var showLinksPanel by mutableStateOf(false)
    var links by mutableStateOf<List<OrganLinkSummary>>(emptyList())
        private set
    var isLinksLoading by mutableStateOf(false)
        private set
    var isSavingLink by mutableStateOf(false)
        private set
    var linkErrorMessage by mutableStateOf<String?>(null)

    // Filter & Sorting States
    private val _sortBy = mutableStateOf<String?>(null)
    var sortBy: String?
        get() = _sortBy.value
        set(value) {
            _sortBy.value = value
            updateFilteredTasks()
        }

    private val _sortOrder = mutableStateOf("asc")
    var sortOrder: String
        get() = _sortOrder.value
        set(value) {
            _sortOrder.value = value
            updateFilteredTasks()
        }

    private val _filterMe = mutableStateOf(false)
    var filterMe: Boolean
        get() = _filterMe.value
        set(value) {
            _filterMe.value = value
            updateFilteredTasks()
        }

    private val _minPriority = mutableStateOf(0)
    var minPriority: Int
        get() = _minPriority.value
        set(value) {
            _minPriority.value = value
            updateFilteredTasks()
        }

    private val _selectedStatuses = mutableStateOf<List<String>>(emptyList())
    var selectedStatuses: List<String>
        get() = _selectedStatuses.value
        set(value) {
            _selectedStatuses.value = value
            updateFilteredTasks()
        }

    // Active View Mode: "kanban" | "list"
    var activeView by mutableStateOf("kanban")

    // Has Permission check
    fun hasPermission(permissionName: String): Boolean {
        if (isProjectAdmin) return true
        return permissions.contains("ALL") || permissions.contains(permissionName)
    }

    // Filtered tasks backing state
    private val _filteredTasks = mutableStateOf<List<Task>>(emptyList())
    val filteredTasks: List<Task>
        get() = _filteredTasks.value

    private var filterJob: kotlinx.coroutines.Job? = null

    private fun updateFilteredTasks() {
        val tasksInput = allTasks
        val filterMeInput = filterMe
        val minPriorityInput = minPriority
        val selectedStatusesInput = selectedStatuses
        val sortByInput = sortBy
        val sortOrderInput = sortOrder

        filterJob?.cancel()
        filterJob = viewModelScope.launch(kotlinx.coroutines.Dispatchers.Default) {
            var list = tasksInput

            // 1. Filter by "me"
            if (filterMeInput) {
                val currentUserId = UserRepository.currentUser?.uuid
                if (currentUserId != null) {
                    list = list.filter { task ->
                        task.manager?.uuid == currentUserId ||
                                task.createdBy?.uuid == currentUserId
                    }
                }
            }

            // 2. Filter by minimum priority
            if (minPriorityInput > 0) {
                list = list.filter { it.priority >= minPriorityInput }
            }

            // 3. Filter by selected statuses
            if (selectedStatusesInput.isNotEmpty()) {
                list = list.filter { selectedStatusesInput.contains(it.status) }
            }

            // 4. Special filter for Due Date sorting (exclude null due dates if sorting by due date)
            if (sortByInput == "dueDate") {
                list = list.filter { it.expiresAt != null }
            }

            // 5. Sorting
            sortByInput?.let { sort ->
                val isAsc = sortOrderInput == "asc"
                list = list.sortedWith { a, b ->
                    val comparison = when (sort) {
                        "priority" -> a.priority.compareTo(b.priority)
                        "date" -> {
                            val strA = a.createdAt ?: ""
                            val strB = b.createdAt ?: ""
                            strA.compareTo(strB)
                        }
                        "dueDate" -> {
                            val strA = a.expiresAt ?: ""
                            val strB = b.expiresAt ?: ""
                            strA.compareTo(strB)
                        }
                        else -> 0
                    }
                    if (isAsc) comparison else -comparison
                }
            }

            if (isActive) {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    _filteredTasks.value = list
                }
            }
        }
    }

    fun initOrgan(projUuid: String, orgUuid: String) {
        if (projectUuid == projUuid && organUuid == orgUuid && organData != null) return
        projectUuid = projUuid
        organUuid = orgUuid
        loadOrganDetails()
    }

    fun loadOrganDetails() {
        val pUuid = projectUuid ?: return
        val oUuid = organUuid ?: return

        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            isAccessDenied = false
            try {
                // Load Project, Organ and Permissions
                val projectRes = ProjectRepository.getProject(pUuid)
                val organRes = OrganRepository.getOrgan(pUuid, oUuid)
                val permissionsRes = OrganRepository.getOrganPermissions(pUuid, oUuid)

                var isProjDenied = false
                projectRes.onFailure { e ->
                    if (e.message?.contains("Access denied", ignoreCase = true) == true) {
                        isProjDenied = true
                    }
                }

                var isOrganDenied = false
                organRes.onFailure { e ->
                    if (e.message?.contains("Access denied", ignoreCase = true) == true) {
                        isOrganDenied = true
                    }
                }

                var isPermDenied = false
                permissionsRes.onFailure { e ->
                    if (e.message?.contains("Access denied", ignoreCase = true) == true) {
                        isPermDenied = true
                    }
                }

                if (isProjDenied || isOrganDenied || isPermDenied) {
                    isAccessDenied = true
                } else if (projectRes.isSuccess && organRes.isSuccess && permissionsRes.isSuccess) {
                    val project = projectRes.getOrNull()
                    val organ = organRes.getOrNull()
                    val perms = permissionsRes.getOrNull()

                    if (project != null && organ != null && perms != null) {
                        projectTitle = project.title
                        projectColor = project.color ?: "#FF7DD4"
                        isProjectAdmin = project.role == "ADMIN" || project.role == "MANAGER"
                        organData = organ
                        permissions = perms.permissions

                        // Load Tasks
                        loadTasks()
                        // Load links if panel is active
                        if (showLinksPanel) {
                            loadLinks()
                        }
                    } else {
                        errorMessage = "Une erreur est survenue lors de la récupération des données"
                    }
                } else {
                    errorMessage = "Erreur de chargement des détails"
                }
            } catch (e: Exception) {
                errorMessage = "Erreur réseau: ${e.localizedMessage}"
                Log.e("ORGAN_DETAILS_VM", "loadOrganDetails error", e)
            } finally {
                isLoading = false
            }
        }
    }

    fun loadTasks() {
        val pUuid = projectUuid ?: return
        val oUuid = organUuid ?: return

        viewModelScope.launch {
            TaskRepository.getTasks(pUuid, oUuid)
                .onSuccess { list ->
                    allTasks = list
                }.onFailure { e ->
                    Log.e("ORGAN_DETAILS_VM", "loadTasks failed", e)
                }
        }
    }

    // Toggle Links Panel
    fun toggleLinksPanel() {
        showLinksPanel = !showLinksPanel
        if (showLinksPanel) {
            loadLinks()
        }
    }

    fun loadLinks() {
        val pUuid = projectUuid ?: return
        val oUuid = organUuid ?: return

        viewModelScope.launch {
            isLinksLoading = true
            linkErrorMessage = null
            OrganLinkRepository.getLinks(pUuid, oUuid)
                .onSuccess { list ->
                    links = list
                }.onFailure { e ->
                    linkErrorMessage = e.message ?: "Erreur"
                    Log.e("ORGAN_DETAILS_VM", "loadLinks error", e)
                }
            isLinksLoading = false
        }
    }

    fun createLink(url: String, description: String?) {
        val pUuid = projectUuid ?: return
        val oUuid = organUuid ?: return

        viewModelScope.launch {
            isSavingLink = true
            linkErrorMessage = null
            OrganLinkRepository.createLink(pUuid, oUuid, url, description)
                .onSuccess {
                    loadLinks()
                }.onFailure { e ->
                    linkErrorMessage = e.message ?: "Erreur de création"
                    Log.e("ORGAN_DETAILS_VM", "createLink error", e)
                }
            isSavingLink = false
        }
    }

    fun updateLink(linkUuid: String, url: String, description: String?) {
        val pUuid = projectUuid ?: return
        val oUuid = organUuid ?: return

        viewModelScope.launch {
            isSavingLink = true
            linkErrorMessage = null
            OrganLinkRepository.updateLink(pUuid, oUuid, linkUuid, url, description)
                .onSuccess {
                    loadLinks()
                }.onFailure { e ->
                    linkErrorMessage = e.message ?: "Erreur de mise à jour"
                    Log.e("ORGAN_DETAILS_VM", "updateLink error", e)
                }
            isSavingLink = false
        }
    }

    fun deleteLink(linkUuid: String) {
        val pUuid = projectUuid ?: return
        val oUuid = organUuid ?: return

        viewModelScope.launch {
            isLinksLoading = true
            linkErrorMessage = null
            OrganLinkRepository.deleteLink(pUuid, oUuid, linkUuid, permanent = true)
                .onSuccess {
                    loadLinks()
                }.onFailure { e ->
                    linkErrorMessage = e.message ?: "Erreur de suppression"
                    Log.e("ORGAN_DETAILS_VM", "deleteLink error", e)
                }
            isLinksLoading = false
        }
    }

    fun updateTaskStatus(task: Task, newStatus: String) {
        val pUuid = projectUuid ?: return
        val oUuid = organUuid ?: return

        // Optimistic UI update
        val originalList = allTasks
        allTasks = allTasks.map {
            if (it.uuid == task.uuid) it.copy(status = newStatus) else it
        }

        viewModelScope.launch {
            val updatedTask = task.copy(status = newStatus)
            TaskRepository.updateTask(pUuid, oUuid, task.uuid, updatedTask)
                .onFailure { e ->
                    // Revert on failure
                    allTasks = originalList
                    Log.e("ORGAN_DETAILS_VM", "updateTaskStatus failed", e)
                }
        }
    }

    fun resetFilters() {
        _sortBy.value = null
        _sortOrder.value = "asc"
        _filterMe.value = false
        _minPriority.value = 0
        _selectedStatuses.value = emptyList()
        updateFilteredTasks()
    }

    fun canDragTask(task: Task): Boolean {
        if (isProjectAdmin) return true
        val hasAll = hasPermission("TASK_STATUS_CHANGE_ALL")
        val hasOwn = hasPermission("TASK_STATUS_CHANGE_OWN")
        if (hasAll) return true
        if (hasOwn) {
            val currentUserId = UserRepository.currentUser?.uuid
            val isManager = task.manager?.uuid == currentUserId
            val isAssignee = task.assignees?.any { it.uuid == currentUserId } ?: false
            return isManager || isAssignee
        }
        return false
    }

    fun canChangeStatus(task: Task, newStatus: String): Boolean {
        if (isProjectAdmin) return true

        val hasAll = hasPermission("TASK_STATUS_CHANGE_ALL")
        val hasOwn = hasPermission("TASK_STATUS_CHANGE_OWN")

        if (!hasAll && !hasOwn) return false

        val currentUserId = UserRepository.currentUser?.uuid
        val isManager = task.manager?.uuid == currentUserId
        val isAssignee = task.assignees?.any { it.uuid == currentUserId } ?: false

        if (!hasAll && hasOwn && !isManager && !isAssignee) return false

        if (newStatus == "DONE" || newStatus == "CANCELED") {
            return isManager
        }

        return true
    }
}
