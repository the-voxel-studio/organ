package fr.studio.voxel.organ.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import fr.studio.voxel.organ.domain.model.MainViewModel
import fr.studio.voxel.organ.domain.model.SidebarViewModel
import fr.studio.voxel.organ.ui.components.Footer
import fr.studio.voxel.organ.ui.components.Header
import fr.studio.voxel.organ.ui.components.ProjectItem
import fr.studio.voxel.organ.ui.components.SecondaryButton

@Composable
fun SideBar(
    mainVM: MainViewModel = viewModel(),
    sidebarVM: SidebarViewModel = viewModel()
){
    Column(
        modifier = Modifier.Companion
            .fillMaxHeight()
            .padding(16.dp),
        horizontalAlignment = Alignment.Companion.CenterHorizontally
    ) {

        Header()

        Spacer(modifier = Modifier.Companion.width(128.dp))

        SecondaryButton(
            text = "Dashboard",
            onClick = { /* TODO */ }
        )

        // 📁 Liste des projets
        mainVM.projects.forEach { project ->
            ProjectItem(project)
        }

        Spacer(modifier = Modifier.Companion.weight(1f))

        Footer()
    }
}