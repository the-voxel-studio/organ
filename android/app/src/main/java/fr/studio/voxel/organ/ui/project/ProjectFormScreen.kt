package fr.studio.voxel.organ.ui.project

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import fr.studio.voxel.organ.R
import fr.studio.voxel.organ.ui.components.PrimaryButton
import fr.studio.voxel.organ.ui.components.ColorSelector
import fr.studio.voxel.organ.ui.components.IconSelector
import fr.studio.voxel.organ.ui.components.LoadingOverlay
import fr.studio.voxel.organ.ui.components.IconType
import fr.studio.voxel.organ.ui.header.Header
import fr.studio.voxel.organ.viewmodel.ProjectFormInvite
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
                            ProjectDetailsSection(
                                title = viewModel.title,
                                onTitleChange = { viewModel.title = it },
                                description = viewModel.description,
                                onDescriptionChange = { viewModel.description = it },
                                status = viewModel.status,
                                onStatusChange = { viewModel.status = it },
                                isEdit = viewModel.isEdit
                            )

                            // Section 2: Visual identity (colors & icons)
                            ProjectVisualIdentitySection(
                                selectedColor = viewModel.selectedColor,
                                onColorSelected = { viewModel.selectedColor = it },
                                selectedIconTab = viewModel.selectedIconTab,
                                onTabSelected = { viewModel.selectedIconTab = it },
                                selectedImageUri = viewModel.selectedImageUri,
                                onImageSelected = { viewModel.updateImageUri(it) },
                                selectedEmoji = viewModel.selectedEmoji,
                                onEmojiChanged = { viewModel.selectedEmoji = it },
                                selectedSvgCode = viewModel.selectedSvgCode,
                                onSvgChanged = { viewModel.selectedSvgCode = it }
                            )

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                            // Section 3: Members & Invitations
                            ProjectMembersSection(
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

@Composable
fun ProjectDetailsSection(
    title: String,
    onTitleChange: (String) -> Unit,
    description: String,
    onDescriptionChange: (String) -> Unit,
    status: String,
    onStatusChange: (String) -> Unit,
    isEdit: Boolean
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Nom du projet",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = title,
            onValueChange = onTitleChange,
            placeholder = { Text("Ex: Refonte du site web") },
            singleLine = true,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = {
                Icon(
                    painter = painterResource(R.drawable.outline_info),
                    contentDescription = "Info obligatoire"
                )
            }
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Description",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = description,
            onValueChange = onDescriptionChange,
            placeholder = { Text("Décrivez brièvement les objectifs du projet...") },
            minLines = 3,
            maxLines = 5,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        )

        if (isEdit) {
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "Statut du projet",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            var expanded by remember { mutableStateOf(false) }
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = { expanded = true },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = when (status) {
                                "ACTIVE" -> "Actif"
                                "ARCHIVED" -> "Archivé"
                                "INACTIVE" -> "Inactif"
                                else -> status
                            },
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Dropdown"
                        )
                    }
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.fillMaxWidth(0.9f)
                ) {
                    DropdownMenuItem(
                        text = { Text("Actif") },
                        onClick = { onStatusChange("ACTIVE"); expanded = false }
                    )
                    DropdownMenuItem(
                        text = { Text("Archivé") },
                        onClick = { onStatusChange("ARCHIVED"); expanded = false }
                    )
                    DropdownMenuItem(
                        text = { Text("Inactif") },
                        onClick = { onStatusChange("INACTIVE"); expanded = false }
                    )
                }
            }
        }
    }
}

@Composable
fun ProjectVisualIdentitySection(
    selectedColor: Color,
    onColorSelected: (Color) -> Unit,
    selectedIconTab: IconType,
    onTabSelected: (IconType) -> Unit,
    selectedImageUri: Uri?,
    onImageSelected: (Uri?) -> Unit,
    selectedEmoji: String,
    onEmojiChanged: (String) -> Unit,
    selectedSvgCode: String,
    onSvgChanged: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Couleur",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
        )
        Spacer(modifier = Modifier.height(8.dp))
        ColorSelector(
            selectedColor = selectedColor,
            onColorSelected = onColorSelected
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Icône",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
        )
        Spacer(modifier = Modifier.height(8.dp))
        IconSelector(
            selectedTab = selectedIconTab,
            onTabSelected = onTabSelected,
            imageUri = selectedImageUri,
            onImageSelected = onImageSelected,
            emojiText = selectedEmoji,
            onEmojiChanged = onEmojiChanged,
            svgCode = selectedSvgCode,
            onSvgChanged = onSvgChanged
        )
    }
}

@Composable
fun ProjectMembersSection(
    invites: List<ProjectFormInvite>,
    onAddInvite: (String) -> Unit,
    onUpdateRole: (Int, String) -> Unit,
    onRemoveInvite: (Int) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Inviter des membres",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))

        var newEmail by remember { mutableStateOf("") }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = newEmail,
                onValueChange = { newEmail = it },
                placeholder = { Text("email@exemple.com") },
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Button(
                onClick = {
                    if (newEmail.isNotBlank()) {
                        onAddInvite(newEmail)
                        newEmail = ""
                    }
                },
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.height(56.dp)
            ) {
                Text("Ajouter")
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Membres du projet",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
        )
        Spacer(modifier = Modifier.height(12.dp))

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            invites.forEachIndexed { index, invite ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = if (invite.isCreator) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .border(
                            width = 1.dp,
                            color = if (invite.isCreator) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f) else MaterialTheme.colorScheme.outlineVariant,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = invite.email,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        var expanded by remember { mutableStateOf(false) }
                        Box {
                            Row(
                                modifier = Modifier
                                    .clickable(enabled = !invite.isCreator) { expanded = true }
                                    .padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val roleLabel = when (invite.role) {
                                    "ADMIN" -> "Administrateur"
                                    "MANAGER" -> "Manager"
                                    "MEMBER" -> "Membre"
                                    else -> invite.role
                                }
                                Text(
                                    text = if (invite.isPending) "$roleLabel (en attente)" else roleLabel,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (invite.isCreator) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                                )
                                if (invite.roleChanged) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "(modifié)",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                if (!invite.isCreator) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = "Rôles",
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Administrateur") },
                                    onClick = {
                                        onUpdateRole(index, "ADMIN")
                                        expanded = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Manager") },
                                    onClick = {
                                        onUpdateRole(index, "MANAGER")
                                        expanded = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Membre") },
                                    onClick = {
                                        onUpdateRole(index, "MEMBER")
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }

                    if (!invite.isCreator) {
                        IconButton(
                            onClick = { onRemoveInvite(index) }
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.poubelle_logo),
                                contentDescription = "Supprimer",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AccessDeniedScreen(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = painterResource(id = R.drawable.outline_info),
            contentDescription = "Accès refusé",
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Accès refusé",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onErrorContainer
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Vous n'avez pas l'autorisation requise pour modifier ce projet. Seuls les administrateurs du projet y ont accès.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        PrimaryButton(
            text = "Retour",
            onClick = onBack,
            modifier = Modifier.fillMaxWidth(0.6f)
        )
    }
}
