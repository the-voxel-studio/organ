package fr.studio.voxel.organ.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import fr.studio.voxel.organ.ui.components.Header
import fr.studio.voxel.organ.ui.components.PrimaryButton
import fr.studio.voxel.organ.OrganScreen
import fr.studio.voxel.organ.R
import fr.studio.voxel.organ.ViewModel.MainViewModel
import fr.studio.voxel.organ.ui.components.DashboardComponents.PriorityTask
import fr.studio.voxel.organ.ui.components.DashboardComponents.ProjectSticker
import fr.studio.voxel.organ.ui.theme.MaterialColorScheme

@Composable
fun Dashboard(
    navController: NavHostController,
    dashVM : MainViewModel = viewModel()
){
    LazyColumn(
        modifier = Modifier
            .fillMaxHeight()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Header(
                navigateUp = { navController.navigate(OrganScreen.Sidebar.name) },
                canOpenSidebar = true
            )
        }
        item {
            Column(
                modifier = Modifier
                    .padding(vertical = 24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "Tableau de bord",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = "Gérez vos projets et tâches prioritaires.",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Normal
                    ),
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
        item {
            PrimaryButton("Nouveau Projet", onClick = {/* TODO */})
        }

        item {
            Row(
                modifier = Modifier
                    .padding(vertical = 32.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(MaterialColorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.menu_tache),
                        contentDescription = "Logo des tâches",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp) // Taille de l'icône seule
                    )
                }

                Spacer(modifier = Modifier.width(24.dp))

                Column(
                    modifier = Modifier
                        .padding(top = 32.dp, bottom = 24.dp)
                        .fillMaxWidth()
                ) {
                    Text(
                        text = "Tâches prioritaires",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = "Vos tâches les plus importantes à faire.",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Normal
                        ),
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }

        item {
            PriorityTask()
        }

        item {
            Row(
                modifier = Modifier
                    .padding(top = 32.dp, bottom = 24.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(MaterialColorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.projet_folder),
                        contentDescription = "Logo des tâches",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.width(24.dp))

                Column(
                    modifier = Modifier
                        .padding(top = 32.dp)
                        .fillMaxWidth()
                ) {
                    Text(
                        text = "Mes Projets",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = "Les projets auxquels vous participez.",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Normal
                        ),
                        color = MaterialTheme.colorScheme.outline
                    )
                }

            }
            ProjectSticker(projectsList = dashVM.projects ?: emptyList())
        }
    }

}