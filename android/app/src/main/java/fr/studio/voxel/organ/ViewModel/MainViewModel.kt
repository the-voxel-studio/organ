package fr.studio.voxel.organ.ViewModel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import fr.studio.voxel.organ.ui.components.DashboardComponents.ProjectVisual
import fr.studio.voxel.organ.R

class MainViewModel : ViewModel() {

    var users by mutableStateOf(listOf<User>())
        private set

    //=====A l'avenir les projets viendront de la BDD=====
    var projects by mutableStateOf<List<Project>?>(
        listOf(
            Project(
                id = 1,
                name = "Projet Alpha",
                description = "Le but de ce projet et de reconstruire la planète entière.",
                dateCreation = "06/05/2026",
                color = "0xFF5EAA6C",
                visual = ProjectVisual.Emoji("🧬"),
                state = "Active",
                memberIds = listOf(1, 2),
                organs = listOf(
                    Organ(1, "Design", listOf(1)),
                    Organ(2, "Développement", listOf(2))
                )
            ),
            Project(
                id = 2,
                name = "Projet Bêta",
                description = "Ecrire un livre en entier de A à Z.",
                dateCreation = "28/10/2025",
                color = "0xFF1E4589",
                visual = ProjectVisual.SvgIcon(R.drawable.menu_tache),
                state = "Terminée",
                memberIds = listOf(3),
                organs = listOf(
                    Organ(3, "Marketing", listOf(3))
                )
            ),
            Project(
                id = 3,
                name = "Projet Gamma",
                description = "Tuer l'enderdragon dans minecraft.",
                dateCreation = "01/01/2026",
                color = "0xFFAEBAEF",
                visual = ProjectVisual.Emoji("🚀"),
                state = "Archivée",
                memberIds = listOf(1, 4),
                organs = emptyList()
            ),
            Project(
                id = 4,
                name = "Projet X",
                description = "faire tout le frontend d'un site web.",
                dateCreation = "31/12/2024",
                color = "0xFF8A60BE",
                visual = ProjectVisual.SvgIcon(R.drawable.poubelle_logo),
                state = "En Attente",
                memberIds = listOf(1, 4),
                organs = emptyList()
            )
       )
    )
        private set

    var tasks by mutableStateOf(
        listOf(
            Task(
                id = 1,
                name = "Tache Alpha",
                progress = 4,
                deadline = "26/02/18",
                projectId = 1,
                organId = 1
            ),
            Task(
                id = 2,
                name = "Tache Bêta",
                progress = 2,
                deadline = "04/08/22",
                projectId = 1,
                organId = 2
            ),
            Task(
                id = 2,
                name = "Tache Omega",
                progress = 8,
                deadline = "10/11/28",
                projectId = 2,
                organId = 3
            )
        )
    )
        private set

    // Pour les tests, fonction de réinitialisation
    fun clearProjects() {
        projects = emptyList() // L'utilisateur existe mais n'a aucun projet
    }

    fun addProject(project: Project) {
        projects = projects?.plus(project)
    }

    fun addOrgan(projectId: Int, organ: Organ) {
        projects = projects?.map {
            if (it.id == projectId) {
                it.copy(organs = it.organs + organ)
            } else it
        }
    }

    fun addUserToOrgan(projectId: Int, organId: Int, userId: Int) {
        projects = projects?.map { project ->
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