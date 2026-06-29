package fr.studio.voxel.organ.ui.project.stats.components

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.studio.voxel.organ.viewmodel.MemberActivityStats

@Composable
fun MemberActivityStatsCard(
    stat: MemberActivityStats,
    highlightColor: Color
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                val nameInitials = remember(stat.firstName, stat.lastName) {
                    (stat.firstName.take(1) + stat.lastName.take(1)).uppercase()
                }
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(highlightColor.copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = nameInitials,
                        fontWeight = FontWeight.Bold,
                        color = highlightColor,
                        fontSize = 14.sp
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${stat.firstName} ${stat.lastName}",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color.Black
                    )
                    Text(
                        text = "${stat.role} • ${stat.email}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFF1F5F9))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${stat.totalActions} act.",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.Black
                        )
                    }

                    Icon(
                        imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = Color.Gray
                    )
                }
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    HorizontalDivider(color = Color(0xFFF1F5F9))
                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Répartition détaillée des actions :",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.Gray
                    )

                    val maxActions = maxOf(1, stat.totalActions)

                    MemberActionRow(label = "📝 Tâches créées", count = stat.createdTasks, max = maxActions, color = Color(0xFF3B82F6))
                    MemberActionRow(label = "🔄 Changements de statut", count = stat.statusChanges, max = maxActions, color = Color(0xFF10B981))
                    MemberActionRow(label = "💬 Commentaires rédigés", count = stat.comments, max = maxActions, color = Color(0xFFF59E0B))
                    MemberActionRow(label = "📎 Fichiers joints", count = stat.attachments, max = maxActions, color = Color(0xFFEC4899))
                    MemberActionRow(label = "⚙️ Mises à jour de tâches", count = stat.updates, max = maxActions, color = highlightColor)
                    MemberActionRow(label = "👁️ Consultations d'éléments", count = stat.consultations, max = maxActions, color = Color(0xFF6366F1))
                }
            }
        }
    }
}

@Composable
fun MemberActionRow(
    label: String,
    count: Int,
    max: Int,
    color: Color
) {
    val progress = remember(count, max) { count.toFloat() / max.toFloat() }
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = label, fontSize = 12.sp, color = Color.Black)
            Text(text = count.toString(), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Black)
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = progress,
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape),
            color = color,
            trackColor = Color(0xFFF1F5F9)
        )
    }
}
