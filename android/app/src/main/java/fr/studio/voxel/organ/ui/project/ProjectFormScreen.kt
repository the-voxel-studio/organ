package fr.studio.voxel.organ.ui.project

import androidx.compose.runtime.Composable

@Deprecated("Use fr.studio.voxel.organ.ui.project.form.ProjectFormScreen instead")
@Composable
fun ProjectFormScreen(
    projectUuid: String?,
    onBack: () -> Unit,
    onSuccess: (String) -> Unit,
    viewModel: fr.studio.voxel.organ.viewmodel.ProjectFormViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    fr.studio.voxel.organ.ui.project.form.ProjectFormScreen(
        projectUuid = projectUuid,
        onBack = onBack,
        onSuccess = onSuccess,
        viewModel = viewModel
    )
}
