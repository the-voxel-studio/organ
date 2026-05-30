package fr.studio.voxel.organ.ui

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
import fr.studio.voxel.organ.ui.authentication.AuthMode
import fr.studio.voxel.organ.ui.authentication.AuthScreen
import fr.studio.voxel.organ.ui.create.Create
import fr.studio.voxel.organ.ui.create.CreateMode
import fr.studio.voxel.organ.ui.dashboard.Dashboard
import fr.studio.voxel.organ.ui.parameter.Parameter
import fr.studio.voxel.organ.ui.sidebar.SideBar

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
) {
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
            ) {
                AuthScreen(
                    mode = AuthMode.SIGN_IN,
                    navController = navController,
                    onModeSwitch = { targetMode ->
                        if (targetMode == AuthMode.SIGN_UP) {
                            navController.navigate(OrganScreen.SignUp.name)
                        }
                    }
                )
            }

            composable(
                route = OrganScreen.SignUp.name
            ) {
                AuthScreen(
                    mode = AuthMode.SIGN_UP,
                    navController = navController,
                    onModeSwitch = { targetMode ->
                        if (targetMode == AuthMode.SIGN_IN) {
                            navController.popBackStack()
                        }
                    }
                )
            }

            composable(
                route = OrganScreen.Sidebar.name,
                enterTransition = { slideInHorizontally(initialOffsetX = { -it }) },
                exitTransition = { slideOutHorizontally(targetOffsetX = { -it }) }
            ) {
                SideBar(
                    navController = navController,
                    onModifButtonClicked = {
                        navController.navigate(OrganScreen.Parameter.name)
                    }
                )
            }

            composable(route = OrganScreen.Dashboard.name) {
                Dashboard(navController = navController)
            }

            composable(route = OrganScreen.Parameter.name) {
                Parameter(
                    navController = navController,
                    onLoggedOut = {
                        navController.navigate(OrganScreen.SignIn.name) {
                            popUpTo(0)
                        }
                    }
                )
            }

            composable(route = OrganScreen.CreateProject.name) {
                Create(mode = CreateMode.PROJECT)
            }

            composable(route = OrganScreen.CreateOrgan.name) {
                Create(mode = CreateMode.ORGAN)
            }
        }
    }
}