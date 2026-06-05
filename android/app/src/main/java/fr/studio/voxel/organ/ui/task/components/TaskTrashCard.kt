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
import fr.studio.voxel.organ.R
import fr.studio.voxel.organ.viewmodel.TaskDetailsViewModel

@Composable
fun TaskTrashCard(
    viewModel: TaskDetailsViewModel,
    modifier: Modifier = Modifier
) {
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.toggleTrash() }
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Corbeille de la tâche",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Icon(
                    painter = painterResource(R.drawable.flechedroite_logo),
                    contentDescription = if (viewModel.isTrashOpen) "Réduire" else "Développer",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .rotate(if (viewModel.isTrashOpen) 270f else 90f)
                        .size(16.dp)
                )
            }

            AnimatedVisibility(
                visible = viewModel.isTrashOpen,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Trashed Comments
                    Text(
                        text = "Commentaires supprimés :",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelLarge
                    )
                    if (viewModel.trashedComments.isEmpty()) {
                        Text(
                            text = "Aucun",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.outline
                        )
                    } else {
                        viewModel.trashedComments.forEach { comment ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = comment.content,
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.weight(1f)
                                )
                                Row {
                                    // Restore comment
                                    IconButton(onClick = { viewModel.restoreComment(comment.uuid) }) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.flechedroite_logo),
                                            contentDescription = "Restaurer",
                                            tint = Color(0xFF10B981),
                                            modifier = Modifier
                                                .size(16.dp)
                                                .rotate(180f)
                                        )
                                    }
                                    // Hard delete comment
                                    IconButton(onClick = { viewModel.deleteComment(comment.uuid, true) }) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Effacer définitivement",
                                            tint = Color(0xFFEF4444),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Trashed Attachments
                    Text(
                        text = "Fichiers joints supprimés :",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelLarge
                    )
                    if (viewModel.trashedAttachments.isEmpty()) {
                        Text(
                            text = "Aucun",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.outline
                        )
                    } else {
                        viewModel.trashedAttachments.forEach { att ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = att.fileName,
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.weight(1f)
                                )
                                Row {
                                    IconButton(onClick = { viewModel.restoreAttachment(att.uuid) }) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.flechedroite_logo),
                                            contentDescription = "Restaurer",
                                            tint = Color(0xFF10B981),
                                            modifier = Modifier
                                                .size(16.dp)
                                                .rotate(180f)
                                        )
                                    }
                                    IconButton(onClick = { viewModel.deleteAttachment(att.uuid, true) }) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Effacer définitivement",
                                            tint = Color(0xFFEF4444),
                                            modifier = Modifier.size(16.dp)
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
