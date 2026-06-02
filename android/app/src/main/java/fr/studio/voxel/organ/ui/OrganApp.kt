package fr.studio.voxel.organ.ui

import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import fr.studio.voxel.organ.data.NotificationRepository
import fr.studio.voxel.organ.data.UserRepository
import fr.studio.voxel.organ.ui.authentication.AuthMode
import fr.studio.voxel.organ.ui.authentication.AuthScreen
import fr.studio.voxel.organ.ui.dashboard.Dashboard
import fr.studio.voxel.organ.ui.notification.NotificationScreen
import fr.studio.voxel.organ.ui.organ.form.OrganFormScreen
import fr.studio.voxel.organ.ui.organ.details.OrganDetailsScreen
import fr.studio.voxel.organ.ui.parameter.details.Parameter
import fr.studio.voxel.organ.ui.project.details.ProjectDetailsScreen
import fr.studio.voxel.organ.ui.project.form.ProjectFormScreen
import fr.studio.voxel.organ.ui.sidebar.SideBar
import fr.studio.voxel.organ.viewmodel.ProjectDetailsViewModel
import fr.studio.voxel.organ.viewmodel.OrganDetailsViewModel
import fr.studio.voxel.organ.viewmodel.DashboardViewModel
import fr.studio.voxel.organ.ui.task.TaskDetailsScreen

enum class OrganScreen {
    SignIn,
    SignUp,
    Dashboard,
    Sidebar,
    Project,
    CreateProject,
    EditProject,
    Organ,
    CreateOrgan,
    EditOrgan,
    Task,
    Parameter,
    Notification
}

