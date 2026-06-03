package fr.studio.voxel.organ.ui.project

import androidx.compose.runtime.Composable

@Deprecated("Use fr.studio.voxel.organ.ui.project.details.ProjectDetailsScreen instead")
@Composable
fun ProjectDetailsScreen(
    projectUuid: String,
    onSidebarClick: () -> Unit,
    onBack: () -> Unit,
    onEditProject: (String) -> Unit,
    onCreateOrgan: (String) -> Unit,
    onOrganClick: (String) -> Unit,
    onTrashClick: (String) -> Unit,
    viewModel: fr.studio.voxel.organ.viewmodel.ProjectDetailsViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    fr.studio.voxel.organ.ui.project.details.ProjectDetailsScreen(
        projectUuid = projectUuid,
        onSidebarClick = onSidebarClick,
        onBack = onBack,
        onEditProject = onEditProject,
        onCreateOrgan = onCreateOrgan,
        onOrganClick = onOrganClick,
        onTrashClick = onTrashClick,
        viewModel = viewModel
    )
}
