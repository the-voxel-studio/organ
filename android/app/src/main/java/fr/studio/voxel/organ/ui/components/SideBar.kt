package fr.studio.voxel.organ.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import fr.studio.voxel.organ.domain.model.MainViewModel
import fr.studio.voxel.organ.domain.model.SidebarViewModel

@Composable
fun SideBar(
    mainVM: MainViewModel = viewModel(),
    sidebarVM: SidebarViewModel = viewModel()
){
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(250.dp)
            .padding(start = 16.dp)
    ) {

        Header()

        PrimaryButton(
            text = "Dashboard",
            onClick = { /* TODO */ }
        )

        // 📁 Liste des projets
        mainVM.projects.forEach { project ->
            ProjectItem(project)
        }

        Spacer(modifier = Modifier.weight(1f))

        Footer()
    }
}
