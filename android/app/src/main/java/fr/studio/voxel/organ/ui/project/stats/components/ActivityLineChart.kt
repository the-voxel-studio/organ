package fr.studio.voxel.organ.ui.project.stats.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import fr.studio.voxel.organ.network.services.DailyStatsHistory

@Composable
fun ActivityLineChart(
    history: List<DailyStatsHistory>,
    highlightColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        val paddingLeft = 40.dp.toPx()
        val paddingBottom = 30.dp.toPx()
        val graphWidth = width - paddingLeft
        val graphHeight = height - paddingBottom

        // Draw background grid lines
        val gridLinesCount = 4
        for (i in 0..gridLinesCount) {
            val y = graphHeight * i / gridLinesCount
            drawLine(
                color = Color(0xFFF1F5F9),
                start = Offset(paddingLeft, y),
                end = Offset(width, y),
                strokeWidth = 1.dp.toPx()
            )
        }

        // Get max value for scaling (min 5 to avoid flat charts)
        val maxVal = maxOf(
            5,
            history.maxOf { maxOf(it.tasksCreated, it.tasksCompleted, it.consultations) }
        ).toFloat()

        val pointsCount = history.size
        val stepX = if (pointsCount > 1) graphWidth / (pointsCount - 1) else graphWidth

        val pathCreated = Path()
        val pathCompleted = Path()
        val pathConsultations = Path()

        for (i in history.indices) {
            val h = history[i]
            val x = paddingLeft + (i * stepX)

            val yCreated = graphHeight - (h.tasksCreated.toFloat() / maxVal * graphHeight)
            val yCompleted = graphHeight - (h.tasksCompleted.toFloat() / maxVal * graphHeight)
            val yConsultations = graphHeight - (h.consultations.toFloat() / maxVal * graphHeight)

            if (i == 0) {
                pathCreated.moveTo(x, yCreated)
                pathCompleted.moveTo(x, yCompleted)
                pathConsultations.moveTo(x, yConsultations)
            } else {
                pathCreated.lineTo(x, yCreated)
                pathCompleted.lineTo(x, yCompleted)
                pathConsultations.lineTo(x, yConsultations)
            }

            if (pointsCount < 15 || i % (pointsCount / 5).coerceAtLeast(1) == 0 || i == pointsCount - 1) {
                drawCircle(color = Color(0xFFFF7DD4), radius = 3.dp.toPx(), center = Offset(x, yCreated))
                drawCircle(color = highlightColor, radius = 3.dp.toPx(), center = Offset(x, yCompleted))
                drawCircle(color = Color(0xFF94A3B8), radius = 3.dp.toPx(), center = Offset(x, yConsultations))
            }
        }

        // Draw Lines
        drawPath(path = pathCreated, color = Color(0xFFFF7DD4), style = Stroke(width = 2.5.dp.toPx()))
        drawPath(path = pathCompleted, color = highlightColor, style = Stroke(width = 2.5.dp.toPx()))
        drawPath(path = pathConsultations, color = Color(0xFF94A3B8), style = Stroke(width = 1.5.dp.toPx(), pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)))

        val textPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.GRAY
            textSize = 10.dp.toPx()
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        
        // Draw Y Axis Labels
        drawContext.canvas.nativeCanvas.drawText(maxVal.toInt().toString(), 5.dp.toPx(), 12.dp.toPx(), textPaint)
        drawContext.canvas.nativeCanvas.drawText("0", 5.dp.toPx(), graphHeight - 4.dp.toPx(), textPaint)

        // Draw X Labels (first, middle, last dates)
        if (history.isNotEmpty()) {
            val labelIndices = setOf(0, history.size / 2, history.size - 1)

            for (idx in labelIndices) {
                if (idx < history.size) {
                    val dateStr = history[idx].date
                    val label = if (dateStr.length >= 10 && dateStr[4] == '-' && dateStr[7] == '-') {
                        "${dateStr.substring(8, 10)}/${dateStr.substring(5, 7)}"
                    } else {
                        dateStr
                    }
                    val x = paddingLeft + (idx * stepX)
                    drawContext.canvas.nativeCanvas.drawText(
                        label,
                        x - 15.dp.toPx(),
                        height - 8.dp.toPx(),
                        textPaint
                    )
                }
            }
        }
    }
}
