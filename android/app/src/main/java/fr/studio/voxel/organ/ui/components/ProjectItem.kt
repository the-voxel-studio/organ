package fr.studio.voxel.organ.ui.components


import androidx.compose.foundation.layout.Column
import androidx.compose.material3.ButtonDefaults

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

import fr.studio.voxel.organ.domain.model.Project

@Composable
fun ProjectItem(
    project: Project,
    isSelected: Boolean,
    onClick: () -> Unit
) {

    Column {
        SecondaryButton(
            text = project.name ,
            isSelected = isSelected,
            onClick = onClick
        )
    }
}