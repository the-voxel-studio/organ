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
fun TaskCommentsCard(
    viewModel: TaskDetailsViewModel,
    isTrashed: Boolean,
    modifier: Modifier = Modifier
) {
    var showCommentsExpanded by remember { mutableStateOf(false) }
    var newCommentText by remember { mutableStateOf("") }

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
                    .clickable { showCommentsExpanded = !showCommentsExpanded }
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Commentaires",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Icon(
                    painter = painterResource(R.drawable.flechedroite_logo),
                    contentDescription = if (showCommentsExpanded) "Réduire" else "Développer",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .rotate(if (showCommentsExpanded) 270f else 90f)
                        .size(16.dp)
                )
            }

            AnimatedVisibility(
                visible = showCommentsExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    val isOwner = viewModel.isTaskOwner()
                    val canAddComment = !isTrashed && viewModel.hasPerm("COMMENT_CREATE", isOwner)

                    if (viewModel.comments.isEmpty()) {
                        Text(
                            text = "Aucun commentaire",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.outline
                        )
                    } else {
                        viewModel.comments.forEach { comment ->
                            val currentUserUuid = fr.studio.voxel.organ.data.UserRepository.currentUser?.uuid
                            val canDelete = !isTrashed && (
                                viewModel.hasPerm("COMMENT_DELETE_ALL") ||
                                (comment.user.uuid == currentUserUuid && viewModel.hasPerm("COMMENT_DELETE_OWN"))
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "${comment.user.firstName} ${comment.user.lastName}",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = comment.createdAt.take(10),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                    Text(
                                        text = comment.content,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                if (canDelete) {
                                    IconButton(onClick = { viewModel.deleteComment(comment.uuid) }) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Supprimer",
                                            tint = Color(0xFFEF4444),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (canAddComment) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedTextField(
                                value = newCommentText,
                                onValueChange = { newCommentText = it },
                                placeholder = { Text("Écrire un commentaire...") },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            )
                            Button(
                                onClick = {
                                    if (newCommentText.isNotBlank()) {
                                        viewModel.addComment(newCommentText)
                                        newCommentText = ""
                                    }
                                },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Publier")
                            }
                        }
                    }
                }
            }
        }
    }
}
