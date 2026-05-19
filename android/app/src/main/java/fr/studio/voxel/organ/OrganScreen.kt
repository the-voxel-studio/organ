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
import fr.studio.voxel.organ.ui.AuthMode
import fr.studio.voxel.organ.ui.AuthScreen
import fr.studio.voxel.organ.ui.SideBar
import fr.studio.voxel.organ.ui.Dashboard


enum class OrganScreen {
    SignIn,
    SignUp,
    Dashboard,
    Sidebar,
    Project,
    Organ,
    Task
}

@Composable
fun OrganApp(
    navController: NavHostController = rememberNavController()
){
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
                    onModeSwitch = { targetMode ->
                        if (targetMode == AuthMode.SIGN_UP) {
                            navController.navigate(OrganScreen.SignUp.name)
                        }
                    }
                )
            }

            composable(
                route = OrganScreen.SignUp.name
            ){
                AuthScreen(
                    mode = AuthMode.SIGN_UP,
                    onModeSwitch = { targetMode ->
                        if (targetMode == AuthMode.SIGN_IN) {
                            // On revient en arrière ou on force la route de connexion
                            navController.popBackStack()
                        }
                    }
                )
            }

            composable(
                route = OrganScreen.Sidebar.name,
                enterTransition = { slideInHorizontally(initialOffsetX = { -it }) },
                exitTransition = { slideOutHorizontally(targetOffsetX = { -it }) }
            ){
                SideBar(navController = navController, utilisateur = "Michel JeTestLeNomLongEncorePlusLongCarFautTronquer")

            }

            composable(route = OrganScreen.Dashboard.name){
                Dashboard(navController = navController)
            }
        }

    }
}