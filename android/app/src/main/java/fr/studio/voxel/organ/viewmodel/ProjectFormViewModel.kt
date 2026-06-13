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
import fr.studio.voxel.organ.data.UserRepository
import fr.studio.voxel.organ.domain.ValidateEmailUseCase
import fr.studio.voxel.organ.network.services.Project
import fr.studio.voxel.organ.ui.components.IconType
import kotlinx.coroutines.launch

data class ProjectFormInvite(
    val uuid: String?,
    val email: String,
    val name: String,
    var role: String, // ADMIN, MANAGER, MEMBER
    val isCreator: Boolean,
    val isExisting: Boolean,
    val isPending: Boolean,
    var roleChanged: Boolean = false
)

class ProjectFormViewModel : ViewModel() {

    private val validateEmailUseCase = ValidateEmailUseCase()

    var projectUuid by mutableStateOf<String?>(null)
        private set

    var isEdit by mutableStateOf(false)
        private set

    var title by mutableStateOf("")
    var description by mutableStateOf("")
    var status by mutableStateOf("ACTIVE")
    var selectedColor by mutableStateOf(Color(0xFFFF7DD4))
    var selectedIconTab by mutableStateOf(IconType.EMOJI)
    var selectedEmoji by mutableStateOf("🚀")
    var selectedSvgCode by mutableStateOf("")
    var selectedImageUri by mutableStateOf<Uri?>(null)
        private set
    var selectedImageUriString by mutableStateOf("")
        private set

    // Members management
    var invites by mutableStateOf<List<ProjectFormInvite>>(emptyList())
    val removedMemberUuids = mutableListOf<String>()

    var isLoading by mutableStateOf(false)
        private set

    var accessDenied by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
    var isSuccess by mutableStateOf(false)
    var createdProjectUuid by mutableStateOf<String?>(null)
        private set

    fun initProject(uuid: String?) {
        this.projectUuid = uuid
        this.isEdit = uuid != null
        this.isSuccess = false
        this.createdProjectUuid = null
        this.errorMessage = null
        this.accessDenied = false
        this.invites = emptyList()
        this.removedMemberUuids.clear()

        if (uuid != null) {
            loadProject(uuid)
        } else {
            // Default creation values
            title = ""
            description = ""
            status = "ACTIVE"
            selectedColor = Color(0xFFFF7DD4)
            selectedIconTab = IconType.EMOJI
            selectedEmoji = "🚀"
            selectedSvgCode = ""
            selectedImageUri = null
            selectedImageUriString = ""

            // Add the creator
            val user = UserRepository.currentUser
            if (user != null) {
                invites = listOf(
                    ProjectFormInvite(
                        uuid = null,
                        email = user.email,
                        name = user.firstName ?: "Créateur",
                        role = "ADMIN",
                        isCreator = true,
                        isExisting = false,
                        isPending = false
                    )
                )
            }
        }
    }

