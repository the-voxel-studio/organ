package fr.studio.voxel.organ.ui.project.details

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import fr.studio.voxel.organ.R
import fr.studio.voxel.organ.ui.header.Header
import fr.studio.voxel.organ.ui.project.details.components.*
import fr.studio.voxel.organ.viewmodel.ProjectDetailsViewModel
import fr.studio.voxel.organ.ui.components.ShimmerBox
import fr.studio.voxel.organ.ui.components.AddButton

@Composable
fun ProjectDetailsShimmer() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Header(navigateUp = {}, canOpenSidebar = false)
        
        Spacer(modifier = Modifier.height(20.dp))
        
        // Project Header Shimmer
        ShimmerBox(modifier = Modifier.fillMaxWidth().height(200.dp))
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Organs Section Shimmer
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            ShimmerBox(modifier = Modifier.width(150.dp).height(24.dp))
            ShimmerBox(modifier = Modifier.width(80.dp).height(24.dp))
        }
        Spacer(modifier = Modifier.height(16.dp))
        repeat(2) {
            ShimmerBox(modifier = Modifier.fillMaxWidth().height(120.dp))
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun ProjectDetailsScreen(
    projectUuid: String,
    onSidebarClick: () -> Unit,
    onBack: () -> Unit,
    onEditProject: (String) -> Unit,
    onCreateOrgan: (String) -> Unit,
    onOrganClick: (String) -> Unit,
    viewModel: ProjectDetailsViewModel = viewModel()
) {
    LaunchedEffect(projectUuid) {
        viewModel.initProject(projectUuid)
    }

    var showNotImplementedFeature by remember { mutableStateOf<String?>(null) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        if (viewModel.isLoading && viewModel.projectData == null) {
            ProjectDetailsShimmer()
        } else {
            if (viewModel.errorMessage != null) {
                ProjectErrorState(
                    message = viewModel.errorMessage ?: "",
                    onBack = onBack
                )
            } else {
                viewModel.projectData?.let { data ->
                    val projectColor = remember(data.project.color) {
                        try {
                            val hex = data.project.color?.replace("#", "") ?: "FF7EB6"
                            Color(android.graphics.Color.parseColor("#$hex"))
                        } catch (e: Exception) {
                            Color(0xFFFF7EB6)
                        }
                    }

                    val role = data.project.role ?: "MEMBER"
                    val canManage = role == "ADMIN" || role == "MANAGER"

                    Box(modifier = Modifier.fillMaxSize()) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 24.dp, vertical = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Header(navigateUp = onSidebarClick, canOpenSidebar = true)

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .verticalScroll(rememberScrollState()),
                                horizontalAlignment = Alignment.Start
                            ) {
                                Spacer(modifier = Modifier.height(20.dp))

                                // Project Header Info
                                ProjectHeaderSection(
                                    data = data,
                                    projectColor = projectColor,
                                    canManage = canManage,
                                    onEditClick = { onEditProject(projectUuid) },
                                    onFeaturePlaceholderClick = { showNotImplementedFeature = it }
                                )

                                Spacer(modifier = Modifier.height(32.dp))

                                // Organs Section
                                OrgansSection(
                                    organs = data.organs,
                                    projectUuid = projectUuid,
                                    projectColor = projectColor,
                                    canManage = canManage,
                                    showTagsPanel = viewModel.showTagsPanel,
                                    onToggleTagsClick = { viewModel.toggleTagsPanel() },
                                    onOrganClick = onOrganClick,
                                    tagsPanel = {
                                        AnimatedVisibility(
                                            visible = viewModel.showTagsPanel,
                                            enter = expandVertically() + fadeIn(),
                                            exit = shrinkVertically() + fadeOut()
                                        ) {
                                            ProjectTagsPanel(
                                                projectUuid = projectUuid,
                                                canManage = canManage,
                                                projectColor = projectColor,
                                                showTagsPanel = true,
                                                tags = viewModel.tags,
                                                isTagsLoading = viewModel.isTagsLoading,
                                                isSavingTag = viewModel.isSavingTag,
                                                tagErrorMessage = viewModel.tagErrorMessage,
                                                onCreateTag = { name, color -> viewModel.createTag(name, color) },
                                                onUpdateTag = { uuid, name, color -> viewModel.updateTag(uuid, name, color) },
                                                onDeleteTag = { uuid -> viewModel.deleteTag(uuid) }
                                            )
                                        }
                                    }
                                )

                                Spacer(modifier = Modifier.height(32.dp))

                                // About Section
                                AboutSection(
                                    description = data.project.description,
                                    createdAt = data.project.createdAt
                                )

                                Spacer(modifier = Modifier.height(32.dp))

                                // Activity Feed Section
                                ActivityFeedSection(
                                    activities = data.activities,
                                    projectColor = projectColor
                                )

                                Spacer(modifier = Modifier.height(80.dp))
                            }
                        }

                        if (canManage) {
                            AddButton(
                                containerColor = projectColor,
                                onClick = { onCreateOrgan(projectUuid) },
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(24.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // Modal popup matching Angular's "showNotImplementedAlert"
    showNotImplementedFeature?.let { featureName ->
        AlertDialog(
            onDismissRequest = { showNotImplementedFeature = null },
            title = {
                Text(
                    text = "Fonctionnalité en cours",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Text(
                    text = "La fonctionnalité \"$featureName\" n'est pas encore disponible dans cette version mobile. Elle sera transposée prochainement !"
                )
            },
            confirmButton = {
                TextButton(onClick = { showNotImplementedFeature = null }) {
                    Text("Compris")
                }
            }
        )
    }
}
