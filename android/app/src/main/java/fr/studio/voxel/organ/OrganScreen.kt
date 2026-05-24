package fr.studio.voxel.organ

import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import androidx.compose.foundation.layout.padding
import androidx.navigation.compose.composable
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import fr.studio.voxel.organ.ViewModel.MainViewModel
import fr.studio.voxel.organ.network.ApiClient
import fr.studio.voxel.organ.ui.AuthMode
import fr.studio.voxel.organ.ui.AuthScreen
import fr.studio.voxel.organ.ui.CreateProject
import fr.studio.voxel.organ.ui.SideBar
import fr.studio.voxel.organ.ui.Dashboard
import fr.studio.voxel.organ.ui.Parameter


enum class OrganScreen {
    SignIn,
    SignUp,
    Dashboard,
    Sidebar,
    Project,
    CreateProject,
    Organ,
    CreateOrgan,
    Task,
    Parameter,
    Notification
}

@Composable
fun OrganApp(
    navController: NavHostController = rememberNavController()
){
    val context = LocalContext.current
    val mainVM: MainViewModel = viewModel(
        viewModelStoreOwner = context as MainActivity
    )

    Scaffold(
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        NavHost(
            navController = navController,
            modifier = Modifier.padding(innerPadding),
            startDestination = OrganScreen.SignIn.name
        ) {
            composable(
                route = OrganScreen.SignIn.name
            ){
                AuthScreen(
                    mode = AuthMode.SIGN_IN,
                    navController = navController,
                    onModeSwitch = { targetMode ->
                        if (targetMode == AuthMode.SIGN_UP) {
                            navController.navigate(OrganScreen.SignUp.name)
                        }
                    },
                    mainVM = mainVM
                )
            }

            composable(
                route = OrganScreen.SignUp.name
            ){
                AuthScreen(
                    mode = AuthMode.SIGN_UP,
                    navController = navController,
                    onModeSwitch = { targetMode ->
                        if (targetMode == AuthMode.SIGN_IN) {
                            // On revient en arrière ou on force la route de connexion
                            navController.popBackStack()
                        }
                    },
                    mainVM = mainVM
                )
            }

            composable(
                route = OrganScreen.Sidebar.name,
                enterTransition = { slideInHorizontally(initialOffsetX = { -it }) },
                exitTransition = { slideOutHorizontally(targetOffsetX = { -it }) }
            ){
                SideBar(
                    navController = navController,
                    mainVM = mainVM,
                    onModifButtonClicked = {
                        navController.navigate(OrganScreen.Parameter.name)
                    }
                )

            }

            composable(route = OrganScreen.Dashboard.name){
                Dashboard(navController = navController, mainVM = mainVM)
            }

            composable (route = OrganScreen.Parameter.name){
                Parameter(
                    onDeleteButtonClicked = {
                        ApiClient.getTokenStorage().clear()
                        mainVM.clearData()
                        navController.navigate(OrganScreen.SignIn.name){
                            popUpTo(0)
                        }
                    }
                )
            }

            composable (route = OrganScreen.CreateProject.name){
                CreateProject()
            }
        }

    }
}