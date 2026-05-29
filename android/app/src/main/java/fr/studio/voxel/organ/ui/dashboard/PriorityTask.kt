package fr.studio.voxel.organ.ui.dashboard

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.studio.voxel.organ.viewmodel.MainViewModel
import fr.studio.voxel.organ.viewmodel.sharedMainViewModel

@Composable
fun PriorityTask(
    mainVM: MainViewModel = sharedMainViewModel()
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
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
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
        val tasks = mainVM.priorityTask
        if(mainVM.isLoading && tasks.isEmpty()){
            Box(
                Modifier.fillMaxWidth().padding(32.dp),
                contentAlignment = Alignment.Center
            ){
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            }
        } else if(tasks.isNotEmpty()){
            tasks.forEach { task ->
                LineTask(task = task)
            }
        }else{
            Text("Aucune tâche urgente", modifier = Modifier.padding(16.dp))
        }


    }

}