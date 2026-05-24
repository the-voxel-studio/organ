package fr.studio.voxel.organ.ui

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.NavHostController
import fr.studio.voxel.organ.OrganScreen
import fr.studio.voxel.organ.R
import fr.studio.voxel.organ.ViewModel.MainViewModel
import fr.studio.voxel.organ.ViewModel.sharedMainViewModel
import fr.studio.voxel.organ.domain.model.SidebarViewModel
import fr.studio.voxel.organ.ui.components.FooterComponents.Footer
import fr.studio.voxel.organ.ui.components.HeaderComponents.Header
import fr.studio.voxel.organ.ui.components.IconButtonPressable
import fr.studio.voxel.organ.ui.components.SidebarComponents.ProjectItem
import fr.studio.voxel.organ.ui.components.SidebarComponents.SecondaryButton

@Composable
fun SideBar(
    navController: NavHostController,
    mainVM: MainViewModel = sharedMainViewModel(),
    sidebarVM: SidebarViewModel = viewModel(),
    onModifButtonClicked : () -> Unit = {}
){
    // Récupère la route de la page juste avant la Sidebar
    val previousRoute = navController.previousBackStackEntry?.destination?.route

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val interactionPoubelle = remember { MutableInteractionSource() }
    val isPressedPoubelle by interactionPoubelle.collectIsPressedAsState()

    val interactionNotif = remember { MutableInteractionSource() }
    val isPressedNotif by interactionNotif.collectIsPressedAsState()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
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
                    .padding(vertical = 16.dp, horizontal = 80.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButtonPressable(
                    icon = R.drawable.poubelle_logo,
                    contentDescription = "poubelle",
                    modifier = Modifier
                        .size(32.dp),
                    onClick = { /* TODO */ },
                    interactionSource = interactionPoubelle,
                    tint = if (isPressedPoubelle) Color.Red else MaterialTheme.colorScheme.onSurface
                )

                IconButtonPressable(
                    icon = R.drawable.notification_logo,
                    contentDescription = "notification",
                    modifier = Modifier
                        .size(32.dp),
                    onClick = { /* TODO */ },
                    interactionSource = interactionNotif,
                    tint = if (isPressedNotif) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            SecondaryButton(
                text = "Dashboard",
                iconRes = R.drawable.dashboard_logo,
                isSelected = (previousRoute == OrganScreen.Dashboard.name),
                onClick = {
                    sidebarVM.clearSelection()
                    navController.navigate(OrganScreen.Dashboard.name) {
                        // Évite d'empiler plusieurs fois la même page
                        popUpTo(OrganScreen.Dashboard.name) { inclusive = true }
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            val projects = mainVM.projects ?: emptyList()

            if (projects.isNotEmpty()) {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "MES PROJETS",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.outline, // gris
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 8.dp, bottom = 8.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    //Liste des projets
                    projects.forEach { project ->
                        val projectRoute = "${OrganScreen.Project.name}/${project.uuid}"
                        ProjectItem(
                            project = project,
                            isSelected = sidebarVM.selectedProjectUuid == project.uuid,
                            onClick = {
                                sidebarVM.selectProject(project.uuid)
                                navController.navigate(projectRoute) {
                                    launchSingleTop = true
                                }
                            }
                        )
                    }
                }
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(16.dp))

            HorizontalDivider(
                modifier = Modifier.fillMaxWidth(0.9f),
                thickness = 2.dp,
                color = MaterialTheme.colorScheme.outline
            )

            Spacer(modifier = Modifier.height(16.dp))

            val userName = mainVM.currentUser?.firstName ?: "Prénom"
            val userLastName = mainVM.currentUser?.lastName ?: "Nom"

            Footer(utilisateur = "$userName $userLastName", onModifButtonClicked = onModifButtonClicked)
        }
    }
}