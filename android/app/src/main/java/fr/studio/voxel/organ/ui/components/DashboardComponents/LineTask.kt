package fr.studio.voxel.organ.ui.components.DashboardComponents

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import fr.studio.voxel.organ.ViewModel.Task
import fr.studio.voxel.organ.network.services.Project
import fr.studio.voxel.organ.ui.theme.MaterialColorScheme

@Composable
fun LineTask(
    task: Task,
    projects: List<Project>,
    modifier: Modifier = Modifier
){
    val project = projects.find{it.id == task.projectId} ?: return
    val organ = project.organs?.find{it.id == task.organId} ?: return

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .border(1.dp, MaterialColorScheme.onSurface, RoundedCornerShape(16.dp))
            .background(MaterialColorScheme.onSurface.copy(0.2f), RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ){
        Column(
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = task.progress.toString(),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }

            Text(
                text = task.name,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.width(70.dp)
            )
        }

        //Spacer(modifier = Modifier.width(20.dp))

        Text(
            text = task.deadline,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.onPrimary
        )

        //Spacer(modifier = Modifier.width(20.dp))

        Column(

        ) {
            Text(
                text = project.title ,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.width(80.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = organ.name,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.width(80.dp)
            )
        }


    }
}