package fr.studio.voxel.organ.ui.trash

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import fr.studio.voxel.organ.R
import fr.studio.voxel.organ.network.services.Project
import fr.studio.voxel.organ.ui.components.*
import fr.studio.voxel.organ.ui.header.Header
import fr.studio.voxel.organ.viewmodel.TrashViewModel

@Composable
fun TrashScreen(
    onBack: () -> Unit,
    viewModel: TrashViewModel = viewModel()
) {
    LaunchedEffect(Unit) {
        viewModel.loadTrashedProjects()
    }

    var searchQuery by remember { mutableStateOf("") }

    // Dialog states
    var showRestoreDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var selectedProject by remember { mutableStateOf<Project?>(null) }

    val filteredProjects = remember(viewModel.trashedProjects, searchQuery) {
        val query = searchQuery.lowercase().trim()
        if (query.isBlank()) viewModel.trashedProjects
        else viewModel.trashedProjects.filter { it.title.lowercase().contains(query) }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        if (viewModel.isLoading && viewModel.trashedProjects.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Header(navigateUp = onBack, canOpenSidebar = false)

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Back link & Title
                    item {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            BackToLink(
                                label = "Retour au Dashboard",
                                onClick = onBack,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Start,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.poubelle_logo),
                                    contentDescription = null,
                                    tint = Color.Red.copy(alpha = 0.8f),
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Corbeille globale",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                                    color = Color.Black
                                )
                            }

                            Text(
                                text = "Retrouvez les projets supprimés dont vous êtes le propriétaire administratif.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                            )
                        }
                    }

                    // Search field (only shown if there are projects in trash)
                    if (viewModel.trashedProjects.isNotEmpty()) {
                        item {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text("Rechercher dans la corbeille...", color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)) },
                                singleLine = true,
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color(0xFFF9F9F9)
                                )
                            )
                        }
                    }

                    if (viewModel.trashedProjects.isEmpty()) {
                        item {
                            EmptyTrashState(
                                title = "Votre corbeille est vide",
                                description = "Aucun projet supprimé à restaurer pour le moment."
                            )
                        }
                    } else if (filteredProjects.isEmpty()) {
                        item {
                            Text(
                                text = "Aucun projet ne correspond à la recherche.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.Gray,
                                modifier = Modifier.padding(vertical = 24.dp)
                            )
                        }
                    } else {
                        items(filteredProjects) { project ->
                            val projectColor = remember(project.color) {
                                try {
                                    Color(android.graphics.Color.parseColor(project.color ?: "#FF7EB6"))
                                } catch (e: Exception) {
                                    Color(0xFFFF7EB6)
                                }
                            }
                            TrashedItemCard(
                                title = project.title,
                                subtitle = "Supprimé le ${formatDeletedDate(project.dateSuppression)}",
                                iconVisual = project.visual,
                                iconTint = projectColor,
                                iconBg = projectColor.copy(alpha = 0.1f),
                                onRestore = {
                                    selectedProject = project
                                    showRestoreDialog = true
                                },
                                onDeletePermanent = {
                                    selectedProject = project
                                    showDeleteDialog = true
                                }
                            )
                        }
                    }
                }
            }

            if (viewModel.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }

    // Dialogs
    if (showRestoreDialog && selectedProject != null) {
        val proj = selectedProject!!
        ProjectRestoreConfirmDialog(
            projectTitle = proj.title,
            onConfirm = {
                showRestoreDialog = false
                viewModel.restoreProject(proj.uuid, onSuccess = {}, onError = {})
            },
            onDismiss = {
                showRestoreDialog = false
                selectedProject = null
            }
        )
    }

    if (showDeleteDialog && selectedProject != null) {
        val proj = selectedProject!!
        DeleteConfirmDialog(
            title = "Supprimer définitivement ?",
            message = "Êtes-vous sûr de vouloir supprimer définitivement le projet \"${proj.title}\" ? Cette action est totalement irréversible. Toutes les tâches, membres et données associés seront perdus.",
            confirmText = "Supprimer",
            iconRes = R.drawable.poubelle_logo,
            iconColor = Color.Red,
            iconBgColor = Color(0xFFFFF1F2),
            onConfirm = {
                showDeleteDialog = false
                viewModel.deleteProjectPermanently(proj.uuid, onSuccess = {}, onError = {})
            },
            onDismiss = {
                showDeleteDialog = false
                selectedProject = null
            }
        )
    }
}

@Composable
fun ProjectRestoreConfirmDialog(
    projectTitle: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(Color(0xFFE0F2FE), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.outline_info),
                        contentDescription = null,
                        tint = Color(0xFF0284C7),
                        modifier = Modifier.size(28.dp)
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Restaurer le projet ?",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color.Black
                    )
                    Text(
                        text = "Êtes-vous sûr de vouloir restaurer le projet \"$projectTitle\" ? Il sera de nouveau visible dans votre espace de travail et sa barre latérale.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                }

                Surface(
                    color = Color(0xFFFEF3C7),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.outline_info),
                                contentDescription = null,
                                tint = Color(0xFFB45309),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "ACTION REQUISE APRÈS RESTAURATION",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                                color = Color(0xFFB45309)
                            )
                        }
                        Text(
                            text = "Les membres du projet, même actifs au moment de la suppression, ne seront pas restaurés automatiquement. Vous devrez les réactiver manuellement depuis la corbeille du projet.",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, lineHeight = 15.sp),
                            color = Color(0xFFD97706)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Text(
                            text = "Annuler",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Button(
                        onClick = onConfirm,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Black, contentColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1.2f).height(48.dp)
                    ) {
                        Text(
                            text = "Confirmer",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
