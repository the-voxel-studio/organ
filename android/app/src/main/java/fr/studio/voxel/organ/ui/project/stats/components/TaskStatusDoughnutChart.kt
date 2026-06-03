package fr.studio.voxel.organ.ui.project.stats.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.studio.voxel.organ.network.services.CurrentOrganStatusStat

@Composable
fun TaskStatusDoughnutChart(
    current: List<CurrentOrganStatusStat>,
    highlightColor: Color,
    modifier: Modifier = Modifier
) {
    val statusStats = remember(current) {
        val map = mutableMapOf<String, Int>()
        current.forEach { item ->
            val status = item.status
            if (status != null && status.isNotBlank() && item.taskCount > 0) {
                map[status] = (map[status] ?: 0) + item.taskCount
            }
        }
        map.toList()
    }

    val totalTasks = remember(statusStats) { statusStats.sumOf { it.second } }

    if (totalTasks == 0) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text("Aucune tâche active", color = Color.Gray)
        }
        return
    }

    val statusColors = remember(highlightColor) {
        mapOf(
            "TODO" to Color(0xFF94A3B8),
            "IN_PROGRESS" to highlightColor,
            "WAITING" to Color(0xFFF59E0B),
            "DONE" to Color(0xFF10B981),
            "CANCELED" to Color(0xFFEF4444)
        )
    }

    val statusTranslations = remember {
        mapOf(
            "TODO" to "À faire",
            "IN_PROGRESS" to "En cours",
            "WAITING" to "En attente",
            "DONE" to "Terminé",
            "CANCELED" to "Annulé"
        )
    }

    Row(
        modifier = modifier.padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Canvas(modifier = Modifier.size(140.dp)) {
            var currentAngle = -90f
            statusStats.forEach { (status, count) ->
                val color = statusColors[status.uppercase()] ?: Color.LightGray
                val sweepAngle = (count.toFloat() / totalTasks.toFloat()) * 360f

                drawArc(
                    color = color,
                    startAngle = currentAngle,
                    sweepAngle = sweepAngle,
                    useCenter = true
                )
                currentAngle += sweepAngle
            }

            drawCircle(
                color = Color.White,
                radius = size.minDimension / 3.2f
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            statusStats.forEach { (status, count) ->
                val color = statusColors[status.uppercase()] ?: Color.LightGray
                val name = statusTranslations[status.uppercase()] ?: status
                val percentage = ((count.toFloat() / totalTasks.toFloat()) * 100).toInt()

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(color, CircleShape)
                    )
                    Text(
                        text = "$name: $count ($percentage%)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
