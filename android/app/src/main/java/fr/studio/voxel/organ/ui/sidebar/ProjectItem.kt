package fr.studio.voxel.organ.ui.sidebar

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import fr.studio.voxel.organ.network.services.Project
import fr.studio.voxel.organ.ui.dashboard.ProjectIconBadge

@Composable
fun ProjectItem(
    project: Project,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val projectColor = try {
        val colorStr = project.color?.removePrefix("0x")?.removePrefix("#") ?: ""
        val parseStr = if (colorStr.length == 6) "FF$colorStr" else colorStr
        Color(parseStr.toLong(16))
    } catch (e: Exception) {
        MaterialTheme.colorScheme.primary
    }

    Column {
        SecondaryButton(
            text = project.title,
            isSelected = isSelected,
            onClick = onClick,
            projectColor = projectColor,
            icon = {
                ProjectIconBadge(
                    visual = project.visual,
                    projectColor = projectColor,
                    badgeSize = 32.dp,
                    iconSize = 20.dp
                )
            }
        )
    }
}