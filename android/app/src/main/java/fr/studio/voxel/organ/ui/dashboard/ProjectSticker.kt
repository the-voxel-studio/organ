package fr.studio.voxel.organ.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
    projectsList: List<Project>
){
    Column(
    ) {
        projectsList.forEach {
                project ->
            Column(
                modifier = modifier
                    .padding(16.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.onSurface,RoundedCornerShape(16.dp))
                    .clickable(onClick = {/*TODO*/})
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val projectColor = try {
                        val colorStr = project.color.removePrefix("0x").removePrefix("#")

                        val parseStr = if (colorStr.length == 6) "FF$colorStr" else colorStr

                        Color(parseStr.toLong(16))
                    } catch (e: Exception) {
                        MaterialTheme.colorScheme.primary
                    }

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
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 1.5.em
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                )
                {
                    val creationText = "Crée le "

                    Text(
                        text = creationText.uppercase(),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.outline
                    )

                    val dateCreation = project.dateCreation.take(10)

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