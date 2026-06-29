package fr.studio.voxel.organ.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.github.skydoves.colorpicker.compose.HsvColorPicker
import com.github.skydoves.colorpicker.compose.rememberColorPickerController
import fr.studio.voxel.organ.R

@Composable
fun ColorSelector(
    selectedColor : Color,
    onColorSelected: (Color) -> Unit = {},
) {
    val themePrimary = MaterialTheme.colorScheme.primary
    val defaultColors = remember {
        listOf(
            themePrimary,
            Color(0xFF4ADE80), // Vert
            Color(0xFF60A5FA), // Bleu
            Color(0xFFFBBF24), // Jaune
            Color(0xFFA78BFA) //Violet
        )
    }

    val isCustomColor = selectedColor !in defaultColors

    var showColorPicker by remember { mutableStateOf(false) }
    val controller = rememberColorPickerController()

    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        defaultColors.forEach { color ->
            ColorCircle(
                color = color,
                isSelected = selectedColor == color,
                onClick = { onColorSelected(color) }
            )
        }

        Box(
            modifier = Modifier
                .size(42.dp)
                .clickable { showColorPicker = true },
            contentAlignment = Alignment.Center
        ) {
            if (isCustomColor) {
                Box(modifier = Modifier
                    .size(32.dp)
                    .background(selectedColor, CircleShape)
                )
            }
            Icon(
                painter = painterResource(R.drawable.outline_add_circle_24),
                contentDescription = "Ajouter votre couleur",
                modifier = Modifier
                    .size(42.dp)
            )
        }

    }
    if (showColorPicker) {
        Dialog(onDismissRequest = { showColorPicker = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp)
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Aperçu de la couleur",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(controller.selectedColor.value) // Couleur en temps réel
                    )

                    HsvColorPicker(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        controller = controller,
                        onColorChanged = { colorEnvelope ->
                            val newColor = colorEnvelope.color
                            onColorSelected(newColor)
                        }
                    )
                    Button(
                        onClick = { showColorPicker = false },
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Text("Valider")
                    }
                }
            }
        }
    }
}
