package fr.studio.voxel.organ.ui.project.form

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import fr.studio.voxel.organ.ui.header.Header
import fr.studio.voxel.organ.ui.project.AccessDeniedScreen
import fr.studio.voxel.organ.ui.project.form.components.ProjectFormDetailsSection
import fr.studio.voxel.organ.ui.project.form.components.ProjectFormMembersSection
import fr.studio.voxel.organ.ui.project.form.components.ProjectFormVisualIdentitySection
import fr.studio.voxel.organ.viewmodel.ProjectFormViewModel

@Composable
fun ProjectFormScreen(
    projectUuid: String?,
    onBack: () -> Unit,
    onSuccess: () -> Unit,
    viewModel: ProjectFormViewModel = viewModel()
) {
    LaunchedEffect(projectUuid) {
        viewModel.initProject(projectUuid)
    }

    LaunchedEffect(viewModel.isSuccess) {
        if (viewModel.isSuccess) {
            onSuccess()
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        LoadingOverlay(
            isLoading = viewModel.isLoading,
            text = if (viewModel.isEdit) "Mise à jour du projet..." else "Création du projet..."
        ) {
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
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onBack() },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.flechedroite_logo),
                                contentDescription = "Retour",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier
                                    .size(20.dp)
                                    .rotate(180f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (viewModel.isEdit) "Retour au Projet" else "Retour au Dashboard",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = if (viewModel.isEdit) "Paramètres du Projet" else "Créer un Nouveau Projet",
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
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                verticalArrangement = Arrangement.spacedBy(24.dp)
                            ) {
                                // Section 1: Title, description, status
                                ProjectFormDetailsSection(
                                    title = viewModel.title,
                                    onTitleChange = { viewModel.title = it },
                                    description = viewModel.description,
                                    onDescriptionChange = { viewModel.description = it },
                                    status = viewModel.status,
                                    onStatusChange = { viewModel.status = it },
                                    isEdit = viewModel.isEdit
                                )

                                // Section 2: Visual identity (colors & icons)
                                val context = LocalContext.current
                                ProjectFormVisualIdentitySection(
                                    selectedColor = viewModel.selectedColor,
                                    onColorSelected = { viewModel.selectedColor = it },
                                    selectedIconTab = viewModel.selectedIconTab,
                                    onTabSelected = { viewModel.selectedIconTab = it },
                                    selectedImageUri = if (viewModel.selectedImageUri != null) viewModel.selectedImageUri else viewModel.selectedImageUriString.ifBlank { null },
                                    onImageSelected = { viewModel.updateImageUri(context, it) },
                                    selectedEmoji = viewModel.selectedEmoji,
                                    onEmojiChanged = { viewModel.selectedEmoji = it },
                                    selectedSvgCode = viewModel.selectedSvgCode,
                                    onSvgChanged = { viewModel.selectedSvgCode = it }
                                )

                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                                // Section 3: Members & Invitations
                                ProjectFormMembersSection(
                                    invites = viewModel.invites,
                                    onAddInvite = { viewModel.addInvite(it) },
                                    onUpdateRole = { index, role -> viewModel.updateRole(index, role) },
                                    onRemoveInvite = { index -> viewModel.removeInvite(index) }
                                )
                            }
                        }

                        // Section 4: Action buttons
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
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
                                text = if (viewModel.isEdit) "Mettre à jour" else "Créer le projet",
                                onClick = { viewModel.submit() },
                                modifier = Modifier
                                    .weight(1.5f)
                                    .height(50.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
