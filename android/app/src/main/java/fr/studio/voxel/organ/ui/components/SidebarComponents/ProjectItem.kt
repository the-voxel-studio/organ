package fr.studio.voxel.organ.ui.components.SidebarComponents


import androidx.compose.foundation.layout.Column

import androidx.compose.runtime.Composable

import fr.studio.voxel.organ.network.services.Project

@Composable
fun ProjectItem(
    project: Project,
    isSelected: Boolean,
    onClick: () -> Unit
) {

    Column {
        SecondaryButton(
            text = project.title,
            isSelected = isSelected,
            onClick = onClick
        )
    }
}