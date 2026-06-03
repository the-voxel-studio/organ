package fr.studio.voxel.organ.ui.organ.form.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import fr.studio.voxel.organ.ui.components.getFirstEmojiGrapheme
import fr.studio.voxel.organ.viewmodel.FormRole
import fr.studio.voxel.organ.viewmodel.RolePreset

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OrganFormRolesSection(
    roles: List<FormRole>,
    presets: Map<String, RolePreset>,
    organPermissions: List<String>,
    taskPermissions: List<String>,
    interactionPermissions: List<String>,
    highlightColor: Color,
    canManage: Boolean,
    onRolesChanged: (List<FormRole>) -> Unit
) {
    var showModal by remember { mutableStateOf(false) }
    var editingRoleId by remember { mutableStateOf<String?>(null) }

    val openRoleDialog = { roleId: String ->
        editingRoleId = roleId
        showModal = true
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Rôles de l'Organ",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Créez des rôles pour structurer les permissions de vos membres dans cet Organ.",
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Modèles de rôles presets
        Text(
            text = "Modèles de rôles",
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            color = Color.Gray
        )
        Spacer(modifier = Modifier.height(8.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            presets.forEach { (key, preset) ->
                val alreadyAdded = roles.any { it.name.equals(preset.name, ignoreCase = true) }
                SuggestionChip(
                    onClick = {
                        if (canManage && !alreadyAdded) {
                            val newRole = FormRole(
                                id = "role-${System.currentTimeMillis()}-${Math.random()}",
                                name = preset.name,
                                iconData = preset.iconData,
                                permissions = preset.permissions
                            )
                            onRolesChanged(roles + newRole)
                        }
                    },
                    label = { Text("${preset.iconData} ${preset.name}") },
                    enabled = canManage && !alreadyAdded,
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = if (alreadyAdded) Color.LightGray.copy(alpha = 0.1f) else Color.White
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Roles List
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            roles.forEach { role ->
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
                    color = Color(0xFFFAFAFA),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = highlightColor.copy(alpha = 0.1f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(role.iconData, fontSize = 16.sp)
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = role.name.ifBlank { "(Rôle sans nom)" },
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (role.name.isBlank()) Color.Gray else Color.Black
                            )
                            Text(
                                text = "${role.permissions.size} permission(s)",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }

                        if (canManage) {
                            IconButton(onClick = { openRoleDialog(role.id) }) {
                                Icon(Icons.Default.Edit, contentDescription = "Modifier", tint = Color.Gray)
                            }
                            IconButton(
                                onClick = {
                                    if (roles.size > 1) {
                                        onRolesChanged(roles.filter { it.id != role.id })
                                    }
                                },
                                enabled = roles.size > 1
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Supprimer",
                                    tint = if (roles.size > 1) Color.Red.copy(alpha = 0.7f) else Color.LightGray
                                )
                            }
                        }
                    }
                }
            }
        }

        if (canManage) {
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = {
                    val newRole = FormRole(
                        id = "role-${System.currentTimeMillis()}-${Math.random()}",
                        name = "",
                        iconData = "👤",
                        permissions = listOf("ORGAN_VIEW"),
                        isNew = true
                    )
                    val updated = roles + newRole
                    onRolesChanged(updated)
                    openRoleDialog(newRole.id)
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Ajouter un rôle", fontWeight = FontWeight.Bold)
            }
        }
    }

    // Role editing Dialog
    if (showModal && editingRoleId != null) {
        val editingRole = roles.find { it.id == editingRoleId }
        if (editingRole != null) {
            RoleEditDialog(
                role = editingRole,
                organPermissions = organPermissions,
                taskPermissions = taskPermissions,
                interactionPermissions = interactionPermissions,
                highlightColor = highlightColor,
                onSave = { name, emoji, permissions ->
                    val updated = roles.map { role ->
                        if (role.id == editingRoleId) {
                            role.copy(
                                name = name,
                                iconData = emoji,
                                permissions = permissions,
                                isNew = false
                            )
                        } else {
                            role
                        }
                    }
                    onRolesChanged(updated)
                    showModal = false
                },
                onDismiss = {
                    if (editingRole.isNew) {
                        onRolesChanged(roles.filter { it.id != editingRoleId })
                    }
                    showModal = false
                }
            )
        }
    }
}
