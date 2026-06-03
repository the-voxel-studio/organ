package fr.studio.voxel.organ.ui.organ.form.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.studio.voxel.organ.network.services.ProjectMember
import fr.studio.voxel.organ.viewmodel.FormMember
import fr.studio.voxel.organ.viewmodel.FormRole

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OrganFormMembersSection(
    addedMembers: List<FormMember>,
    projectMembers: List<ProjectMember>,
    roles: List<FormRole>,
    highlightColor: Color,
    canManage: Boolean,
    onMembersChanged: (List<FormMember>) -> Unit
) {
    var expandedDropdown by remember { mutableStateOf(false) }
    var selectedMemberUuid by remember { mutableStateOf("") }

    // Filter project members that are not already added to the organ
    val availableMembers = remember(projectMembers, addedMembers) {
        projectMembers.filter { pm ->
            !addedMembers.any { am -> am.userUuid == pm.user.uuid }
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Membres de l'Organ",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Associez les collaborateurs du projet à cet Organ et assignez-leur des rôles spécifiques.",
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (canManage && availableMembers.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    val selectedMember = availableMembers.find { it.user.uuid == selectedMemberUuid }
                    OutlinedButton(
                        onClick = { expandedDropdown = true },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color(0xFFF9F9F9),
                            contentColor = Color.Black
                        )
                    ) {
                        Text(
                            text = selectedMember?.let { "${it.user.firstName} ${it.user.lastName}" } ?: "Sélectionner un collaborateur...",
                            color = if (selectedMember != null) Color.Black else Color.Gray,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    DropdownMenu(
                        expanded = expandedDropdown,
                        onDismissRequest = { expandedDropdown = false },
                        modifier = Modifier.fillMaxWidth(0.6f)
                    ) {
                        availableMembers.forEach { pm ->
                            DropdownMenuItem(
                                text = { Text("${pm.user.firstName} ${pm.user.lastName} (${pm.user.email})") },
                                onClick = {
                                    selectedMemberUuid = pm.user.uuid
                                    expandedDropdown = false
                                }
                            )
                        }
                    }
                }

                Button(
                    onClick = {
                        val member = availableMembers.find { it.user.uuid == selectedMemberUuid }
                        if (member != null) {
                            val newMember = FormMember(
                                userUuid = member.user.uuid,
                                name = "${member.user.firstName} ${member.user.lastName}",
                                email = member.user.email,
                                roles = emptyList() // User must assign at least one role!
                            )
                            onMembersChanged(addedMembers + newMember)
                            selectedMemberUuid = ""
                        }
                    },
                    enabled = selectedMemberUuid.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = highlightColor),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.height(50.dp)
                ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Ajouter")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // List of added members
        if (addedMembers.isEmpty()) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
                color = Color.White,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(36.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Aucun membre n'a encore été assigné à cet Organ.", style = MaterialTheme.typography.bodyMedium, color = Color.Gray, textAlign = TextAlign.Center)
                }
            }
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                addedMembers.forEach { member ->
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
                        color = Color.White,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(member.name, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                    Text(member.email, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                }

                                if (canManage) {
                                    IconButton(onClick = {
                                        onMembersChanged(addedMembers.filter { it.userUuid != member.userUuid })
                                    }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Retirer du membre", tint = Color.Red.copy(alpha = 0.7f))
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text("Rôles attribués (minimum 1) :", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                            Spacer(modifier = Modifier.height(8.dp))

                            // List of checkable roles
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                roles.forEach { role ->
                                    val isActive = member.roles.contains(role.id)
                                    val border = if (isActive) {
                                        BorderStroke(1.5.dp, highlightColor)
                                    } else {
                                        BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                                    }
                                    val bg = if (isActive) highlightColor.copy(alpha = 0.1f) else Color.White
                                    val textColor = if (isActive) highlightColor else Color.Gray

                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        border = border,
                                        color = bg,
                                        modifier = Modifier
                                            .clickable(enabled = canManage) {
                                                val currentRoles = member.roles.toMutableList()
                                                if (currentRoles.contains(role.id)) {
                                                    // Allow unassigning only if at least one role remains
                                                    if (currentRoles.size > 1) {
                                                        currentRoles.remove(role.id)
                                                    }
                                                } else {
                                                    currentRoles.add(role.id)
                                                }
                                                onMembersChanged(addedMembers.map {
                                                    if (it.userUuid == member.userUuid) it.copy(roles = currentRoles) else it
                                                })
                                            }
                                    ) {
                                        Text(
                                            text = "${role.iconData} ${role.name.ifBlank { "(Rôle sans nom)" }}",
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                            color = textColor,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                        )
                                    }
                                }
                            }

                            if (member.roles.isEmpty()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Attention : Veuillez attribuer au moins un rôle à ce collaborateur.", color = Color.Red, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
