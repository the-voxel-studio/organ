package fr.studio.voxel.organ.ui.organ

import androidx.compose.runtime.Composable

@Deprecated("Use fr.studio.voxel.organ.ui.organ.form.OrganFormScreen instead")
@Composable
fun OrganFormScreen(
    projectUuid: String,
    organUuid: String?,
    onBack: () -> Unit,
    onSuccess: () -> Unit,
    viewModel: fr.studio.voxel.organ.viewmodel.OrganFormViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    fr.studio.voxel.organ.ui.organ.form.OrganFormScreen(
        projectUuid = projectUuid,
        organUuid = organUuid,
        onBack = onBack,
        onSuccess = onSuccess,
        viewModel = viewModel
    )
}
