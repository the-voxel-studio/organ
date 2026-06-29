package fr.studio.voxel.organ.ui.organ.form.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import fr.studio.voxel.organ.ui.components.getFirstEmojiGrapheme
import fr.studio.voxel.organ.viewmodel.FormRole

@Composable
fun RoleEditDialog(
    role: FormRole,
    organPermissions: List<String>,
    taskPermissions: List<String>,
    interactionPermissions: List<String>,
    highlightColor: Color,
    onSave: (name: String, emoji: String, permissions: List<String>) -> Unit,
    onDismiss: () -> Unit
) {
    var modalRoleName by remember { mutableStateOf(role.name) }
    var modalRoleEmoji by remember { mutableStateOf(role.iconData) }
    var modalRolePermissions by remember { mutableStateOf(role.permissions) }
    var modalRoleError by remember { mutableStateOf("") }

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
        "TASK_LINK_HARD_DELETE_OWN" to Pair("Supprimer définitivement les liens de ses tâches", "Effacer de manière irréversible les liens de ses tickets."),
        "TASK_LINK_HARD_DELETE_ALL" to Pair("Supprimer définitivement les liens de toutes les tâches", "Effacer de manière irréversible les liens de tous les tickets."),
        "TASK_TAG_HARD_DELETE" to Pair("Supprimer définitivement les tags", "Effacer de manière irréversible les étiquettes de l'Organ."),
        "TASK_DEPENDENCY_MANAGE_OWN" to Pair("Gérer les dépendances de ses tâches", "Lier ses tâches à des tâches parentes."),
        "TASK_DEPENDENCY_MANAGE_ALL" to Pair("Gérer les dépendances de toutes les tâches", "Lier tous les tickets."),
        "TASK_VALIDATE" to Pair("Valider les tâches", "Valider ou vérifier l'avancement des tâches."),
        "COMMENT_CREATE" to Pair("Ajouter des commentaires", "Participer aux discussions sous les tickets."),
        "COMMENT_EDIT_OWN" to Pair("Modifier ses commentaires", "Editer ses propres messages de discussion."),
        "COMMENT_EDIT_ALL" to Pair("Modifier tous les commentaires", "Editer les messages de n'importe quel membre."),
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

    Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            color = Color.White,
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Configurer le rôle",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Fermer"
                        )
                    }
                }

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
                        modifier = Modifier.width(96.dp),
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = highlightColor,
                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color(0xFFF9F9F9)
                        )
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
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = highlightColor,
                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color(0xFFF9F9F9)
                        )
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
                    Text("Gestion de l'Organ", style = MaterialTheme.typography.bodyLarge , color = highlightColor, modifier = Modifier.padding(vertical = 8.dp))
                    organPermissions.forEach { name ->
                        PermissionItemRow(
                            name = name,
                            label = permissionLabels[name]?.first ?: name,
                            desc = permissionLabels[name]?.second ?: "",
                            checked = name == "ORGAN_VIEW" || modalRolePermissions.contains(name),
                            enabled = name != "ORGAN_VIEW",
                            onCheckedChange = { isChecked ->
                                val current = modalRolePermissions.toMutableList()
                                if (isChecked) {
                                    current.add(name)
                                    if (name == "ORGAN_MANAGE_ROLES") {
                                        current.add("ORGAN_MANAGE_MEMBERS")
                                    }
                                } else {
                                    current.remove(name)
                                    if (name == "ORGAN_MANAGE_MEMBERS") current.remove("ORGAN_MANAGE_ROLES")
                                }
                                modalRolePermissions = current.distinct()
                            }
                        )
                    }

                    // Group 2: Tasks
                    Text("Gestion des Tâches", style = MaterialTheme.typography.bodyLarge , color = highlightColor, modifier = Modifier.padding(vertical = 8.dp))
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
                    Text("Commentaires & Fichiers", style = MaterialTheme.typography.bodyLarge , color = highlightColor, modifier = Modifier.padding(vertical = 8.dp))
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
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).height(48.dp)
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
                            val finalPerms = modalRolePermissions.toMutableList()
                            if (!finalPerms.contains("ORGAN_VIEW")) {
                                finalPerms.add("ORGAN_VIEW")
                            }
                            if (finalPerms.contains("ORGAN_MANAGE_ROLES")) {
                                if (!finalPerms.contains("ORGAN_MANAGE_MEMBERS")) {
                                    finalPerms.add("ORGAN_MANAGE_MEMBERS")
                                }
                            }
                            onSave(name, modalRoleEmoji.ifBlank { "👤" }, finalPerms)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = highlightColor,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1.5f).height(48.dp)
                    ) {
                        Text("Enregistrer", fontWeight = FontWeight.Bold)
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
        Box(
            modifier = Modifier.size(48.dp),
            contentAlignment = Alignment.Center
        ){
            Checkbox(
                checked = checked,
                onCheckedChange = if (enabled) onCheckedChange else null,
                enabled = enabled
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(label, style = MaterialTheme.typography.bodySmall)
            if (desc.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(desc, style = MaterialTheme.typography.bodySmall.copy(fontSize = 14.sp, fontWeight = FontWeight.Normal), color = Color.Gray)
            }
        }
    }
}
