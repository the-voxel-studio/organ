package fr.studio.voxel.organ.ui.create

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import fr.studio.voxel.organ.R
import fr.studio.voxel.organ.ui.header.Header
import fr.studio.voxel.organ.viewmodel.CreateViewModel

@Composable
fun StepIdentity(
    mode : CreateMode,
    name : String,
    description : String,
    onNameChanged : (String) -> Unit = {},
    onDescrChanged : (String) -> Unit = {},
    createMV : CreateViewModel = viewModel()

){
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Header(navigateUp = {})

            Column(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.Start
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.flechedroite_logo),
                        contentDescription = "Flèche Retour au Dashboard",
                        tint = MaterialTheme.colorScheme.onSurface,

                        modifier = Modifier
                            .size(24.dp)
                            .rotate(180f)
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Text(
                        text = if (mode == CreateMode.PROJECT) "Retour au Dashboard" else "Retour au Projet",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Normal
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = if (mode == CreateMode.PROJECT) "Créer un Nouveau Projet" else "Créer un Nouvel Organ",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = if (mode == CreateMode.PROJECT) "Nom du projet" else "Nom de l'Organ",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface.copy(0.8f)
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = onNameChanged,
                    placeholder = { Text(text = if (mode == CreateMode.PROJECT) "Ex: Refonte du site web" else "Ex : Développement Frontend") },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 64.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    ),
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
                    color = MaterialTheme.colorScheme.onSurface.copy(0.8f)
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = onDescrChanged,
                    placeholder = { Text(text = if (mode == CreateMode.PROJECT) "Décrivez briévement les objectifs du projet..." else "Décrivez le rôle de cet Organ dans le projet") },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 64.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Couleur",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface.copy(0.8f)
                )

                Spacer(modifier = Modifier.height(12.dp))

                ColorSelector(
                    selectedColor = createMV.selectedColorInput,
                    onColorSelected = { color -> createMV.updateSelectedColor(color) }
                )

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Icone",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface.copy(0.8f)
                )

                Spacer(modifier = Modifier.height(12.dp))

                IconSelector(
                    selectedTab = createMV.currentIconTab,
                    onTabSelected = { tab -> createMV.updateIconTab(tab) },

                    imageUri = createMV.selectedImageUri,
                    onImageSelected = { uri -> createMV.updateImageUri(uri) },

                    emojiText = createMV.selectedEmoji,
                    onEmojiChanged = { emoji -> createMV.updateEmoji(emoji) },

                    svgCode = createMV.selectedSvgCode,
                    onSvgChanged = { code -> createMV.updateSvgCode(code) },
                )

            }
        }
    }
}