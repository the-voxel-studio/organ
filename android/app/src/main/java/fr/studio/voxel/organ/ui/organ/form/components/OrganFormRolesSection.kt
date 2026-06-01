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
    var modalRoleName by remember { mutableStateOf("") }
    var modalRoleEmoji by remember { mutableStateOf("👤") }
    var modalRolePermissions by remember { mutableStateOf<List<String>>(emptyList()) }
    var modalRoleError by remember { mutableStateOf("") }

    val openRoleDialog = { roleId: String ->
        val role = roles.find { it.id == roleId }
        if (role != null) {
            editingRoleId = roleId
            modalRoleName = role.name
            modalRoleEmoji = role.iconData
            modalRolePermissions = role.permissions
            modalRoleError = ""
            showModal = true
        }
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
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
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
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Ajouter un rôle personnalisé", fontWeight = FontWeight.Bold)
            }
        }
    }

    // Role editing Dialog
    if (showModal && editingRoleId != null) {
        val permissionLabels = mapOf(
            "ORGAN_VIEW" to Pair("Voir l'Organ", "Accéder à l'Organ, voir les tâches et les membres associés."),
            "ORGAN_EDIT" to Pair("Modifier l'Organ", "Changer le nom, la description et l'identité visuelle de l'Organ."),
            "ORGAN_MANAGE_MEMBERS" to Pair("Gérer les membres", "Inviter ou retirer des membres de l'Organ."),
            "ORGAN_MANAGE_ROLES" to Pair("Gérer les rôles", "Créer, modifier les permissions et supprimer des rôles locaux."),
            "ORGAN_LINK_MANAGE" to Pair("Gérer les liens utiles", "Ajouter, modifier ou supprimer des liens partagés."),
            "ORGAN_HARD_DELETE" to Pair("Supprimer définitivement l'Organ", "Effacer irréversiblement l'Organ."),
            "TASK_VIEW_ALL" to Pair("Voir toutes les tâches", "Visualiser les tickets de tous les membres."),
            "TASK_CREATE" to Pair("Créer des tâches", "Ajouter de nouveaux tickets dans l'Organ."),
            "TASK_EDIT_OWN" to Pair("Modifier ses propres tâches", "Mettre à jour ses propres tickets."),
            "TASK_EDIT_ALL" to Pair("Modifier toutes les tâches", "Editer l'ensemble des tickets de l'Organ."),
            "TASK_DELETE_OWN" to Pair("Supprimer ses tâches", "Mettre ses propres tâches à la corbeille."),
            "TASK_DELETE_ALL" to Pair("Supprimer toutes les tâches", "Mettre n'importe quel ticket à la corbeille."),
            "TASK_HARD_DELETE_OWN" to Pair("Supprimer définitivement ses tâches", "Effacer irréversiblement ses tâches."),
            "TASK_HARD_DELETE_ALL" to Pair("Supprimer définitivement toutes les tâches", "Effacer irréversiblement n'importe quel ticket."),
            "TASK_STATUS_CHANGE_OWN" to Pair("Changer le statut de ses tâches", "Faire progresser l'état de ses propres tickets."),
            "TASK_STATUS_CHANGE_ALL" to Pair("Changer le statut de toutes les tâches", "Modifier le statut de tous les tickets."),
            "TASK_PRIORITY_CHANGE_OWN" to Pair("Gérer la priorité de ses tâches", "Changer l'urgence de ses propres tickets."),
            "TASK_PRIORITY_CHANGE_ALL" to Pair("Gérer la priorité de toutes les tâches", "Changer l'urgence de tous les tickets."),
            "TASK_DATES_MANAGE_OWN" to Pair("Gérer les dates de ses tâches", "Modifier les échéances de ses propres tickets."),
            "TASK_DATES_MANAGE_ALL" to Pair("Gérer les dates de toutes les tâches", "Modifier les échéances de tous les tickets."),
            "TASK_ESTIMATE_MANAGE_OWN" to Pair("Gérer les estimations de ses tâches", "Estimer le temps de ses propres tickets."),
            "TASK_ESTIMATE_MANAGE_ALL" to Pair("Gérer les estimations de toutes les tâches", "Modifier les estimations de tous les tickets."),
            "TASK_ASSIGN_SELF" to Pair("S'assigner à des tâches", "Se positionner comme intervenant sur un ticket."),
            "TASK_ASSIGN_OTHERS" to Pair("Assigner des collaborateurs", "Affecter des tâches à d'autres membres."),
            "TASK_TAG_MANAGE_OWN" to Pair("Gérer les tags de ses tâches", "Ajouter/retirer des étiquettes sur ses tickets."),
            "TASK_TAG_MANAGE_ALL" to Pair("Gérer les tags de toutes les tâches", "Ajouter/retirer des étiquettes sur tous les tickets."),
            "TASK_LINK_MANAGE_OWN" to Pair("Gérer les liens de ses tâches", "Associer des liens de documentation à ses tickets."),
            "TASK_LINK_MANAGE_ALL" to Pair("Gérer les liens de toutes les tâches", "Associer des liens de documentation à tous les tickets."),
            "TASK_DEPENDENCY_MANAGE_OWN" to Pair("Gérer les dépendances de ses tâches", "Lier ses tâches à des tâches parentes."),
            "TASK_DEPENDENCY_MANAGE_ALL" to Pair("Gérer les dépendances de toutes les tâches", "Lier tous les tickets."),
            "TASK_VALIDATE" to Pair("Valider les tâches", "Valider ou vérifier l'avancement des tâches."),
            "COMMENT_CREATE" to Pair("Ajouter des commentaires", "Participer aux discussions sous les tickets."),
            "COMMENT_EDIT_OWN" to Pair("Modifier ses commentaires", "Editer ses propres messages de discussion."),
            "COMMENT_DELETE_OWN" to Pair("Supprimer ses commentaires", "Envoyer ses propres messages à la corbeille."),
            "COMMENT_DELETE_ALL" to Pair("Supprimer tous les commentaires", "Masquer les messages inappropriés."),
            "COMMENT_HARD_DELETE_OWN" to Pair("Effacer définitivement ses commentaires", "Supprimer irréversiblement ses messages."),
            "COMMENT_HARD_DELETE_ALL" to Pair("Effacer définitivement tous les commentaires", "Supprimer définitivement les messages masqués."),
            "ATTACHMENT_ADD" to Pair("Ajouter des fichiers joints", "Téléverser des fichiers sous les tâches."),
            "ATTACHMENT_DELETE_OWN" to Pair("Supprimer ses fichiers", "Retirer vos propres documents (corbeille)."),
            "ATTACHMENT_DELETE_ALL" to Pair("Supprimer tous les fichiers", "Modérer tous les documents joints."),
            "ATTACHMENT_HARD_DELETE_OWN" to Pair("Supprimer définitivement vos fichiers", "Effacer irréversiblement vos documents."),
            "ATTACHMENT_HARD_DELETE_ALL" to Pair("Supprimer définitivement tous les fichiers", "Nettoyage définitif du stockage.")
        )

        Dialog(onDismissRequest = {
            val role = roles.find { it.id == editingRoleId }
            if (role != null && role.isNew) {
                onRolesChanged(roles.filter { it.id != editingRoleId })
            }
            showModal = false
        }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color.White,
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.9f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp)
                ) {
                    Text(
                        text = "Configurer le rôle",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )

                    if (modalRoleError.isNotBlank()) {
                        Text(
                            text = modalRoleError,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Fields Name & Emoji
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = modalRoleEmoji,
                            onValueChange = {
                                modalRoleEmoji = getFirstEmojiGrapheme(it)
                            },
                            label = { Text("Emoji") },
                            modifier = Modifier.width(72.dp),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = modalRoleName,
                            onValueChange = {
                                modalRoleName = it
                                modalRoleError = ""
                            },
                            label = { Text("Nom du rôle") },
                            placeholder = { Text("Ex: Développeur") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Permissions associées",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color.Gray
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Grouped permissions selection list
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                    ) {
                        // Group 1: Organ
                        Text("Gestion de l'Organ", fontWeight = FontWeight.Bold, color = highlightColor, fontSize = 12.sp, modifier = Modifier.padding(vertical = 8.dp))
                        organPermissions.forEach { name ->
                            PermissionItemRow(
                                name = name,
                                label = permissionLabels[name]?.first ?: name,
                                desc = permissionLabels[name]?.second ?: "",
                                checked = name == "ORGAN_VIEW" || modalRolePermissions.contains(name) || (name == "ORGAN_MANAGE_ROLES" && modalRolePermissions.contains("ORGAN_MANAGE_MEMBERS")),
                                enabled = name != "ORGAN_VIEW",
                                onCheckedChange = { isChecked ->
                                    val current = modalRolePermissions.toMutableList()
                                    if (isChecked) {
                                        current.add(name)
                                        if (name == "ORGAN_MANAGE_ROLES" && !current.contains("ORGAN_MANAGE_MEMBERS")) {
                                            current.add("ORGAN_MANAGE_MEMBERS")
                                        }
                                    } else {
                                        current.remove(name)
                                    }
                                    modalRolePermissions = current
                                }
                            )
                        }

                        // Group 2: Tasks
                        Text("Gestion des Tâches", fontWeight = FontWeight.Bold, color = highlightColor, fontSize = 12.sp, modifier = Modifier.padding(vertical = 8.dp))
                        taskPermissions.forEach { name ->
                            PermissionItemRow(
                                name = name,
                                label = permissionLabels[name]?.first ?: name,
                                desc = permissionLabels[name]?.second ?: "",
                                checked = modalRolePermissions.contains(name),
                                onCheckedChange = { isChecked ->
                                    val current = modalRolePermissions.toMutableList()
                                    if (isChecked) current.add(name) else current.remove(name)
                                    modalRolePermissions = current
                                }
                            )
                        }

                        // Group 3: Comments & attachments
                        Text("Commentaires & Fichiers", fontWeight = FontWeight.Bold, color = highlightColor, fontSize = 12.sp, modifier = Modifier.padding(vertical = 8.dp))
                        interactionPermissions.forEach { name ->
                            PermissionItemRow(
                                name = name,
                                label = permissionLabels[name]?.first ?: name,
                                desc = permissionLabels[name]?.second ?: "",
                                checked = modalRolePermissions.contains(name),
                                onCheckedChange = { isChecked ->
                                    val current = modalRolePermissions.toMutableList()
                                    if (isChecked) current.add(name) else current.remove(name)
                                    modalRolePermissions = current
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val role = roles.find { it.id == editingRoleId }
                                if (role != null && role.isNew) {
                                    onRolesChanged(roles.filter { it.id != editingRoleId })
                                }
                                showModal = false
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Annuler")
                        }

                        Button(
                            onClick = {
                                val name = modalRoleName.trim()
                                if (name.isBlank()) {
                                    modalRoleError = "Le nom du rôle est obligatoire."
                                    return@Button
                                }
                                val updated = roles.map { role ->
                                    if (role.id == editingRoleId) {
                                        val finalPerms = modalRolePermissions.toMutableList()
                                        if (!finalPerms.contains("ORGAN_VIEW")) {
                                            finalPerms.add("ORGAN_VIEW")
                                        }
                                        if (finalPerms.contains("ORGAN_MANAGE_ROLES")) {
                                            if (!finalPerms.contains("ORGAN_MANAGE_MEMBERS")) {
                                                finalPerms.add("ORGAN_MANAGE_MEMBERS")
                                            }
                                        } else {
                                            finalPerms.remove("ORGAN_MANAGE_MEMBERS")
                                        }
                                        role.copy(
                                            name = name,
                                            iconData = modalRoleEmoji.ifBlank { "👤" },
                                            permissions = finalPerms,
                                            isNew = false
                                        )
                                    } else {
                                        role
                                    }
                                }
                                onRolesChanged(updated)
                                showModal = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = highlightColor),
                            modifier = Modifier.weight(1.5f)
                        ) {
                            Text("Enregistrer")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PermissionItemRow(
    name: String,
    label: String,
    desc: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onCheckedChange(!checked) }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = if (enabled) onCheckedChange else null,
            enabled = enabled
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(label, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
            if (desc.isNotBlank()) {
                Text(desc, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
        }
    }
}
