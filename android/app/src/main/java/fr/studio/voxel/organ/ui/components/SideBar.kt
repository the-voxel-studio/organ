package fr.studio.voxel.organ.components

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import fr.studio.voxel.organ.domain.model.MainViewModel
import fr.studio.voxel.organ.domain.model.SidebarViewModel

@Composable
fun Sidebar(
    mainVM: MainViewModel = viewModel(),
    sidebarVM: SidebarViewModel = viewModel()
) {

    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(250.dp)
    ) {

        // 🔝 Logo + titre
        Header()

        // 🔔 Notifications
        NotificationIcon()

        // 📊 Dashboard
        PrimaryButton("Dashboard") { }

        // 📁 Liste des projets
        vm.projects.forEach { project ->
            ProjectItem(project)
        }

        Spacer(modifier = Modifier.weight(1f))

        // 👤 Profil + settings
        UserSection()
    }
}

@Composable
fun Sidebar(
    mainVM: MainViewModel = viewModel(),
    sidebarVM: SidebarViewModel = viewModel()
) {

    Column {

        mainVM.projects.forEach { project ->

            Text(
                text = project.name,
                modifier = Modifier.clickable {
                    sidebarVM.selectProject(project.id)
                }
            )

            project.organs.forEach { organ ->
                Text(
                    text = organ.name,
                    modifier = Modifier.clickable {
                        sidebarVM.selectOrgan(organ.id)
                    }
                )
            }
        }
    }
}