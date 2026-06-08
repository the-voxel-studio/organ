package fr.studio.voxel.organ.viewmodel

import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.studio.voxel.organ.data.ProjectRepository
import fr.studio.voxel.organ.network.ApiClient
import fr.studio.voxel.organ.network.services.Organ
import fr.studio.voxel.organ.network.services.OrganApiService
import fr.studio.voxel.organ.network.services.ProjectApiService
import fr.studio.voxel.organ.network.services.CreateRoleRequest
import fr.studio.voxel.organ.network.services.AssignRoleRequest
import fr.studio.voxel.organ.ui.components.IconType
import kotlinx.coroutines.launch
import fr.studio.voxel.organ.utils.ImageHelper

data class RolePreset(
    val name: String,
    val iconData: String,
    val description: String,
    var permissions: List<String> = emptyList()
)

data class FormRole(
    val id: String,
    val name: String,
    val iconType: String = "EMOJI",
    val iconData: String = "👤",
    val permissions: List<String> = listOf("ORGAN_VIEW"),
    val isNew: Boolean = false
)

data class FormMember(
    val userUuid: String,
    val name: String,
    val email: String,
    val roles: List<String> = emptyList(),
    val initialRoles: List<String> = emptyList(),
    val isExisting: Boolean = false
)

class OrganFormViewModel : ViewModel() {

    private val organService = ApiClient.createService(OrganApiService::class.java)
    private val projectService = ApiClient.createService(ProjectApiService::class.java)

    var projectUuid by mutableStateOf("")
        private set

    var organUuid by mutableStateOf<String?>(null)
        private set

    var isEdit by mutableStateOf(false)
        private set

    var title by mutableStateOf("")
    var description by mutableStateOf("")
    var selectedColor by mutableStateOf(Color(0xFF4ADE80)) // default green for organs
    var selectedIconTab by mutableStateOf(IconType.EMOJI)
    var selectedEmoji by mutableStateOf("🔧")
    var selectedSvgCode by mutableStateOf("")
    var selectedImageUri by mutableStateOf<Uri?>(null)
        private set
    var selectedImageUriString by mutableStateOf("")
        private set

    var isLoading by mutableStateOf(false)
        private set

    var accessDenied by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
    var isSuccess by mutableStateOf(false)
    var createdOrganUuid by mutableStateOf<String?>(null)
        private set

    // Context Metadata
    var projectTitle by mutableStateOf("")
    var projectColor by mutableStateOf(Color(0xFFF27B9B))

    // Form inputs for roles & members
    var availablePermissions by mutableStateOf<List<String>>(emptyList())
    var organPermissions by mutableStateOf<List<String>>(emptyList())
    var taskPermissions by mutableStateOf<List<String>>(emptyList())
    var interactionPermissions by mutableStateOf<List<String>>(emptyList())

    var projectMembers by mutableStateOf<List<fr.studio.voxel.organ.network.services.ProjectMember>>(emptyList())

    var roles by mutableStateOf<List<FormRole>>(emptyList())
    var addedMembers by mutableStateOf<List<FormMember>>(emptyList())

    var originalRoles: List<FormRole> = emptyList()
    var originalMembers: List<FormMember> = emptyList()

    var canEditInfo by mutableStateOf(true)
    var canManageRoles by mutableStateOf(true)
    var canManageMembers by mutableStateOf(true)

