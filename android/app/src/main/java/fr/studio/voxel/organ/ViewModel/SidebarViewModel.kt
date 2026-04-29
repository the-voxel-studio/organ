package fr.studio.voxel.organ.domain.model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

class SidebarViewModel : ViewModel(
) {

    var selectedProjectId by mutableStateOf<Int?>(null)
    var selectedOrganId by mutableStateOf<Int?>(null)

    fun selectProject(id: Int) {
        selectedProjectId = id
        selectedOrganId = null
    }

    fun selectOrgan(id: Int) {
        selectedOrganId = id
    }
}

