package fr.studio.voxel.organ

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import androidx.compose.foundation.layout.padding
import androidx.navigation.compose.composable
import fr.studio.voxel.organ.ui.SideBar


enum class OrganScreen() {
    LogIn,
    Register,
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
            startDestination = OrganScreen.Sidebar.name
        ) {
            composable(route = OrganScreen.Sidebar.name){
                SideBar()
            }
        }

    }
}