package fr.studio.voxel.organ.ui.task.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import fr.studio.voxel.organ.R
import fr.studio.voxel.organ.ui.theme.MaterialColorScheme
import fr.studio.voxel.organ.viewmodel.TaskDetailsViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TaskTagsCard(
    viewModel: TaskDetailsViewModel,
    isTrashed: Boolean,
    modifier: Modifier = Modifier
) {
    var showTagsExpanded by remember { mutableStateOf(false) }
    var showAddTagMenu by remember { mutableStateOf(false) }

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
            val canManageTags = !isTrashed && (
                viewModel.hasPerm("TASK_TAG_MANAGE", isOwner) ||
                viewModel.hasPerm("TASK_EDIT", isOwner)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showTagsExpanded = !showTagsExpanded }
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tags de la tâche",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Icon(
                    painter = painterResource(R.drawable.flechedroite_logo),
                    contentDescription = if (showTagsExpanded) "Réduire" else "Développer",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .rotate(if (showTagsExpanded) 270f else 90f)
                        .size(16.dp)
                )
            }

            AnimatedVisibility(
                visible = showTagsExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    val taskTags = viewModel.taskData?.tags ?: emptyList()
                    if (taskTags.isEmpty()) {
                        Text(
                            text = "Aucun tag associé",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.outline
                        )
                    } else {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            taskTags.forEach { tag ->
                                val tagColor = try {
                                    Color(android.graphics.Color.parseColor(if (tag.color.startsWith("#")) tag.color else "#${tag.color}"))
                                } catch (e: Exception) {
                                    MaterialColorScheme.primary
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(tagColor.copy(alpha = 0.15f))
                                        .border(1.dp, tagColor.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = tag.name,
                                        color = tagColor,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                    if (canManageTags) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Enlever",
                                            tint = tagColor,
                                            modifier = Modifier
                                                .size(12.dp)
                                                .clickable { viewModel.removeTag(tag.uuid) }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (canManageTags) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Box {
                            Text(
                                text = "+ Associer",
                                color = MaterialColorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clickable { showAddTagMenu = true }
                                    .padding(8.dp)
                            )

                            val currentTags = viewModel.taskData?.tags ?: emptyList()
                            val remainingTags = viewModel.allProjectTags.filter { t ->
                                !currentTags.any { ct -> ct.uuid == t.uuid }
                            }

                            DropdownMenu(
                                expanded = showAddTagMenu,
                                onDismissRequest = { showAddTagMenu = false }
                            ) {
                                if (remainingTags.isEmpty()) {
                                    DropdownMenuItem(
                                        text = { Text("Aucun autre tag") },
                                        onClick = { showAddTagMenu = false }
                                    )
                                } else {
                                    remainingTags.forEach { tag ->
                                        DropdownMenuItem(
                                            text = { Text(tag.name) },
                                            onClick = {
                                                viewModel.addTag(tag.uuid)
                                                showAddTagMenu = false
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
