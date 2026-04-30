package fr.studio.voxel.organ

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.res.dimensionResource
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


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrganAppBar(
    canOpenSidebar: Boolean,
    navigateUp: () -> Unit,
    modifier: Modifier = Modifier)
{
    TopAppBar(
        title = { Text(stringResource(id = R.string.app_name)) },
        colors = TopAppBarDefaults.mediumTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer),
        modifier = modifier,
        navigationIcon = {
            if (canOpenSidebar) {
                IconButton(onClick = { navigateUp }) {
                    Icon(Icons.Default.Menu, contentDescription = "Menu")
                }
            }
        }
    )
}

@Composable
fun OrganApp(
    navController: NavHostController = rememberNavController()
){
    Scaffold(
        topBar = {
            OrganAppBar(
                canOpenSidebar = false,
                navigateUp = { /* TODO */ }
            )
        }
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