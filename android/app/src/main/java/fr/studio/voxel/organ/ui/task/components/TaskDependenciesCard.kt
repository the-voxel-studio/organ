package fr.studio.voxel.organ.ui.task.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.studio.voxel.organ.R
import fr.studio.voxel.organ.ui.theme.MaterialColorScheme
import fr.studio.voxel.organ.viewmodel.TaskDetailsViewModel

@Composable
fun TaskDependenciesCard(
    viewModel: TaskDetailsViewModel,
    isTrashed: Boolean,
    modifier: Modifier = Modifier
) {
    var showDepsExpanded by remember { mutableStateOf(false) }
    var showAddDependencyMenu by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val isOwner = viewModel.isTaskOwner()
            val canManageDeps = !isTrashed && (
                viewModel.hasPerm("TASK_DEPENDENCY_MANAGE", isOwner) ||
                viewModel.hasPerm("TASK_EDIT", isOwner)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDepsExpanded = !showDepsExpanded }
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Dépendances",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Icon(
                    painter = painterResource(R.drawable.flechedroite_logo),
                    contentDescription = if (showDepsExpanded) "Réduire" else "Développer",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .rotate(if (showDepsExpanded) 270f else 90f)
                        .size(16.dp)
                )
            }

            AnimatedVisibility(
                visible = showDepsExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (viewModel.dependencies.isEmpty()) {
                        Text(
                            text = "Aucune dépendance",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    } else {
                        viewModel.dependencies.forEach { dep ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = dep.title,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(dep.status, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                    if (canManageDeps) {
                                        IconButton(onClick = { viewModel.removeDependency(dep.dependsOnTaskUuid) }) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Retirer dépendance",
                                                tint = Color(0xFFEF4444),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (canManageDeps) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Box {
                            Text(
                                text = "+ Dépendance",
                                color = MaterialColorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clickable { showAddDependencyMenu = true }
                                    .padding(8.dp)
                            )

                            val availableTasks = viewModel.organTasks.filter { ot ->
                                !viewModel.dependencies.any { d -> d.dependsOnTaskUuid == ot.uuid }
                            }

                            DropdownMenu(
                                expanded = showAddDependencyMenu,
                                onDismissRequest = { showAddDependencyMenu = false }
                            ) {
                                if (availableTasks.isEmpty()) {
                                    DropdownMenuItem(
                                        text = { Text("Aucune tâche disponible") },
                                        onClick = { showAddDependencyMenu = false }
                                    )
                                } else {
                                    availableTasks.forEach { ot ->
                                        DropdownMenuItem(
                                            text = { Text(ot.title) },
                                            onClick = {
                                                viewModel.addDependency(ot.uuid)
                                                showAddDependencyMenu = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
