package fr.studio.voxel.organ.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import fr.studio.voxel.organ.network.services.Project

@Composable
fun ProjectSticker(
    modifier : Modifier = Modifier,
    projectsList: List<Project>,
    onProjectClick: (String) -> Unit = {}
) {
    Column {
        projectsList.forEach { project ->
            val projectColor = try {
                val colorStr = project.color?.removePrefix("0x")?.removePrefix("#") ?: ""
                val parseStr = if (colorStr.length == 6) "FF$colorStr" else colorStr
                Color(parseStr.toLong(16))
            } catch (e: Exception) {
                MaterialTheme.colorScheme.primary
            }

            Row(
                modifier = modifier
                    .padding(vertical = 8.dp)
                    .fillMaxWidth()
                    .height(IntrinsicSize.Max)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                    .clickable(onClick = { onProjectClick(project.uuid) })
            ) {
                // Colored left bar representing the side border effect in Angular
                Box(
                    modifier = Modifier
                        .width(6.dp)
                        .fillMaxHeight()
                        .background(projectColor)
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        ProjectIconBadge(
                            visual = project.visual,
                            projectColor = projectColor
                        )

                        ProjectStateSticker(state = project.state, color = projectColor)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = project.title,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 1.5.em
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = project.description ?: "Aucune description",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        lineHeight = 1.5.em
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val creationText = "Créé le "
                        Text(
                            text = creationText.uppercase(),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.outline
                        )

                        val dateCreation = project.dateCreation?.take(10) ?: ""
                        Text(
                            text = dateCreation,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}