package fr.studio.voxel.organ.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import fr.studio.voxel.organ.ui.header.Header
import fr.studio.voxel.organ.R
import fr.studio.voxel.organ.ui.OrganScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import fr.studio.voxel.organ.viewmodel.DashboardViewModel
import fr.studio.voxel.organ.ui.components.AddButton
import fr.studio.voxel.organ.ui.components.LoadingOverlay
import fr.studio.voxel.organ.ui.components.PrimaryButton
import fr.studio.voxel.organ.ui.theme.MaterialColorScheme

import fr.studio.voxel.organ.ui.components.ShimmerBox

@Composable
fun DashboardShimmer() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header Mock
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ShimmerBox(modifier = Modifier.size(48.dp), shape = CircleShape)
            ShimmerBox(modifier = Modifier.width(100.dp).height(24.dp))
            ShimmerBox(modifier = Modifier.size(48.dp), shape = CircleShape)
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Title Section Mock
        Column(modifier = Modifier.fillMaxWidth()) {
            ShimmerBox(modifier = Modifier.width(200.dp).height(32.dp))
            Spacer(modifier = Modifier.height(8.dp))
            ShimmerBox(modifier = Modifier.fillMaxWidth(0.7f).height(16.dp))
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Task Section Mock
        Row(verticalAlignment = Alignment.CenterVertically) {
            ShimmerBox(modifier = Modifier.size(56.dp), shape = CircleShape)
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                ShimmerBox(modifier = Modifier.width(150.dp).height(20.dp))
                Spacer(modifier = Modifier.height(8.dp))
                ShimmerBox(modifier = Modifier.width(250.dp).height(14.dp))
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        repeat(2) {
            ShimmerBox(modifier = Modifier.fillMaxWidth().height(80.dp))
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun Dashboard(
    navController: NavHostController,
    dashboardVM : DashboardViewModel = viewModel()
){
    Box(modifier = Modifier.fillMaxSize()) {
        if (dashboardVM.isLoading) {
            DashboardShimmer()
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Header(
                    navigateUp = { navController.navigate(OrganScreen.Sidebar.name) },
                    canOpenSidebar = true
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
            item {
                Column(
                    modifier = Modifier
                        .padding(top = 24.dp)
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
                Row(
                    modifier = Modifier
                        .padding(vertical = 16.dp)
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

                    Spacer(modifier = Modifier.width(16.dp))

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
                PriorityTask(
                    onTaskClick = { task ->
                        val projectUuid = task.projectUuid ?: ""
                        val organUuid = task.organUuid ?: ""
                        navController.navigate("${OrganScreen.Task.name}/$projectUuid/$organUuid/${task.uuid}")
                    },
                    dashboardVM = dashboardVM
                )
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
                when{
                    dashboardVM.error != null -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp, horizontal = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.outline_info),
                                contentDescription = "Erreur",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = dashboardVM.error ?: "Erreur inconnue",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            PrimaryButton(
                                text = "Réessayer",
                                onClick = { dashboardVM.refresh() },
                                modifier = Modifier.fillMaxWidth(0.6f)
                            )
                        }
                    }
                    else -> {
                        ProjectSticker(
                            projectsList = dashboardVM.projects ?: emptyList(),
                            onProjectClick = { uuid -> navController.navigate("${OrganScreen.Project.name}/$uuid") }
                        )
                    }
                }
            }
            item{
                Spacer(modifier = Modifier.height(50.dp))
            }
            }
        }
        AddButton(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
        ) {
            navController.navigate(OrganScreen.CreateProject.name)
        }
    }
}
}