package fr.studio.voxel.organ.ui.organ.details.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.studio.voxel.organ.viewmodel.OrganDetailsViewModel

@Composable
fun FilterChipCustom(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    highlightColor: Color
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) highlightColor.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface)
            .border(
                width = 1.dp,
                color = if (selected) highlightColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold),
            color = if (selected) highlightColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
    }
}

@Composable
fun OrganFiltersRow(
    viewModel: OrganDetailsViewModel,
    highlightColor: Color
) {
    var showPriorityMenu by remember { mutableStateOf(false) }
    var showStatusMenu by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }

    val hasActiveFilters = viewModel.filterMe || viewModel.minPriority > 0 ||
            viewModel.selectedStatuses.isNotEmpty() || viewModel.sortBy != null

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChipCustom(
                    selected = viewModel.filterMe,
                    onClick = { viewModel.filterMe = !viewModel.filterMe },
                    label = "Moi",
                    highlightColor = highlightColor
                )

                Box {
                    FilterChipCustom(
                        selected = viewModel.minPriority > 0,
                        onClick = { showPriorityMenu = true },
                        label = if (viewModel.minPriority > 0) "Prio >= ${viewModel.minPriority}" else "Priorité",
                        highlightColor = highlightColor
                    )
                    DropdownMenu(
                        expanded = showPriorityMenu,
                        onDismissRequest = { showPriorityMenu = false }
                    ) {
                        (0..5).forEach { priority ->
                            DropdownMenuItem(
                                text = { Text(if (priority == 0) "Toutes" else ">= $priority") },
                                onClick = {
                                    viewModel.minPriority = priority
                                    showPriorityMenu = false
                                }
                            )
                        }
                    }
                }

                Box {
                    FilterChipCustom(
                        selected = viewModel.selectedStatuses.isNotEmpty(),
                        onClick = { showStatusMenu = true },
                        label = if (viewModel.selectedStatuses.isNotEmpty()) "${viewModel.selectedStatuses.size} statuts" else "Statuts",
                        highlightColor = highlightColor
                    )
                    DropdownMenu(
                        expanded = showStatusMenu,
                        onDismissRequest = { showStatusMenu = false }
                    ) {
                        val statuses = listOf(
                            "TODO" to "À faire",
                            "IN_PROGRESS" to "En cours",
                            "WAITING" to "En attente",
                            "DONE" to "Terminé",
                            "CANCELED" to "Annulé"
                        )
                        statuses.forEach { (status, label) ->
                            val isSelected = viewModel.selectedStatuses.contains(status)
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Checkbox(
                                            checked = isSelected,
                                            onCheckedChange = null,
                                            colors = CheckboxDefaults.colors(checkedColor = highlightColor)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(label)
                                    }
                                },
                                onClick = {
                                    val current = viewModel.selectedStatuses.toMutableList()
                                    if (isSelected) {
                                        current.remove(status)
                                    } else {
                                        current.add(status)
                                    }
                                    viewModel.selectedStatuses = current
                                }
                            )
                        }
                    }
                }

                Box {
                    FilterChipCustom(
                        selected = viewModel.sortBy != null,
                        onClick = { showSortMenu = true },
                        label = when (viewModel.sortBy) {
                            "priority" -> "Prio ${if (viewModel.sortOrder == "asc") "↑" else "↓"}"
                            "dueDate" -> "Échéance ${if (viewModel.sortOrder == "asc") "↑" else "↓"}"
                            "date" -> "Créé ${if (viewModel.sortOrder == "asc") "↑" else "↓"}"
                            else -> "Trier"
                        },
                        highlightColor = highlightColor
                    )
                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false }
                    ) {
                        listOf(
                            "priority" to "Priorité",
                            "dueDate" to "Date d'échéance",
                            "date" to "Date de création"
                        ).forEach { (sort, label) ->
                            DropdownMenuItem(
                                text = { Text("$label (Croissant)") },
                                onClick = {
                                    viewModel.sortBy = sort
                                    viewModel.sortOrder = "asc"
                                    showSortMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("$label (Décroissant)") },
                                onClick = {
                                    viewModel.sortBy = sort
                                    viewModel.sortOrder = "desc"
                                    showSortMenu = false
                                }
                            )
                        }
                    }
                }
            }

            if (hasActiveFilters) {
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.error.copy(alpha = 0.1f))
                        .clickable { viewModel.resetFilters() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Réinitialiser",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
