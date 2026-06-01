package fr.studio.voxel.organ.ui.project.form.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import fr.studio.voxel.organ.R

@Composable
fun ProjectFormDetailsSection(
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
