package fr.studio.voxel.organ.ui.organ

import androidx.compose.runtime.Composable

@Deprecated("Use fr.studio.voxel.organ.ui.organ.details.OrganDetailsScreen instead")
@Composable
fun OrganDetailsScreen(
    projectUuid: String,
    organUuid: String,
    onBack: () -> Unit,
    onEditOrgan: (String, String) -> Unit,
    onSidebarClick: () -> Unit,
    onTaskClick: (String) -> Unit = {},
    viewModel: fr.studio.voxel.organ.viewmodel.OrganDetailsViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    fr.studio.voxel.organ.ui.organ.details.OrganDetailsScreen(
        projectUuid = projectUuid,
        organUuid = organUuid,
        onBack = onBack,
        onEditOrgan = onEditOrgan,
        onSidebarClick = onSidebarClick,
        onTaskClick = onTaskClick,
        viewModel = viewModel
    )
}