    val presets = mapOf(
        "responsible" to RolePreset("Responsable", "👑", "Contrôle total sur l'Organ, les rôles et toutes les tâches."),
        "manager" to RolePreset("Manager", "📂", "Gère l'intégralité du cycle de vie des tâches, les assignations et les membres."),
        "participant" to RolePreset("Participant", "👨‍💻", "Peut créer des tâches et gérer ses propres tickets et commentaires.", listOf(
            "ORGAN_VIEW", "TASK_CREATE", "TASK_EDIT_OWN", "TASK_DELETE_OWN", "TASK_STATUS_CHANGE_OWN", "TASK_PRIORITY_CHANGE_OWN",
            "TASK_DATES_MANAGE_OWN", "TASK_ESTIMATE_MANAGE_OWN", "TASK_ASSIGN_SELF", "TASK_LINK_MANAGE_OWN", "TASK_TAG_MANAGE_OWN",
            "TASK_DEPENDENCY_MANAGE_OWN", "COMMENT_CREATE", "COMMENT_EDIT_OWN", "COMMENT_DELETE_OWN", "ATTACHMENT_ADD", "ATTACHMENT_DELETE_OWN"
        )),
        "reviewer" to RolePreset("Correcteur", "✅", "Focus sur la revue, la validation et le changement de statut des tâches.", listOf(
            "ORGAN_VIEW", "TASK_EDIT_ALL", "TASK_STATUS_CHANGE_ALL", "TASK_VALIDATE", "TASK_PRIORITY_CHANGE_ALL", "TASK_DATES_MANAGE_ALL",
            "COMMENT_CREATE", "COMMENT_EDIT_OWN", "COMMENT_DELETE_ALL", "ATTACHMENT_ADD"
        )),
        "tester" to RolePreset("Testeur", "🔍", "Rapporte des bugs et valide les corrections effectuées.", listOf(
            "ORGAN_VIEW", "TASK_CREATE", "TASK_STATUS_CHANGE_OWN", "COMMENT_CREATE", "ATTACHMENT_ADD"
        )),
        "observer" to RolePreset("Observateur", "👁️", "Accès en lecture seule avec possibilité de commenter.", listOf("ORGAN_VIEW", "COMMENT_CREATE")),
        "guest" to RolePreset("Invité", "✉️", "Accès très restreint, uniquement en lecture seule.", listOf("ORGAN_VIEW"))
    )

    fun initOrgan(projectUuid: String, organUuid: String?) {
        this.projectUuid = projectUuid
        this.organUuid = organUuid
        this.isEdit = organUuid != null
        this.isSuccess = false
        this.createdOrganUuid = null
        this.errorMessage = null
        this.accessDenied = false

        checkPermissionsAndLoad(projectUuid, organUuid)
    }

