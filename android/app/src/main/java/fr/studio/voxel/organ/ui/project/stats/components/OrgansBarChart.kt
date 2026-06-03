package fr.studio.voxel.organ.ui.project.stats.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.studio.voxel.organ.network.services.CurrentOrganStatusStat
import java.util.Locale

@Composable
fun OrgansBarChart(
    current: List<CurrentOrganStatusStat>,
    highlightColor: Color,
    modifier: Modifier = Modifier
) {
    val organStats = remember(current) {
        val map = mutableMapOf<String, Pair<Int, Double>>()
        current.forEach { item ->
            val name = item.organName ?: "Sans Organ"
            val prev = map[name] ?: Pair(0, 0.0)
            map[name] = Pair(prev.first + item.taskCount, prev.second + item.totalHours)
        }
        map.toList().filter { it.second.first > 0 || it.second.second > 0 }.take(5)
    }

    if (organStats.isEmpty()) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text("Aucune donnée d'Organ", color = Color.Gray)
        }
        return
    }

    Column(modifier = modifier) {
        Canvas(modifier = Modifier.weight(1f).fillMaxWidth()) {
            val width = size.width
            val height = size.height

            val paddingLeft = 70.dp.toPx()
            val paddingBottom = 10.dp.toPx()
            val graphWidth = width - paddingLeft - 50.dp.toPx()
            val graphHeight = height - paddingBottom

            val barSpaceHeight = graphHeight / organStats.size
            val maxVal = maxOf(5f, organStats.maxOf { maxOf(it.second.first.toFloat(), it.second.second.toFloat()) })

            val titlePaint = android.graphics.Paint().apply {
                color = android.graphics.Color.BLACK
                textSize = 9.dp.toPx()
                textAlign = android.graphics.Paint.Align.RIGHT
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            }

            val valuePaint = android.graphics.Paint().apply {
                color = android.graphics.Color.GRAY
                textSize = 8.dp.toPx()
                textAlign = android.graphics.Paint.Align.LEFT
            }

            for (i in organStats.indices) {
                val (organName, data) = organStats[i]
                val tasks = data.first.toFloat()
                val hours = data.second.toFloat()

                val barCenterY = barSpaceHeight * i + barSpaceHeight / 2
                val barHeight = 8.dp.toPx()

                val widthTasks = (tasks / maxVal * graphWidth).coerceAtLeast(4.dp.toPx())
                val widthHours = (hours / maxVal * graphWidth).coerceAtLeast(4.dp.toPx())

                val displayTitle = if (organName.length > 12) organName.take(10) + ".." else organName
                drawContext.canvas.nativeCanvas.drawText(
                    displayTitle,
                    paddingLeft - 8.dp.toPx(),
                    barCenterY + 4.dp.toPx(),
                    titlePaint
                )

                // Tasks bar
                drawRoundRect(
                    color = highlightColor,
                    topLeft = Offset(paddingLeft, barCenterY - barHeight - 1.dp.toPx()),
                    size = Size(widthTasks, barHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx(), 4.dp.toPx())
                )
                drawContext.canvas.nativeCanvas.drawText(
                    "${tasks.toInt()} t.",
                    paddingLeft + widthTasks + 6.dp.toPx(),
                    barCenterY - 2.dp.toPx(),
                    valuePaint
                )

                // Hours bar
                drawRoundRect(
                    color = Color(0xFFFF7DD4),
                    topLeft = Offset(paddingLeft, barCenterY + 1.dp.toPx()),
                    size = Size(widthHours, barHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx(), 4.dp.toPx())
                )
                val hoursFormatted = if (hours % 1 == 0f) hours.toInt().toString() else String.format(Locale.FRANCE, "%.1f", hours)
                drawContext.canvas.nativeCanvas.drawText(
                    "$hoursFormatted h.",
                    paddingLeft + widthHours + 6.dp.toPx(),
                    barCenterY + 8.dp.toPx(),
                    valuePaint
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(highlightColor, RoundedCornerShape(3.dp))
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Tâches actives",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )

            Spacer(modifier = Modifier.width(24.dp))

            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(Color(0xFFFF7DD4), RoundedCornerShape(3.dp))
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Heures estimées",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
        }
    }
}
