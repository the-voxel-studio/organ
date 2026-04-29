package fr.studio.voxel.organ.domain.model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

class MainViewModel : ViewModel() {

    var users by mutableStateOf(listOf<User>())
        private set

    //=====A l'avenir les projets viendront de la BDD=====
    var projects by mutableStateOf(
        listOf(
            Project(
                id = 1,
                name = "Projet Alpha",
                memberIds = listOf(1, 2),
                organs = listOf(
                    Organ(1, "Design", listOf(1)),
                    Organ(2, "Développement", listOf(2))
                )
            ),
            Project(
                id = 2,
                name = "Projet Bêta",
                memberIds = listOf(3),
                organs = listOf(
                    Organ(3, "Marketing", listOf(3))
                )
            ),
            Project(
                id = 3,
                name = "Projet Gamma",
                memberIds = listOf(1, 4),
                organs = emptyList()
            )
        )
    )
        private set

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