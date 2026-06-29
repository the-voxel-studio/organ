package fr.studio.voxel.organ.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.studio.voxel.organ.data.ProjectRepository
import fr.studio.voxel.organ.network.services.Task

@Composable
fun LineTask(
    task: Task,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Resolve project color from local store
    val projectColorStr = ProjectRepository.projects?.find { it.uuid == task.projectUuid }?.color
    val projectColor = try {
        val colorStr = projectColorStr?.removePrefix("0x")?.removePrefix("#") ?: "FF7DD4"
        val parseStr = if (colorStr.length == 6) "FF$colorStr" else colorStr
        Color(parseStr.toLong(16))
    } catch (e: Exception) {
        MaterialTheme.colorScheme.primary
    }

    // Resolve organ color from local store
    val projectOrgans = ProjectRepository.projects?.find { it.uuid == task.projectUuid }?.organs
    val organColorStr = projectOrgans?.find { it.uuid == task.organUuid }?.highlightColor
    val organColor = try {
        val colorStr = organColorStr?.removePrefix("0x")?.removePrefix("#") ?: "355EE4"
        val parseStr = if (colorStr.length == 6) "FF$colorStr" else colorStr
        Color(parseStr.toLong(16))
    } catch (e: Exception) {
        MaterialTheme.colorScheme.secondary
    }

    // Dynamic priority color coding matching Angular
    val priorityBg = when {
        task.priority >= 4 -> Color(0xFFEF4444) // Red-500
        task.priority == 3 -> Color(0xFFF59E0B) // Amber-400
        else -> Color(0xFFF3F4F6) // Gray-100
    }
    val priorityTextColor = when {
        task.priority >= 3 -> Color.White
        else -> Color(0xFF6B7280) // Gray-500
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Line 1: Priority + Title + Date
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(priorityBg),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = task.priority.toString(),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Black
                        ),
                        color = priorityTextColor
                    )
                }

                Text(
                    text = task.title,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            task.expiresAt?.let { expiresAt ->
                Text(
                    text = expiresAt.take(10),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        }

        // Line 2: Project / Organ badges side by side
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Project badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(projectColor.copy(alpha = 0.08f))
                    .border(1.dp, projectColor.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(projectColor)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = task.projectName ?: "Projet",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = projectColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Organ badge
            task.organName?.let { organName ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(organColor.copy(alpha = 0.08f))
                        .border(1.dp, organColor.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(organColor)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = organName,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = organColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}