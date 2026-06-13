package fr.studio.voxel.organ.ui.task.components

import androidx.compose.animation.*
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import fr.studio.voxel.organ.R
import fr.studio.voxel.organ.ui.theme.MaterialColorScheme
import fr.studio.voxel.organ.viewmodel.TaskDetailsViewModel
import fr.studio.voxel.organ.ui.organ.details.components.openBrowser
import fr.studio.voxel.organ.domain.ValidateUrlUseCase

@Composable
fun TaskLinksCard(
    viewModel: TaskDetailsViewModel,
    isTrashed: Boolean,
    modifier: Modifier = Modifier
) {
    var showLinksExpanded by remember { mutableStateOf(false) }
    var newLinkUrl by remember { mutableStateOf("") }
    var newLinkDesc by remember { mutableStateOf("") }

    val validateUrlUseCase = remember { ValidateUrlUseCase() }
    val isUrlValid = remember(newLinkUrl) {
        validateUrlUseCase(newLinkUrl)
    }

    val context = LocalContext.current
 
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
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.outline
                        )
                    } else {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            viewModel.links.forEach { link ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.surface)
                                        .border(
                                            1.dp,
                                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                            RoundedCornerShape(12.dp)
                                        )
                                        .clickable { openBrowser(context, link.url) }
                                        .padding(horizontal = 12.dp, vertical = 10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Link,
                                        contentDescription = null,
                                        tint = MaterialColorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = link.description ?: link.url,
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (link.description != null) {
                                            Text(
                                                text = link.url,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                    if (canManageLinks) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Supprimer",
                                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                            modifier = Modifier
                                                .size(16.dp)
                                                .clickable { viewModel.deleteLink(link.uuid) }
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
                                isError = !isUrlValid,
                                supportingText = {
                                    if (!isUrlValid) {
                                        Text("Veuillez saisir une URL valide")
                                    }
                                },
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
                                    if (newLinkUrl.isNotBlank() && isUrlValid) {
                                        viewModel.addLink(newLinkUrl, if (newLinkDesc.isNotBlank()) newLinkDesc else null)
                                        newLinkUrl = ""
                                        newLinkDesc = ""
                                    }
                                },
                                enabled = newLinkUrl.isNotBlank() && isUrlValid,
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
