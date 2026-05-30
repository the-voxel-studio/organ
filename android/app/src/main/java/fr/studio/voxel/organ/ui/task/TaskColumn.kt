package fr.studio.voxel.organ.ui.task

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import fr.studio.voxel.organ.R
import fr.studio.voxel.organ.network.services.Task
import fr.studio.voxel.organ.ui.theme.MaterialColorScheme
import fr.studio.voxel.organ.ui.theme.MaterialTypography

@Composable
fun TaskColumn(
    nameColumn : String,
    tasks: List<Task>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialColorScheme.onSurface.copy(alpha = 0.03f))
            .border(1.dp, MaterialColorScheme.onSurface.copy(alpha = 0.06f), RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = nameColumn,
                style = MaterialTypography.titleMedium,
                color = MaterialColorScheme.onSurface
            )

            TaskCountBadge(tasks.size)
        }

        Spacer(modifier = Modifier.height(32.dp))

        // 🔹 Liste des tâches
        tasks.forEach { task ->
            TaskItem(task = task)
            Spacer(modifier = Modifier.height(12.dp))
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Bouton Ajouter tâche
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialColorScheme.onSurface.copy(alpha = 0.02f))
                .border(1.dp, MaterialColorScheme.onSurface.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                .clickable {
                    // TODO: action ajouter tâche
                }
                .padding(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                // Icône "+"
                Icon(
                    painterResource(id = R.drawable.outline_add_circle_24),
                    contentDescription = "Logo ajouter tâche",
                    tint = MaterialColorScheme.onSurface,

                    modifier = Modifier
                        .size(32.dp)
                )

                Spacer(modifier = Modifier.width(24.dp))

                Text(
                    text = "Ajouter Tâche",
                    style = MaterialTypography.labelLarge,
                    color = MaterialColorScheme.onSurface
                )
            }
        }
    }
}
