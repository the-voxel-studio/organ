package fr.studio.voxel.organ.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import fr.studio.voxel.organ.domain.model.Task
import fr.studio.voxel.organ.ui.theme.AppColorScheme
import fr.studio.voxel.organ.ui.theme.AppTypography

@Composable
fun TaskItem(
    task: Task,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .border(2.dp, AppColorScheme.onSurface)
            .background(AppColorScheme.surface)
            .padding(12.dp)
    ) {

        Column {
            // Ligne du haut : progression + bouton 6 points
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${task.progress}/10",
                    style = AppTypography.labelLarge,
                    color = AppColorScheme.onSurface
                )

                // Bouton 6 points (drag futur)
                Column {
                    repeat(3) {
                        Row {
                            repeat(2) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .padding(1.dp)
                                        .background(AppColorScheme.onSurface.copy(alpha = 0.4f), CircleShape)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Nom de la tâche
            Text(
                text = task.name,
                style = AppTypography.bodyLarge,
                color = AppColorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Date limite alignée à droite
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = task.deadline,
                    style = AppTypography.labelSmall,
                    color = AppColorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}
