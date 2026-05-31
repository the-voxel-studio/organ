package fr.studio.voxel.organ.ui.organ

import android.net.Uri
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import fr.studio.voxel.organ.R
import fr.studio.voxel.organ.ui.components.LoadingOverlay
import fr.studio.voxel.organ.ui.components.PrimaryButton
import fr.studio.voxel.organ.ui.components.ColorSelector
import fr.studio.voxel.organ.ui.components.IconSelector
import fr.studio.voxel.organ.ui.components.IconType
import fr.studio.voxel.organ.ui.header.Header
import fr.studio.voxel.organ.viewmodel.OrganFormViewModel

@Composable
fun OrganFormScreen(
    projectUuid: String,
    organUuid: String?,
    onBack: () -> Unit,
    onSuccess: () -> Unit,
    viewModel: OrganFormViewModel = viewModel()
) {
    LaunchedEffect(projectUuid, organUuid) {
        viewModel.initOrgan(projectUuid, organUuid)
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
            text = if (viewModel.isEdit) "Mise à jour de l'organ..." else "Création de l'organ..."
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
                            text = if (viewModel.isEdit) "Retour à l'Organ" else "Retour au Projet",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = if (viewModel.isEdit) "Paramètres de l'Organ" else "Créer un nouvel organ",
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
                            // Section 1: Details
                            OrganDetailsSection(
                                title = viewModel.title,
                                onTitleChange = { viewModel.title = it },
                                description = viewModel.description,
                                onDescriptionChange = { viewModel.description = it }
                            )

                            // Section 2: Visual identity
                            OrganVisualIdentitySection(
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
                        }
                    }

                    // Section 3: Action buttons
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
                            text = if (viewModel.isEdit) "Mettre à jour" else "Créer l'organ",
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
fun OrganDetailsSection(
    title: String,
    onTitleChange: (String) -> Unit,
    description: String,
    onDescriptionChange: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Nom de l'Organ",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = title,
            onValueChange = onTitleChange,
            placeholder = { Text("Ex: Développement Frontend") },
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
            placeholder = { Text("Décrivez le rôle de cet Organ dans le projet...") },
            minLines = 3,
            maxLines = 5,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun OrganVisualIdentitySection(
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
            text = "Vous n'avez pas l'autorisation requise pour modifier cet organ. Seuls les administrateurs et managers du projet y ont accès.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        PrimaryButton(
            text = "Retour",
            onClick = onBack,
            modifier = Modifier.fillMaxWidth(0.6f)
        )
    }
}