@Composable
fun OrganApp(
    navController: NavHostController = rememberNavController()
) {
    val currentUser = UserRepository.currentUser
    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            NotificationRepository.loadNotifications()
            NotificationRepository.setupMercure()
        } else {
            NotificationRepository.clear()
        }
    }

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

            composable(route = OrganScreen.Dashboard.name) { backStackEntry ->
                val viewModel: DashboardViewModel = viewModel()
                val shouldRefresh by backStackEntry.savedStateHandle.getStateFlow("refresh_dashboard", false).collectAsState()
                LaunchedEffect(shouldRefresh) {
                    if (shouldRefresh) {
                        viewModel.loadDashboard()
                        backStackEntry.savedStateHandle["refresh_dashboard"] = false
                    }
                }
                Dashboard(navController = navController, dashboardVM = viewModel)
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

            composable(route = OrganScreen.Notification.name) {
                NotificationScreen(navController = navController)
            }

            composable(
                route = "${OrganScreen.Project.name}/{projectUuid}",
                arguments = listOf(navArgument("projectUuid") { type = NavType.StringType })
            ) { backStackEntry ->
                val projectUuid = backStackEntry.arguments?.getString("projectUuid") ?: ""
                val viewModel: ProjectDetailsViewModel = viewModel()
                val shouldRefresh by backStackEntry.savedStateHandle.getStateFlow("refresh_project", false).collectAsState()

                LaunchedEffect(shouldRefresh) {
                    if (shouldRefresh) {
                        viewModel.loadProjectDetails(projectUuid)
                        backStackEntry.savedStateHandle["refresh_project"] = false
                    }
                }

                ProjectDetailsScreen(
                    projectUuid = projectUuid,
                    onSidebarClick = { navController.navigate(OrganScreen.Sidebar.name) },
                    onBack = { navController.popBackStack() },
                    onEditProject = { uuid -> navController.navigate("${OrganScreen.EditProject.name}/$uuid") },
                    onCreateOrgan = { uuid -> navController.navigate("${OrganScreen.CreateOrgan.name}/$uuid") },
                    onOrganClick = { organUuid -> navController.navigate("${OrganScreen.Organ.name}/$projectUuid/$organUuid") },
                    viewModel = viewModel
                )
            }

            composable(route = OrganScreen.CreateProject.name) {
                ProjectFormScreen(
                    projectUuid = null,
                    onBack = { navController.popBackStack() },
                    onSuccess = { navController.popBackStack() }
                )
            }

            composable(
                route = "${OrganScreen.EditProject.name}/{projectUuid}",
                arguments = listOf(navArgument("projectUuid") { type = NavType.StringType })
            ) { backStackEntry ->
                val projectUuid = backStackEntry.arguments?.getString("projectUuid")
                ProjectFormScreen(
                    projectUuid = projectUuid,
                    onBack = { navController.popBackStack() },
                    onSuccess = {
                        navController.previousBackStackEntry?.savedStateHandle?.set("refresh_project", true)
                        navController.popBackStack()
                    }
                )
            }

            composable(
                route = "${OrganScreen.CreateOrgan.name}/{projectUuid}",
                arguments = listOf(navArgument("projectUuid") { type = NavType.StringType })
            ) { backStackEntry ->
                val projectUuid = backStackEntry.arguments?.getString("projectUuid") ?: ""
                OrganFormScreen(
                    projectUuid = projectUuid,
                    organUuid = null,
                    onBack = { navController.popBackStack() },
                    onSuccess = {
                        navController.previousBackStackEntry?.savedStateHandle?.set("refresh_project", true)
                        navController.popBackStack()
                    }
                )
            }

            composable(
                route = "${OrganScreen.EditOrgan.name}/{projectUuid}/{organUuid}",
                arguments = listOf(
                    navArgument("projectUuid") { type = NavType.StringType },
                    navArgument("organUuid") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val projectUuid = backStackEntry.arguments?.getString("projectUuid") ?: ""
                val organUuid = backStackEntry.arguments?.getString("organUuid")
                OrganFormScreen(
                    projectUuid = projectUuid,
                    organUuid = organUuid,
                    onBack = { navController.popBackStack() },
                    onSuccess = {
                        navController.previousBackStackEntry?.savedStateHandle?.set("refresh_project", true)
                        navController.previousBackStackEntry?.savedStateHandle?.set("refresh_organ", true)
                        navController.popBackStack()
                    }
                )
            }

            composable(
                route = "${OrganScreen.Organ.name}/{projectUuid}/{organUuid}",
                arguments = listOf(
                    navArgument("projectUuid") { type = NavType.StringType },
                    navArgument("organUuid") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val projectUuid = backStackEntry.arguments?.getString("projectUuid") ?: ""
                val organUuid = backStackEntry.arguments?.getString("organUuid") ?: ""
                val viewModel: OrganDetailsViewModel = viewModel()
                val shouldRefresh by backStackEntry.savedStateHandle.getStateFlow("refresh_organ", false).collectAsState()

                LaunchedEffect(shouldRefresh) {
                    if (shouldRefresh) {
                        viewModel.loadOrganDetails()
                        backStackEntry.savedStateHandle["refresh_organ"] = false
                    }
                }

                OrganDetailsScreen(
                    projectUuid = projectUuid,
                    organUuid = organUuid,
                    onBack = { navController.popBackStack() },
                    onEditOrgan = { projUuid, orgUuid ->
                        navController.navigate("${OrganScreen.EditOrgan.name}/$projUuid/$orgUuid")
                    },
                    onSidebarClick = { navController.navigate(OrganScreen.Sidebar.name) },
                    onTaskClick = { taskUuid ->
                        navController.navigate("${OrganScreen.Task.name}/$projectUuid/$organUuid/$taskUuid")
                    },
                    viewModel = viewModel
                )
            }

            composable(
                route = "${OrganScreen.Task.name}/{projectUuid}/{organUuid}/{taskUuid}",
                arguments = listOf(
                    navArgument("projectUuid") { type = NavType.StringType },
                    navArgument("organUuid") { type = NavType.StringType },
                    navArgument("taskUuid") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val projectUuid = backStackEntry.arguments?.getString("projectUuid") ?: ""
                val organUuid = backStackEntry.arguments?.getString("organUuid") ?: ""
                val taskUuid = backStackEntry.arguments?.getString("taskUuid") ?: ""
                
                TaskDetailsScreen(
                    projectUuid = projectUuid,
                    organUuid = organUuid,
                    taskUuid = taskUuid,
                    onBack = {
                        navController.previousBackStackEntry?.savedStateHandle?.set("refresh_organ", true)
                        navController.previousBackStackEntry?.savedStateHandle?.set("refresh_dashboard", true)
                        try {
                            navController.getBackStackEntry(OrganScreen.Dashboard.name).savedStateHandle.set("refresh_dashboard", true)
                        } catch (e: Exception) {}
                        navController.popBackStack()
                    },
                    onSuccess = {
                        navController.previousBackStackEntry?.savedStateHandle?.set("refresh_organ", true)
                        navController.previousBackStackEntry?.savedStateHandle?.set("refresh_dashboard", true)
                        try {
                            navController.getBackStackEntry(OrganScreen.Dashboard.name).savedStateHandle.set("refresh_dashboard", true)
                        } catch (e: Exception) {}
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}