package fr.studio.voxel.organ.ui.project.stats.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp

@Composable
fun AuditPicActivityChart(
    activityData: List<Pair<String, Int>>,
    highlightColor: Color,
    modifier: Modifier = Modifier
) {
    if (activityData.isEmpty()) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text("Aucune donnée d'activité", color = Color.Gray)
        }
        return
    }

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        val paddingLeft = 30.dp.toPx()
        val paddingBottom = 20.dp.toPx()
        val graphWidth = width - paddingLeft
        val graphHeight = height - paddingBottom

        // Draw grid
        for (i in 0..3) {
            val y = graphHeight * i / 3
            drawLine(
                color = Color(0xFFF1F5F9),
                start = Offset(paddingLeft, y),
                end = Offset(width, y),
                strokeWidth = 1.dp.toPx()
            )
        }

        val maxVal = maxOf(5f, activityData.maxOf { it.second.toFloat() })
        val pointsCount = activityData.size
        val stepX = if (pointsCount > 1) graphWidth / (pointsCount - 1) else graphWidth

        val path = Path()

        for (i in activityData.indices) {
            val (_, count) = activityData[i]
            val x = paddingLeft + (i * stepX)
            val y = graphHeight - (count.toFloat() / maxVal * graphHeight)

            if (i == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }

            drawCircle(color = highlightColor, radius = 3.dp.toPx(), center = Offset(x, y))
        }

        drawPath(path = path, color = highlightColor, style = Stroke(width = 2.dp.toPx()))

        val textPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.GRAY
            textSize = 9.dp.toPx()
        }

        // Min/Max text labels
        drawContext.canvas.nativeCanvas.drawText(maxVal.toInt().toString(), 5.dp.toPx(), 10.dp.toPx(), textPaint)
        drawContext.canvas.nativeCanvas.drawText("0", 5.dp.toPx(), graphHeight - 2.dp.toPx(), textPaint)

        // Draw X Labels
        if (activityData.isNotEmpty()) {
            val labelIndices = setOf(0, activityData.size / 2, activityData.size - 1)

            for (idx in labelIndices) {
                if (idx < activityData.size) {
                    val dateStr = activityData[idx].first
                    val label = if (dateStr.length >= 10 && dateStr[4] == '-' && dateStr[7] == '-') {
                        "${dateStr.substring(8, 10)}/${dateStr.substring(5, 7)}"
                    } else {
                        dateStr
                    }
                    val x = paddingLeft + (idx * stepX)
                    drawContext.canvas.nativeCanvas.drawText(
                        label,
                        x - 10.dp.toPx(),
                        height - 4.dp.toPx(),
                        textPaint
                    )
                }
            }
        }
    }
}
