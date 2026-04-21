package fr.studio.voxel.organ.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import fr.studio.voxel.organ.domain.model.Project

@Composable
fun ProjectItem(project: Project) {

    var expanded by remember { mutableStateOf(false) }

    Column {

        Text(
            text = project.name,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.clickable {
                expanded = !expanded
            }
        )

        if (expanded) {
            project.organs.forEach { organ ->
                Text(
                    text = "- ${organ.name}",
                    modifier = Modifier.padding(start = 16.dp)
                )
            }
        }
    }
}