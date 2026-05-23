package fr.studio.voxel.organ.domain.model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

class SidebarViewModel : ViewModel(
) {

    var selectedProjectUuid by mutableStateOf<String?>(null)

    fun selectProject(uuid: String) {
        selectedProjectUuid = uuid
    }

    fun clearSelection() {
        selectedProjectUuid = null
    }
}

