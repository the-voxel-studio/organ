package fr.studio.voxel.organ.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import fr.studio.voxel.organ.domain.model.Task
import fr.studio.voxel.organ.ui.theme.AppColorScheme
import fr.studio.voxel.organ.ui.theme.AppTypography

@Composable
fun TaskColumn(
    nameColumn : String,
    tasks: List<Task>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .border(2.dp, AppColorScheme.onSurface)
            .background(AppColorScheme.surface)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = nameColumn,
                style = AppTypography.titleLarge,
                color = AppColorScheme.onSurface
            )

            // Bouton 3 points
            Row {
                repeat(3) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .padding(2.dp)
                            .background(AppColorScheme.onSurface.copy(alpha = 0.4f), CircleShape)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 🔹 Liste des tâches
        tasks.forEach { task ->
            TaskItem(task = task)
            Spacer(modifier = Modifier.height(12.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Bouton Ajouter tâche
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(2.dp, AppColorScheme.onSurface.copy(alpha = 0.4f))
                .clickable {
                    // TODO: action ajouter tâche
                }
                .padding(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icône "+"
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .border(2.dp, AppColorScheme.onSurface, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "+",
                        style = AppTypography.labelLarge,
                        color = AppColorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = "Ajouter Tâche",
                    style = AppTypography.labelLarge,
                    color = AppColorScheme.onSurface
                )
            }
        }
    }
}
