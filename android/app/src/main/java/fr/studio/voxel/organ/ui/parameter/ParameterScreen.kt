package fr.studio.voxel.organ.ui.parameter

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController

@Deprecated("Use fr.studio.voxel.organ.ui.parameter.details.Parameter instead")
@Composable
fun Parameter(
    navController: NavHostController,
    parameterVM: fr.studio.voxel.organ.viewmodel.ParameterViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    onLoggedOut: () -> Unit = {}
) {
    fr.studio.voxel.organ.ui.parameter.details.Parameter(
        navController = navController,
        parameterVM = parameterVM,
        onLoggedOut = onLoggedOut
    )
}