package fr.studio.voxel.organ.ui.components


import androidx.compose.foundation.layout.Column

import androidx.compose.runtime.Composable

import fr.studio.voxel.organ.ViewModel.Project

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