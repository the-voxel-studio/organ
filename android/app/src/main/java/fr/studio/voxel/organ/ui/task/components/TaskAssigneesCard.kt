package fr.studio.voxel.organ.ui.task.components

import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.studio.voxel.organ.R
import fr.studio.voxel.organ.ui.theme.MaterialColorScheme
import fr.studio.voxel.organ.viewmodel.TaskDetailsViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TaskAssigneesCard(
    viewModel: TaskDetailsViewModel,
    isTrashed: Boolean,
    modifier: Modifier = Modifier
) {
    var showAssigneesExpanded by remember { mutableStateOf(false) }
    var showAddAssigneeMenu by remember { mutableStateOf(false) }

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
            val canManageAssignees = !isTrashed && (
                viewModel.hasPerm("TASK_ASSIGN_OTHERS", isOwner) ||
                viewModel.hasPerm("TASK_ASSIGN_SELF") ||
                viewModel.hasPerm("TASK_EDIT", isOwner)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showAssigneesExpanded = !showAssigneesExpanded }
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Membres assignés",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Icon(
                    painter = painterResource(R.drawable.flechedroite_logo),
                    contentDescription = if (showAssigneesExpanded) "Réduire" else "Développer",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .rotate(if (showAssigneesExpanded) 270f else 90f)
                        .size(16.dp)
                )
            }

            AnimatedVisibility(
                visible = showAssigneesExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (viewModel.assignees.isEmpty()) {
                        Text(
                            text = "Aucun collaborateur assigné",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.outline
                        )
                    } else {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            viewModel.assignees.forEach { assignee ->
                                AssistChip(
                                    onClick = {},
                                    label = { Text("${assignee.firstName} ${assignee.lastName}") },
                                    trailingIcon = {
                                        if (canManageAssignees) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Retirer",
                                                tint = Color(0xFFEF4444),
                                                modifier = Modifier
                                                    .size(16.dp)
                                                    .clickable { viewModel.removeAssignee(assignee.uuid) }
                                            )
                                        }
                                    },
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }
                        }
                    }

                    if (canManageAssignees) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Box {
                            Text(
                                text = "+ Ajouter",
                                color = MaterialColorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clickable { showAddAssigneeMenu = true }
                                    .padding(8.dp)
                            )

                            val availableMembers = viewModel.projectMembers.filter { pm ->
                                !viewModel.assignees.any { a -> a.uuid == pm.user.uuid }
                            }

                            DropdownMenu(
                                expanded = showAddAssigneeMenu,
                                onDismissRequest = { showAddAssigneeMenu = false }
                            ) {
                                if (availableMembers.isEmpty()) {
                                    DropdownMenuItem(
                                        text = { Text("Aucun autre membre") },
                                        onClick = { showAddAssigneeMenu = false }
                                    )
                                } else {
                                    availableMembers.forEach { pm ->
                                        DropdownMenuItem(
                                            text = { Text("${pm.user.firstName} ${pm.user.lastName}") },
                                            onClick = {
                                                viewModel.addAssignee(pm.user.uuid)
                                                showAddAssigneeMenu = false
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