    fun updateImageUri(context: android.content.Context, uri: Uri?) {
        selectedImageUri = uri
        if (uri != null) {
            isLoading = true
            viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                val base64 = ImageHelper.uriToBase64(context, uri)
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    selectedImageUriString = base64 ?: ""
                    isLoading = false
                }
            }
        } else {
            selectedImageUriString = ""
        }
    }



    private fun checkPermissionsAndLoad(projectUuid: String, organUuid: String?) {
        viewModelScope.launch {
            isLoading = true
            try {
                // Get available permissions
                val permsRes = organService.getAvailablePermissions()
                if (permsRes.isSuccessful) {
                    val perms = permsRes.body()?.map { it.name } ?: emptyList()
                    availablePermissions = perms

                    organPermissions = perms.filter { it.startsWith("ORGAN_") && it != "ORGAN_HARD_DELETE" }
                    taskPermissions = perms.filter { it.startsWith("TASK_") }
                    interactionPermissions = perms.filter { it.startsWith("COMMENT_") || it.startsWith("ATTACHMENT_") }

                    presets["responsible"]?.permissions = perms.filter { it != "PROJECT_HARD_DELETE" && it != "ORGAN_HARD_DELETE" }
                    presets["manager"]?.permissions = perms.filter { it != "ORGAN_MANAGE_ROLES" && it != "ORGAN_HARD_DELETE" && it != "PROJECT_HARD_DELETE" }
                }

                // Get project details
                val projRes = projectService.getProject(projectUuid)
                if (projRes.isSuccessful) {
                    val proj = projRes.body()
                    if (proj != null) {
                        projectTitle = proj.title
                        projectColor = parseHexColor(proj.color ?: "")
                    }
                }

                // Get project members
                val membersRes = projectService.getProjectMembers(projectUuid)
                if (membersRes.isSuccessful) {
                    projectMembers = membersRes.body() ?: emptyList()
                }

                // Verify user privileges
                var userPerms = emptyList<String>()
                if (organUuid != null) {
                    val userPermsRes = organService.getOrganPermissions(projectUuid, organUuid)
                    if (userPermsRes.isSuccessful) {
                        userPerms = userPermsRes.body()?.permissions ?: emptyList()
                    }
                }

                val projectPermsRes = projectService.getProjectPermissions(projectUuid)
                var projectRole = "MEMBER"
                if (projectPermsRes.isSuccessful) {
                    projectRole = projectPermsRes.body()?.role ?: "MEMBER"
                }

                val fullUserPerms = userPerms.toMutableList()
                if (projectRole == "ADMIN" || projectRole == "MANAGER") {
                    fullUserPerms.add("ALL")
                }

                val hasAccess = fullUserPerms.contains("ORGAN_EDIT") || fullUserPerms.contains("ORGAN_MANAGE_ROLES") || fullUserPerms.contains("ALL") || (organUuid == null && (projectRole == "ADMIN" || projectRole == "MANAGER"))
                if (!hasAccess) {
                    accessDenied = true
                    isLoading = false
                    return@launch
                }

                canEditInfo = fullUserPerms.contains("ORGAN_EDIT") || fullUserPerms.contains("ALL") || organUuid == null
                canManageRoles = fullUserPerms.contains("ORGAN_MANAGE_ROLES") || fullUserPerms.contains("ORGAN_MANAGE_MEMBERS") || fullUserPerms.contains("ALL") || organUuid == null
                canManageMembers = fullUserPerms.contains("ORGAN_MANAGE_MEMBERS") || fullUserPerms.contains("ORGAN_MANAGE_ROLES") || fullUserPerms.contains("ALL") || organUuid == null

                if (organUuid != null) {
                    // Edit organ
                    val response = organService.getOrgan(projectUuid, organUuid)
                    if (response.isSuccessful) {
                        val organ = response.body()
                        if (organ != null) {
                            title = organ.title
                            description = organ.description ?: ""
                            selectedColor = parseHexColor(organ.highlightColor)

                            val type = when (organ.iconType) {
                                "EMOJI" -> IconType.EMOJI
                                "SVG" -> IconType.SVG
                                "IMAGE", "BLOB" -> IconType.IMAGE
                                else -> IconType.EMOJI
                            }
                            selectedIconTab = type
                            when (type) {
                                IconType.EMOJI -> selectedEmoji = organ.iconData ?: "🔧"
                                IconType.SVG -> selectedSvgCode = organ.iconData ?: ""
                                IconType.IMAGE -> {
                                    selectedImageUriString = organ.iconData ?: ""
                                    selectedImageUri = if (selectedImageUriString.isNotBlank()) Uri.parse(selectedImageUriString) else null
                                }
                                else -> {}
                            }
                        }
                    }

                    // Load roles and assigned members
                    val rolesRes = organService.getOrganRoles(projectUuid, organUuid)
                    if (rolesRes.isSuccessful) {
                        val rolesList = rolesRes.body() ?: emptyList()
                        roles = rolesList.map { r ->
                            FormRole(
                                id = r.uuid,
                                name = r.name,
                                iconType = r.iconType,
                                iconData = r.iconData ?: "👤",
                                permissions = r.permissions ?: emptyList()
                            )
                        }
                        originalRoles = roles.toList()

                        val membersMap = mutableMapOf<String, FormMember>()
                        rolesList.forEach { role ->
                            role.members?.forEach { m ->
                                val existing = membersMap[m.uuid]
                                val rolesListForMember = (existing?.roles ?: emptyList()) + role.uuid
                                val name = listOf(m.firstName, m.lastName).filter { it.isNotBlank() }.joinToString(" ").ifBlank { m.email }
                                membersMap[m.uuid] = FormMember(
                                    userUuid = m.uuid,
                                    name = name,
                                    email = m.email,
                                    roles = rolesListForMember,
                                    initialRoles = rolesListForMember,
                                    isExisting = true
                                )
                            }
                        }
                        addedMembers = membersMap.values.toList()
                        originalMembers = addedMembers.toList()
                    }
                } else {
                    // Creation organ
                    title = ""
                    description = ""
                    selectedColor = projectColor
                    selectedIconTab = IconType.EMOJI
                    selectedEmoji = "🔧"
                    selectedSvgCode = ""
                    selectedImageUri = null
                    selectedImageUriString = ""

                    val newRoleId = "role-resp-" + System.currentTimeMillis()
                    val respPreset = presets["responsible"]!!
                    roles = listOf(
                        FormRole(
                            id = newRoleId,
                            name = respPreset.name,
                            iconData = respPreset.iconData,
                            permissions = respPreset.permissions
                        )
                    )
                    addedMembers = emptyList()
                    originalRoles = emptyList()
                    originalMembers = emptyList()
                }
            } catch (e: Exception) {
                errorMessage = "Erreur réseau: ${e.localizedMessage}"
                Log.e("ORGAN_VM", "initOrgan error", e)
            } finally {
                isLoading = false
            }
        }
    }

    fun updateRoles(newRoles: List<FormRole>) {
        roles = newRoles
        val roleIds = newRoles.map { it.id }.toSet()
        addedMembers = addedMembers.map { member ->
            member.copy(roles = member.roles.filter { roleIds.contains(it) })
        }
    }

    fun submit() {
        if (title.isBlank()) {
            errorMessage = "Le titre est obligatoire."
            return
        }
        if (addedMembers.any { it.roles.isEmpty() }) {
            errorMessage = "Veuillez attribuer au moins un rôle à chaque collaborateur."
            return
        }

        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            try {
                val hexColor = String.format("#%06X", 0xFFFFFF and selectedColor.toArgb())
                val iconTypeStr = when (selectedIconTab) {
                    IconType.EMOJI -> "EMOJI"
                    IconType.SVG -> "SVG"
                    IconType.IMAGE, IconType.CAMERA -> "BLOB"
                }
                val iconDataStr = when (selectedIconTab) {
                    IconType.EMOJI -> selectedEmoji
                    IconType.SVG -> selectedSvgCode
                    IconType.IMAGE, IconType.CAMERA -> selectedImageUriString
                }

                val finalOrganUuid: String

                if (isEdit && organUuid != null) {
                    finalOrganUuid = organUuid!!
                    if (canEditInfo) {
                        val organData = Organ(
                            id = null,
                            uuid = finalOrganUuid,
                            projectId = 0,
                            title = title,
                            description = description.ifBlank { null },
                            iconType = iconTypeStr,
                            iconData = iconDataStr.ifBlank { null },
                            highlightColor = hexColor,
                            createdAt = "",
                            deletedAt = null
                        )
                        val response = organService.updateOrgan(projectUuid, finalOrganUuid, organData)
                        if (!response.isSuccessful) {
                            errorMessage = "Erreur lors de la mise à jour des détails : ${response.code()}"
                            isLoading = false
                            return@launch
                        }
                    }
                } else {
                    val organData = Organ(
                        id = null,
                        uuid = "",
                        projectId = 0,
                        title = title,
                        description = description.ifBlank { null },
                        iconType = iconTypeStr,
                        iconData = iconDataStr.ifBlank { null },
                        highlightColor = hexColor,
                        createdAt = "",
                        deletedAt = null
                    )
                    val response = organService.createOrgan(projectUuid, organData)
                    if (response.isSuccessful && response.body() != null) {
                        finalOrganUuid = response.body()!!.uuid
                    } else {
                        errorMessage = "Erreur lors de la création de l'Organ : ${response.code()}"
                        isLoading = false
                        return@launch
                    }
                }

                // Roles and members sync logic
                if (canManageRoles) {
                    val roleIdMap = mutableMapOf<String, String>()

                    // 1. Delete removed roles
                    if (isEdit) {
                        for (origRole in originalRoles) {
                            if (!origRole.id.startsWith("role-") && !roles.any { it.id == origRole.id }) {
                                organService.deleteOrganRole(projectUuid, finalOrganUuid, origRole.id)
                            }
                        }
                    }

                    // 2. Create new roles or update existing
                    for (role in roles) {
                        val isNew = role.id.startsWith("role-")
                        val req = CreateRoleRequest(
                            name = role.name,
                            iconType = role.iconType,
                            iconData = role.iconData,
                            permissions = role.permissions
                        )

                        if (isNew) {
                            val res = organService.createOrganRole(projectUuid, finalOrganUuid, req)
                            if (res.isSuccessful && res.body() != null) {
                                roleIdMap[role.id] = res.body()!!.uuid
                            }
                        } else {
                            organService.updateOrganRole(projectUuid, finalOrganUuid, role.id, req)
                            roleIdMap[role.id] = role.id
                        }
                    }

                    // 3. Update members assignments
                    if (canManageMembers) {
                        for (member in addedMembers) {
                            val currentRolesServer = member.roles.map { roleIdMap[it] ?: it }
                            val initialRolesServer = member.initialRoles.map { roleIdMap[it] ?: it }

                            val rolesToAdd = currentRolesServer.filter { !initialRolesServer.contains(it) }
                            val rolesToRemove = initialRolesServer.filter { !currentRolesServer.contains(it) }

                            for (serverRoleUuid in rolesToAdd) {
                                organService.assignRole(projectUuid, finalOrganUuid, serverRoleUuid, AssignRoleRequest(member.userUuid))
                            }
                            for (serverRoleUuid in rolesToRemove) {
                                organService.unassignRole(projectUuid, finalOrganUuid, serverRoleUuid, member.userUuid)
                            }
                        }

                        // Remove completely unassigned members
                        if (isEdit) {
                            val removedMembers = originalMembers.filter { orig ->
                                !addedMembers.any { it.userUuid == orig.userUuid }
                            }
                            for (removed in removedMembers) {
                                for (localRoleId in removed.initialRoles) {
                                    val serverRoleUuid = roleIdMap[localRoleId] ?: localRoleId
                                    organService.unassignRole(projectUuid, finalOrganUuid, serverRoleUuid, removed.userUuid)
                                }
                            }
                        }
                    }
                } else if (canManageMembers) {
                    // User can manage members but not roles
                    for (member in addedMembers) {
                        val rolesToAdd = member.roles.filter { !member.initialRoles.contains(it) }
                        val rolesToRemove = member.initialRoles.filter { !member.roles.contains(it) }

                        for (roleId in rolesToAdd) {
                            organService.assignRole(projectUuid, finalOrganUuid, roleId, AssignRoleRequest(member.userUuid))
                        }
                        for (roleId in rolesToRemove) {
                            organService.unassignRole(projectUuid, finalOrganUuid, roleId, member.userUuid)
                        }
                    }

                    if (isEdit) {
                        val removedMembers = originalMembers.filter { orig ->
                            !addedMembers.any { it.userUuid == orig.userUuid }
                        }
                        for (removed in removedMembers) {
                            for (roleId in removed.initialRoles) {
                                organService.unassignRole(projectUuid, finalOrganUuid, roleId, removed.userUuid)
                            }
                        }
                    }
                }

                ProjectRepository.fetchDashboard()
                createdOrganUuid = finalOrganUuid
                isSuccess = true
            } catch (e: Exception) {
                errorMessage = "Erreur réseau : ${e.localizedMessage}"
                Log.e("ORGAN_VM", "submit error", e)
            } finally {
                isLoading = false
            }
        }
    }

    fun deleteOrgan(onSuccess: () -> Unit, onError: (String) -> Unit) {
        val oUuid = organUuid ?: return
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            try {
                val res = organService.deleteOrgan(projectUuid, oUuid, permanent = false)
                if (res.isSuccessful) {
                    ProjectRepository.removeOrgan(projectUuid, oUuid)
                    onSuccess()
                } else {
                    errorMessage = res.message().ifBlank { "Échec de la suppression de l'Organ." }
                    onError(errorMessage ?: "")
                }
            } catch (e: Exception) {
                Log.e("ORGAN_FORM_VM", "Error deleting organ", e)
                errorMessage = "Erreur réseau."
                onError(errorMessage ?: "")
            } finally {
                isLoading = false
            }
        }
    }

    private fun parseHexColor(hex: String): Color {
        return try {
            val cleanHex = hex.replace("#", "")
            if (cleanHex.length == 6) {
                Color(android.graphics.Color.parseColor("#$cleanHex"))
            } else {
                Color(0xFF4ADE80)
            }
        } catch (e: Exception) {
            Color(0xFF4ADE80)
        }
    }
}