    fun updateImageUri(context: android.content.Context, uri: Uri?) {
        selectedImageUri = uri
        if (uri != null) {
            isLoading = true
            viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                val base64 = uriToBase64(context, uri)
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    selectedImageUriString = base64 ?: ""
                    isLoading = false
                }
            }
        } else {
            selectedImageUriString = ""
        }
    }

    private fun uriToBase64(context: android.content.Context, uri: Uri): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
            inputStream?.close()
            
            if (bitmap != null) {
                val maxDimension = 1024
                val width = bitmap.width
                val height = bitmap.height
                val newBitmap = if (width > maxDimension || height > maxDimension) {
                    val ratio = width.toFloat() / height.toFloat()
                    val newWidth = if (ratio > 1) maxDimension else (maxDimension * ratio).toInt()
                    val newHeight = if (ratio > 1) (maxDimension / ratio).toInt() else maxDimension
                    android.graphics.Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
                } else {
                    bitmap
                }

                val outputStream = java.io.ByteArrayOutputStream()
                newBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, outputStream)
                val bytes = outputStream.toByteArray()
                
                if (newBitmap != bitmap) {
                    newBitmap.recycle()
                }
                bitmap.recycle()

                val base64 = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
                val mimeType = "image/jpeg"
                "data:$mimeType;base64,$base64"
            } else {
                null
            }
        } catch (e: java.lang.Exception) {
            Log.e("PROJECT_VM", "Error converting and compressing Uri to Base64", e)
            null
        }
    }

    // Members list operations
    fun addInvite(email: String) {
        val trimmed = email.trim()
        if (trimmed.isBlank() || !validateEmailUseCase(trimmed)) {
            errorMessage = "Adresse e-mail invalide"
            return
        }
        if (invites.any { it.email.equals(trimmed, ignoreCase = true) }) {
            errorMessage = "Cette personne est déjà dans la liste"
            return
        }

        errorMessage = null
        invites = invites + ProjectFormInvite(
            uuid = null,
            email = trimmed,
            name = "Invité",
            role = "MEMBER",
            isCreator = false,
            isExisting = false,
            isPending = true
        )
    }

    fun updateRole(index: Int, newRole: String) {
        val list = invites.toMutableList()
        val item = list[index]
        if (item.isCreator) return

        list[index] = item.copy(
            role = newRole,
            roleChanged = true
        )
        invites = list
    }

    fun removeInvite(index: Int) {
        val list = invites.toMutableList()
        val item = list[index]
        if (item.isCreator) return

        if (item.isExisting && item.uuid != null) {
            removedMemberUuids.add(item.uuid)
        }
        list.removeAt(index)
        invites = list
    }

    private fun loadProject(uuid: String) {
        viewModelScope.launch {
            isLoading = true
            try {
                // First check permission
                val permRes = ProjectRepository.getProjectPermissions(uuid)
                permRes.onSuccess { perm ->
                    if (perm.role != "ADMIN") {
                        accessDenied = true
                        isLoading = false
                        return@launch
                    }
                }.onFailure { e ->
                    errorMessage = "Erreur lors de la vérification des permissions: ${e.message}"
                    isLoading = false
                    return@launch
                }

                // Load project data
                val response = ProjectRepository.getProject(uuid)
                response.onSuccess { project ->
                    title = project.title
                    description = project.description ?: ""
                    status = project.state ?: "ACTIVE"
                    selectedColor = parseHexColor(project.color ?: "#FF7EB6")
                    
                    val type = when (project.iconType) {
                        "EMOJI" -> IconType.EMOJI
                        "SVG" -> IconType.SVG
                        "IMAGE", "BLOB" -> IconType.IMAGE
                        else -> IconType.EMOJI
                    }
                    selectedIconTab = type
                    when (type) {
                        IconType.EMOJI -> selectedEmoji = project.iconData ?: "🚀"
                        IconType.SVG -> selectedSvgCode = project.iconData ?: ""
                        IconType.IMAGE -> {
                            selectedImageUriString = project.iconData ?: ""
                            selectedImageUri = if (selectedImageUriString.isNotBlank()) Uri.parse(selectedImageUriString) else null
                        }
                        else -> {}
                    }
                }.onFailure { e ->
                    errorMessage = "Impossible de charger le projet: ${e.message}"
                    isLoading = false
                    return@launch
                }

                // Load project members
                val mappedMembers = mutableListOf<ProjectFormInvite>()
                val currentUserEmail = UserRepository.currentUser?.email ?: ""
                
                val membersRes = ProjectRepository.getProjectMembers(uuid)
                membersRes.onSuccess { membersList ->
                    membersList.forEach { member ->
                        val isCreator = member.user.email == currentUserEmail || member.role == "ADMIN"
                        mappedMembers.add(
                            ProjectFormInvite(
                                uuid = member.uuid,
                                email = member.user.email,
                                name = member.user.firstName,
                                role = member.role,
                                isCreator = isCreator,
                                isExisting = true,
                                isPending = false
                            )
                        )
                    }
                }

                // Load project invitations
                val invitesRes = ProjectRepository.getProjectInvitations(uuid)
                invitesRes.onSuccess { invitationsList ->
                    invitationsList.forEach { invite ->
                        mappedMembers.add(
                            ProjectFormInvite(
                                uuid = invite.uuid,
                                email = invite.email,
                                name = "Invité",
                                role = invite.role,
                                isCreator = false,
                                isExisting = true,
                                isPending = true
                            )
                        )
                    }
                }

                // Sort creator first
                mappedMembers.sortWith(compareByDescending { it.isCreator })
                invites = mappedMembers

            } catch (e: Exception) {
                errorMessage = "Erreur réseau: ${e.localizedMessage}"
                Log.e("PROJECT_VM", "loadProject error", e)
            } finally {
                isLoading = false
            }
        }
    }

    fun submit() {
        if (title.isBlank()) {
            errorMessage = "Le titre est obligatoire."
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

                var successUuid: String? = null

                if (isEdit && projectUuid != null) {
                    // Update project details
                    val projectData = Project(
                        id = null,
                        uuid = projectUuid!!,
                        title = title,
                        description = description.ifBlank { null },
                        color = hexColor,
                        state = status,
                        iconType = iconTypeStr,
                        iconData = iconDataStr.ifBlank { null },
                        dateCreation = "",
                        dateSuppression = null
                    )
                    ProjectRepository.updateProject(projectUuid!!, projectData)
                        .onSuccess {
                            successUuid = projectUuid
                        }.onFailure { e ->
                            errorMessage = "Erreur lors de la mise à jour : ${e.message}"
                        }
                } else {
                    // Create project
                    val projectData = Project(
                        id = null,
                        uuid = "",
                        title = title,
                        description = description.ifBlank { null },
                        color = hexColor,
                        state = status,
                        iconType = iconTypeStr,
                        iconData = iconDataStr.ifBlank { null },
                        dateCreation = "",
                        dateSuppression = null
                    )
                    ProjectRepository.createProject(projectData)
                        .onSuccess { created ->
                            successUuid = created.uuid
                        }.onFailure { e ->
                            errorMessage = "Erreur lors de la création : ${e.message}"
                        }
                }

                if (successUuid != null) {
                    // Process deletions (removeMember)
                    for (memberUuid in removedMemberUuids) {
                        try {
                            ProjectRepository.removeMember(successUuid, memberUuid)
                        } catch (e: Exception) {
                            Log.e("PROJECT_VM", "removeMember failed for $memberUuid", e)
                        }
                    }

                    // Process role updates (updateMemberRole)
                    val roleUpdates = invites.filter { it.isExisting && it.roleChanged && !it.isPending }
                    val sortedUpdates = roleUpdates.sortedByDescending { it.role == "ADMIN" }
                    
                    for (update in sortedUpdates) {
                        try {
                            if (update.uuid != null) {
                                ProjectRepository.updateMemberRole(successUuid, update.uuid, update.role)
                            }
                        } catch (e: Exception) {
                            Log.e("PROJECT_VM", "updateMemberRole failed for ${update.email}", e)
                        }
                    }

                    // Process new invitations (inviteMember)
                    val newInvites = invites.filter { !it.isExisting && !it.isCreator }
                    for (invite in newInvites) {
                        try {
                            ProjectRepository.inviteMember(successUuid, invite.email, invite.role)
                        } catch (e: Exception) {
                            Log.e("PROJECT_VM", "inviteMember failed for ${invite.email}", e)
                        }
                    }

                    // Refresh global repository dashboard data
                    ProjectRepository.fetchDashboard()
                    createdProjectUuid = successUuid
                    isSuccess = true
                }
            } catch (e: Exception) {
                errorMessage = "Erreur réseau : ${e.localizedMessage}"
                Log.e("PROJECT_VM", "submit error", e)
            } finally {
                isLoading = false
            }
        }
    }

    fun deleteProject(onSuccess: () -> Unit, onError: (String) -> Unit) {
        val pUuid = projectUuid ?: return
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            ProjectRepository.deleteProject(pUuid)
                .onSuccess {
                    onSuccess()
                }.onFailure { e ->
                    errorMessage = e.message ?: "Échec de la suppression du Projet."
                    onError(errorMessage ?: "")
                }
            isLoading = false
        }
    }

    private fun parseHexColor(hex: String): Color {
        return try {
            val cleanHex = hex.replace("#", "")
            if (cleanHex.length == 6) {
                Color(android.graphics.Color.parseColor("#$cleanHex"))
            } else {
                Color(0xFFFF7DD4)
            }
        } catch (e: Exception) {
            Color(0xFFFF7DD4)
        }
    }
}
