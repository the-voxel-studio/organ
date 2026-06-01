package fr.studio.voxel.organ.ui.project.form.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import fr.studio.voxel.organ.viewmodel.ProjectFormInvite

@Composable
fun ProjectFormMembersSection(
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
