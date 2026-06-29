package fr.studio.voxel.organ.ui.organ.form.components

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import fr.studio.voxel.organ.ui.components.ColorSelector
import fr.studio.voxel.organ.ui.components.IconSelector
import fr.studio.voxel.organ.ui.components.IconType

@Composable
fun OrganFormVisualIdentitySection(
    selectedColor: Color,
    onColorSelected: (Color) -> Unit,
    selectedIconTab: IconType,
    onTabSelected: (IconType) -> Unit,
    selectedImageUri: Any?,
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
