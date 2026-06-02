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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import fr.studio.voxel.organ.R
import fr.studio.voxel.organ.ui.theme.MaterialColorScheme
import fr.studio.voxel.organ.viewmodel.TaskDetailsViewModel

@Composable
fun TaskLinksCard(
    viewModel: TaskDetailsViewModel,
    isTrashed: Boolean,
    modifier: Modifier = Modifier
) {
    var showLinksExpanded by remember { mutableStateOf(false) }
    var newLinkUrl by remember { mutableStateOf("") }
    var newLinkDesc by remember { mutableStateOf("") }

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
            val canManageLinks = !isTrashed && (
                viewModel.hasPerm("TASK_LINK_MANAGE", isOwner) ||
                viewModel.hasPerm("TASK_EDIT", isOwner)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showLinksExpanded = !showLinksExpanded }
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Liens associés",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Icon(
                    painter = painterResource(R.drawable.flechedroite_logo),
                    contentDescription = if (showLinksExpanded) "Réduire" else "Développer",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .rotate(if (showLinksExpanded) 270f else 90f)
                        .size(16.dp)
                )
            }

            AnimatedVisibility(
                visible = showLinksExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (viewModel.links.isEmpty()) {
                        Text(
                            text = "Aucun lien",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    } else {
                        viewModel.links.forEach { link ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = link.description ?: link.url,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialColorScheme.primary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (link.description != null) {
                                        Text(
                                            text = link.url,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.outline,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                if (canManageLinks) {
                                    IconButton(onClick = { viewModel.deleteLink(link.uuid) }) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Supprimer",
                                            tint = Color(0xFFEF4444),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (canManageLinks) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = newLinkUrl,
                                onValueChange = { newLinkUrl = it },
                                placeholder = { Text("https://example.com") },
                                label = { Text("URL du lien") },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = newLinkDesc,
                                onValueChange = { newLinkDesc = it },
                                placeholder = { Text("Description (facultatif)") },
                                label = { Text("Description") },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Button(
                                onClick = {
                                    if (newLinkUrl.isNotBlank()) {
                                        viewModel.addLink(newLinkUrl, if (newLinkDesc.isNotBlank()) newLinkDesc else null)
                                        newLinkUrl = ""
                                        newLinkDesc = ""
                                    }
                                },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Ajouter le lien")
                            }
                        }
                    }
                }
            }
        }
    }
}
