package fr.studio.voxel.organ.ui.organ.form.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import fr.studio.voxel.organ.R

@Composable
fun OrganFormDetailsSection(
    title: String,
    onTitleChange: (String) -> Unit,
    description: String,
    onDescriptionChange: (String) -> Unit,
    highlightColor: Color
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
            placeholder = { Text("Ex: Développement Frontend", color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)) },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = {
                Icon(
                    painter = painterResource(R.drawable.outline_info),
                    contentDescription = "Info obligatoire"
                )
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = highlightColor,
                unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color(0xFFF9F9F9)
            )
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
            placeholder = { Text("Décrivez le rôle de cet Organ dans le projet...", color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)) },
            minLines = 3,
            maxLines = 5,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = highlightColor,
                unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color(0xFFF9F9F9)
            )
        )
    }
}
