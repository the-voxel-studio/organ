package fr.studio.voxel.organ.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import fr.studio.voxel.organ.R
import fr.studio.voxel.organ.domain.model.MainViewModel
import fr.studio.voxel.organ.domain.model.SidebarViewModel
import fr.studio.voxel.organ.ui.components.Footer
import fr.studio.voxel.organ.ui.components.Header
import fr.studio.voxel.organ.ui.components.PrimaryButton
import fr.studio.voxel.organ.ui.components.ProjectItem
import fr.studio.voxel.organ.ui.components.SecondaryButton

@Composable
fun SideBar(
    mainVM: MainViewModel = viewModel(),
    sidebarVM: SidebarViewModel = viewModel()
){
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Header(navigateUp = {})

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ){
            Image(
                painter = painterResource(R.drawable.poubelle_logo),
                contentDescription = "poubelle",
                modifier = Modifier
                    .size(24.dp)
                    .clickable {
                        {/*TODO*/}
                    }
            )

            Image(
                painter = painterResource(R.drawable.notification_logo),
                contentDescription = "notification",
                modifier = Modifier
                    .size(24.dp)
                    .clickable {
                        {/*TODO*/}
                    }
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        SecondaryButton(
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