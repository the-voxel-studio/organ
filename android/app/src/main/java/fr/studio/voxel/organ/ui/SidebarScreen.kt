package fr.studio.voxel.organ.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.NavHostController
import fr.studio.voxel.organ.OrganScreen
import fr.studio.voxel.organ.R
import fr.studio.voxel.organ.domain.model.MainViewModel
import fr.studio.voxel.organ.domain.model.SidebarViewModel
import fr.studio.voxel.organ.ui.components.Footer
import fr.studio.voxel.organ.ui.components.Header
import fr.studio.voxel.organ.ui.components.ProjectItem
import fr.studio.voxel.organ.ui.components.SecondaryButton

@Composable
fun SideBar(
    navController: NavHostController,
    mainVM: MainViewModel = viewModel(),
    sidebarVM: SidebarViewModel = viewModel()
){
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val interactionPoubelle = remember { MutableInteractionSource() }
    val isPressedPoubelle by interactionPoubelle.collectIsPressedAsState()

    val interactionNotif = remember { MutableInteractionSource() }
    val isPressedNotif by interactionNotif.collectIsPressedAsState()

    Column(
        modifier = Modifier
            .fillMaxHeight()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Header(navigateUp = {})

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ){
            Icon(
                painter = painterResource(R.drawable.poubelle_logo),
                contentDescription = "poubelle",
                modifier = Modifier
                    .size(24.dp)
                    .clickable (
                        interactionSource = interactionPoubelle,
                        indication = null,
                        onClick = { /* TODO */ }
                    ),
                tint = if (isPressedPoubelle) Color.Red else MaterialTheme.colorScheme.onSurface
            )

            Icon(
                painter = painterResource(R.drawable.notification_logo),
                contentDescription = "notification",
                modifier = Modifier
                    .size(24.dp)
                    .clickable (
                        interactionSource = interactionNotif,
                        indication = null,
                        onClick = { /* TODO */ }
                    ),
                tint = if (isPressedNotif) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        SecondaryButton(
            text = "Dashboard",
            iconRes = R.drawable.dashboard_logo,
            isSelected = currentRoute == OrganScreen.Dashboard.name,
            onClick = {
                navController.navigate(OrganScreen.Dashboard.name) {
                // Évite d'empiler plusieurs fois la même page
                popUpTo(OrganScreen.Dashboard.name) { inclusive = true }
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (mainVM.projects.isNotEmpty()) {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "MES PROJETS",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline, // gris
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, bottom = 8.dp)
            )
            //Liste des projets
            mainVM.projects.forEach { project ->
                val projectRoute = "${OrganScreen.Project.name}/${project.id}"
                ProjectItem(
                    project = project,
                    isSelected = currentRoute == projectRoute,
                    onClick = { navController.navigate(projectRoute) }
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Footer()
    }
}