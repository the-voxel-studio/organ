package fr.studio.voxel.organ.ui.task.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import fr.studio.voxel.organ.R
import fr.studio.voxel.organ.viewmodel.TaskDetailsViewModel

@Composable
fun TaskTimelineCard(
    viewModel: TaskDetailsViewModel,
    modifier: Modifier = Modifier
) {
    var showTimelineExpanded by remember { mutableStateOf(false) }

    if (viewModel.timeline.isNotEmpty()) {
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
                        .clickable { showTimelineExpanded = !showTimelineExpanded }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Historique d'activité",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Icon(
                        painter = painterResource(R.drawable.flechedroite_logo),
                        contentDescription = if (showTimelineExpanded) "Réduire" else "Développer",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .rotate(if (showTimelineExpanded) 270f else 90f)
                            .size(16.dp)
                    )
                }

                AnimatedVisibility(
                    visible = showTimelineExpanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        viewModel.timeline.forEach { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when (item.type) {
                                                "COMMENT" -> Color(0xFF355EE4)
                                                "ATTACHMENT" -> Color(0xFFEF4444)
                                                else -> Color(0xFF94A3B8)
                                            }
                                        )
                                        .align(Alignment.CenterVertically)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    val author = item.userName ?: "Système"
                                    val logText = when (item.type) {
                                        "COMMENT" -> "$author a commenté : \"${item.detail}\""
                                        "ATTACHMENT" -> "$author a joint le fichier : \"${item.detail}\""
                                        else -> {
                                            val field = when (item.fieldName) {
                                                "status" -> "le statut"
                                                "priority" -> "la priorité"
                                                "title" -> "le titre"
                                                "description" -> "la description"
                                                "manager" -> "le responsable"
                                                else -> item.fieldName ?: "la tâche"
                                            }
                                            "$author a modifié $field (${item.actionType})"
                                        }
                                    }
                                    Text(
                                        text = logText,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = item.createdAt.take(16).replace("T", " "),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.outline
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
