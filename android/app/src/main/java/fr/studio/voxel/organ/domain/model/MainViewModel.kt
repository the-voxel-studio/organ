package fr.studio.voxel.organ.domain.model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

class MainViewModel : ViewModel() {

    var users by mutableStateOf(listOf<User>())
        private set

    var projects by mutableStateOf(listOf<Project>())
        private set

    // 🔧 LOGIQUE MÉTIER
    fun addProject(project: Project) {
        projects = projects + project
    }

    fun addOrgan(projectId: Int, organ: Organ) {
        projects = projects.map {
            if (it.id == projectId) {
                it.copy(organs = it.organs + organ)
            } else it
        }
    }

    fun addUserToOrgan(projectId: Int, organId: Int, userId: Int) {
        projects = projects.map { project ->
            if (project.id == projectId) {
                project.copy(
                    organs = project.organs.map { organ ->
                        if (organ.id == organId) {
                            organ.copy(memberIds = organ.memberIds + userId)
                        } else organ
                    }
                )
            } else project
        }
    }
}