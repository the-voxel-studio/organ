package fr.studio.voxel.organ.ui.organ.form

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import fr.studio.voxel.organ.R
import fr.studio.voxel.organ.ui.components.LoadingOverlay
import fr.studio.voxel.organ.ui.components.PrimaryButton
import fr.studio.voxel.organ.ui.components.BackToLink
import fr.studio.voxel.organ.ui.components.DangerZoneSection
import fr.studio.voxel.organ.ui.components.DeleteConfirmDialog
import fr.studio.voxel.organ.ui.header.Header
import fr.studio.voxel.organ.ui.organ.form.components.OrganFormDetailsSection
import fr.studio.voxel.organ.ui.organ.form.components.OrganFormMembersSection
import fr.studio.voxel.organ.ui.organ.form.components.OrganFormRolesSection
import fr.studio.voxel.organ.ui.organ.form.components.OrganFormVisualIdentitySection
import fr.studio.voxel.organ.ui.project.AccessDeniedScreen
import fr.studio.voxel.organ.viewmodel.OrganFormViewModel

import fr.studio.voxel.organ.ui.components.ShimmerBox

@Composable
fun OrganFormShimmer() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Header(navigateUp = {})
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Back link mock
        ShimmerBox(modifier = Modifier.width(150.dp).height(20.dp))
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Title mock
        ShimmerBox(modifier = Modifier.width(200.dp).height(32.dp))
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Form card mock
        ShimmerBox(modifier = Modifier.fillMaxWidth().height(400.dp), shape = RoundedCornerShape(24.dp))
    }
}

@Composable
fun OrganFormScreen(
    projectUuid: String,
    organUuid: String?,
    onBack: () -> Unit,
    onSuccess: (String, String) -> Unit,
    onDeleted: () -> Unit = {},
    viewModel: OrganFormViewModel = viewModel()
) {
    LaunchedEffect(projectUuid, organUuid) {
        viewModel.initOrgan(projectUuid, organUuid)
    }

    LaunchedEffect(viewModel.isSuccess) {
        if (viewModel.isSuccess) {
            viewModel.createdOrganUuid?.let { onSuccess(projectUuid, it) }
        }
    }

    var showDeleteDialog by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        if (viewModel.isLoading) {
            OrganFormShimmer()
        } else {
            if (viewModel.accessDenied) {
                AccessDeniedScreen(onBack = onBack)
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Header(navigateUp = onBack)


                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.Start
                    ) {
                        // Back link
                        BackToLink(
                            label = if (viewModel.isEdit) "Retour à l'Organ" else "Retour au Projet",
                            onClick = onBack,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = if (viewModel.isEdit) "Paramètres de l'Organ" else "Créer un nouvel Organ",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        viewModel.errorMessage?.let { error ->
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = error,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Form card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                verticalArrangement = Arrangement.spacedBy(24.dp)
                            ) {
                                // Section 1: Details
                                OrganFormDetailsSection(
                                    title = viewModel.title,
                                    onTitleChange = { if (viewModel.canEditInfo) viewModel.title = it },
                                    description = viewModel.description,
                                    onDescriptionChange = { if (viewModel.canEditInfo) viewModel.description = it },
                                    highlightColor = viewModel.selectedColor
                                )

                                // Section 2: Visual identity
                                val context = LocalContext.current
                                OrganFormVisualIdentitySection(
                                    selectedColor = viewModel.selectedColor,
                                    onColorSelected = { if (viewModel.canEditInfo) viewModel.selectedColor = it },
                                    selectedIconTab = viewModel.selectedIconTab,
                                    onTabSelected = { if (viewModel.canEditInfo) viewModel.selectedIconTab = it },
                                    selectedImageUri = if (viewModel.selectedImageUri != null) viewModel.selectedImageUri else viewModel.selectedImageUriString.ifBlank { null },
                                    onImageSelected = { if (viewModel.canEditInfo) viewModel.updateImageUri(context, it) },
                                    selectedEmoji = viewModel.selectedEmoji,
                                    onEmojiChanged = { if (viewModel.canEditInfo) viewModel.selectedEmoji = it },
                                    selectedSvgCode = viewModel.selectedSvgCode,
                                    onSvgChanged = { if (viewModel.canEditInfo) viewModel.selectedSvgCode = it }
                                )

                                // Section 3: Roles Configuration
                                OrganFormRolesSection(
                                    roles = viewModel.roles,
                                    presets = viewModel.presets,
                                    organPermissions = viewModel.organPermissions,
                                    taskPermissions = viewModel.taskPermissions,
                                    interactionPermissions = viewModel.interactionPermissions,
                                    highlightColor = viewModel.selectedColor,
                                    canManage = viewModel.canManageRoles,
                                    onRolesChanged = { viewModel.updateRoles(it) }
                                )

                                // Section 4: Members management
                                OrganFormMembersSection(
                                    addedMembers = viewModel.addedMembers,
                                    projectMembers = viewModel.projectMembers,
                                    roles = viewModel.roles,
                                    highlightColor = viewModel.selectedColor,
                                    canManage = viewModel.canManageMembers,
                                    onMembersChanged = { viewModel.addedMembers = it }
                                )
                            }
                        }

                        // Action buttons
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 24.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = onBack,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp)
                            ) {
                                Text("Annuler", fontWeight = FontWeight.Bold)
                            }

                            PrimaryButton(
                                text = if (viewModel.isEdit) "Mettre à jour" else "Créer l'Organ",
                                onClick = { viewModel.submit() },
                                modifier = Modifier
                                    .weight(1.5f)
                                    .height(50.dp)
                            )
                        }

                        if (viewModel.isEdit) {
                            Spacer(modifier = Modifier.height(24.dp))
                            DangerZoneSection(
                                title = "Zone de Danger",
                                description = "Une fois supprimé, cet Organ et toutes ses données associées seront déplacés dans la corbeille.",
                                buttonText = "Supprimer l'Organ",
                                onDeleteClick = { showDeleteDialog = true }
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                        } else {
                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        DeleteConfirmDialog(
            title = "Supprimer l'Organ",
            message = "Voulez-vous supprimer l'Organ \"${viewModel.title}\" ? Cette action déplacera l'Organ et ses tâches associées vers la corbeille.",
            onConfirm = {
                showDeleteDialog = false
                viewModel.deleteOrgan(
                    onSuccess = {
                        onDeleted()
                    },
                    onError = {}
                )
            },
            onDismiss = { showDeleteDialog = false }
        )
    }
}
