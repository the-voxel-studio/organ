package fr.studio.voxel.organ.ui.components.DashboardComponents

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import fr.studio.voxel.organ.domain.model.MainViewModel
import fr.studio.voxel.organ.domain.model.Project
import fr.studio.voxel.organ.domain.model.Task
import fr.studio.voxel.organ.ui.theme.MaterialColorScheme

@Composable
fun PriorityTask(
    mainVM: MainViewModel = viewModel(),
    modifier : Modifier = Modifier
){
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .border(1.dp, MaterialTheme.colorScheme.onSurface, RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Tâche",
                color = MaterialTheme.colorScheme.outline,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 22.sp
                )
            )

            Text(
                text = "Echéance",
                color = MaterialTheme.colorScheme.outline,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 22.sp
                )
            )

            Text(
                text = "Projet",
                color = MaterialTheme.colorScheme.outline,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 22.sp
                )
            )
        }

        Spacer(Modifier.height(24.dp))

        val projects = mainVM.projects ?: emptyList()
        val tasks = mainVM.tasks
        if(tasks.isNotEmpty()){
            tasks.forEach { task ->
                LineTask(task = task, projects = projects)
            }
        }

    }

}