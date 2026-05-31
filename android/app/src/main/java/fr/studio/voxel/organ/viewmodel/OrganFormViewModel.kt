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
import fr.studio.voxel.organ.ui.components.IconType
import kotlinx.coroutines.launch

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

    fun initOrgan(projectUuid: String, organUuid: String?) {
        this.projectUuid = projectUuid
        this.organUuid = organUuid
        this.isEdit = organUuid != null
        this.isSuccess = false
        this.errorMessage = null
        this.accessDenied = false

        checkPermissionsAndLoad(projectUuid, organUuid)
    }

    fun updateImageUri(uri: Uri?) {
        selectedImageUri = uri
        selectedImageUriString = uri?.toString() ?: ""
    }

    private fun checkPermissionsAndLoad(projectUuid: String, organUuid: String?) {
        viewModelScope.launch {
            isLoading = true
            try {
                // First check project permissions
                val permRes = projectService.getProjectPermissions(projectUuid)
                if (permRes.isSuccessful) {
                    val role = permRes.body()?.role
                    // Only ADMIN or MANAGER are allowed to create/edit organs
                    if (role != "ADMIN" && role != "MANAGER") {
                        accessDenied = true
                        isLoading = false
                        return@launch
                    }
                } else {
                    errorMessage = "Erreur lors de la vérification des permissions: ${permRes.code()}"
                    isLoading = false
                    return@launch
                }

                if (organUuid != null) {
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
                                "IMAGE" -> IconType.IMAGE
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
                    } else {
                        errorMessage = "Impossible de charger l'Organ: ${response.code()}"
                    }
                } else {
                    // Default creation values
                    title = ""
                    description = ""
                    selectedColor = Color(0xFF4ADE80)
                    selectedIconTab = IconType.EMOJI
                    selectedEmoji = "🔧"
                    selectedSvgCode = ""
                    selectedImageUri = null
                    selectedImageUriString = ""
                }
            } catch (e: Exception) {
                errorMessage = "Erreur réseau: ${e.localizedMessage}"
                Log.e("ORGAN_VM", "initOrgan error", e)
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
                val iconTypeStr = selectedIconTab.name
                val iconDataStr = when (selectedIconTab) {
                    IconType.EMOJI -> selectedEmoji
                    IconType.SVG -> selectedSvgCode
                    IconType.IMAGE -> selectedImageUriString
                    IconType.CAMERA -> ""
                }

                if (isEdit && organUuid != null) {
                    // Update
                    val organData = Organ(
                        id = null,
                        uuid = organUuid!!,
                        projectId = 0, // backend uses path variable anyway
                        title = title,
                        description = description.ifBlank { null },
                        iconType = iconTypeStr,
                        iconData = iconDataStr.ifBlank { null },
                        highlightColor = hexColor,
                        createdAt = "",
                        deletedAt = null
                    )
                    val response = organService.updateOrgan(projectUuid, organUuid!!, organData)
                    if (response.isSuccessful) {
                        ProjectRepository.fetchDashboard()
                        isSuccess = true
                    } else {
                        errorMessage = "Erreur lors de la mise à jour : ${response.code()}"
                    }
                } else {
                    // Create
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
                    if (response.isSuccessful) {
                        ProjectRepository.fetchDashboard()
                        isSuccess = true
                    } else {
                        errorMessage = "Erreur lors de la création : ${response.code()}"
                    }
                }
            } catch (e: Exception) {
                errorMessage = "Erreur réseau : ${e.localizedMessage}"
                Log.e("ORGAN_VM", "submit error", e)
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
