package fr.studio.voxel.organ.ui.project.trash

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import fr.studio.voxel.organ.R
import fr.studio.voxel.organ.network.services.*
import fr.studio.voxel.organ.ui.components.*
import fr.studio.voxel.organ.ui.dashboard.ProjectIconBadge
import fr.studio.voxel.organ.ui.dashboard.ProjectVisual
import fr.studio.voxel.organ.ui.header.Header
import fr.studio.voxel.organ.ui.project.AccessDeniedScreen
import fr.studio.voxel.organ.viewmodel.ProjectTrashViewModel
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

@Composable
fun ProjectTrashScreen(
    projectUuid: String,
    onBack: () -> Unit,
    viewModel: ProjectTrashViewModel = viewModel()
) {
    LaunchedEffect(projectUuid) {
        viewModel.loadTrash(projectUuid)
    }

    var searchQuery by remember { mutableStateOf("") }

    // Collapsing states
    var organsCollapsed by remember { mutableStateOf(false) }
    var membersCollapsed by remember { mutableStateOf(false) }
    var tagsCollapsed by remember { mutableStateOf(false) }

    // Action dialog states
    var showConfirmDialog by remember { mutableStateOf(false) }
    var actionType by remember { mutableStateOf<String?>(null) } // "organ", "member", "tag", "bulk_members"
    var actionUuid by remember { mutableStateOf("") }
    var actionTitle by remember { mutableStateOf("") }
    var actionOp by remember { mutableStateOf("") } // "restore", "delete"

    val highlightColor = remember(viewModel.highlightColor) {
        try {
            Color(android.graphics.Color.parseColor(viewModel.highlightColor))
        } catch (e: Exception) {
            Color(0xFFFF7EB6)
        }
    }

    // Filter calculations
    val filteredOrgans = remember(viewModel.trashedOrgans, searchQuery) {
        val query = searchQuery.lowercase().trim()
        if (query.isBlank()) viewModel.trashedOrgans
        else viewModel.trashedOrgans.filter { it.title.lowercase().contains(query) }
    }

    val filteredMembers = remember(viewModel.trashedMembers, searchQuery) {
        val query = searchQuery.lowercase().trim()
        if (query.isBlank()) viewModel.trashedMembers
        else viewModel.trashedMembers.filter {
            val fullName = "${it.user.firstName} ${it.user.lastName}".lowercase()
            fullName.contains(query) || it.user.email.lowercase().contains(query) || it.role.lowercase().contains(query)
        }
    }

    val filteredTags = remember(viewModel.trashedTags, searchQuery) {
        val query = searchQuery.lowercase().trim()
        if (query.isBlank()) viewModel.trashedTags
        else viewModel.trashedTags.filter { it.name.lowercase().contains(query) }
    }

    val hasTrashedItems = viewModel.trashedOrgans.isNotEmpty() ||
            viewModel.trashedMembers.isNotEmpty() ||
            viewModel.trashedTags.isNotEmpty()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        if (viewModel.isLoading && viewModel.trashedOrgans.isEmpty() && viewModel.trashedMembers.isEmpty() && viewModel.trashedTags.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = highlightColor)
            }
        } else if (viewModel.accessDenied) {
            AccessDeniedScreen(onBack = onBack)
        } else {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Header(navigateUp = onBack, canOpenSidebar = false)

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Back link & Title
                        item {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                BackToLink(
                                    label = "Retour au Projet",
                                    onClick = onBack,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Start,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.poubelle_logo),
                                        contentDescription = null,
                                        tint = Color.Red.copy(alpha = 0.8f),
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = "Corbeille - ${viewModel.projectTitle}",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                                        color = Color.Black
                                    )
                                }

                                Text(
                                    text = "Retrouvez ici les éléments supprimés de ce projet.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                                )
                            }
                        }

                        // Search field
                        item {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text("Rechercher dans la corbeille...", color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)) },
                                singleLine = true,
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = highlightColor,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color(0xFFF9F9F9)
                                )
                            )
                        }

                        if (!hasTrashedItems) {
                            item {
                                EmptyTrashState(
                                    title = "La corbeille de ce Projet est vide",
                                    description = "Tout est propre ! Aucun Organ, membre ou tag n'attend d'être restauré."
                                )
                            }
                        } else {
                            // Section 1: Organs
                            if (viewModel.trashedOrgans.isNotEmpty()) {
                                item {
                                    TrashSectionHeader(
                                        title = "Organs",
                                        isCollapsed = organsCollapsed,
                                        onToggle = { organsCollapsed = !organsCollapsed }
                                    )
                                }

                                if (!organsCollapsed) {
                                    if (filteredOrgans.isEmpty()) {
                                        item { Text("Aucun Organ ne correspond à la recherche.", style = MaterialTheme.typography.bodySmall, color = Color.Gray) }
                                    } else {
                                        items(filteredOrgans) { organ ->
                                            TrashedItemCard(
                                                title = organ.title,
                                                subtitle = "Supprimé le ${formatDeletedDate(organ.deletedAt)}",
                                                iconVisual = organ.visual,
                                                iconRes = R.drawable.ic_organ_cube_placeholder,
                                                iconTint = highlightColor,
                                                iconBg = highlightColor.copy(alpha = 0.1f),
                                                onRestore = {
                                                    actionType = "organ"
                                                    actionUuid = organ.uuid
                                                    actionTitle = organ.title
                                                    actionOp = "restore"
                                                    showConfirmDialog = true
                                                },
                                                onDeletePermanent = {
                                                    actionType = "organ"
                                                    actionUuid = organ.uuid
                                                    actionTitle = organ.title
                                                    actionOp = "delete"
                                                    showConfirmDialog = true
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            // Section 2: Members
                            if (viewModel.trashedMembers.isNotEmpty()) {
                                item {
                                    TrashSectionHeader(
                                        title = "Membres",
                                        isCollapsed = membersCollapsed,
                                        onToggle = { membersCollapsed = !membersCollapsed }
                                    )
                                }

                                if (!membersCollapsed) {
                                    // Bulk Member Action Panel
                                    if (viewModel.selectedMembers.isNotEmpty()) {
                                        item {
                                            Surface(
                                                color = highlightColor.copy(alpha = 0.05f),
                                                shape = RoundedCornerShape(12.dp),
                                                border = BorderStroke(1.dp, highlightColor.copy(alpha = 0.2f)),
                                                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(12.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text(
                                                        text = "${viewModel.selectedMembers.size} membre(s) sélectionné(s)",
                                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                        color = highlightColor
                                                    )
                                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                        TextButton(onClick = { viewModel.clearMemberSelection() }) {
                                                            Text("Annuler", color = Color.Gray)
                                                        }
                                                        Button(
                                                            onClick = {
                                                                actionType = "bulk_members"
                                                                actionTitle = "${viewModel.selectedMembers.size} membres"
                                                                actionOp = "restore"
                                                                showConfirmDialog = true
                                                            },
                                                            colors = ButtonDefaults.buttonColors(containerColor = highlightColor, contentColor = Color.White),
                                                            shape = RoundedCornerShape(8.dp)
                                                        ) {
                                                            Text("Restaurer", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    if (filteredMembers.isEmpty()) {
                                        item { Text("Aucun membre ne correspond à la recherche.", style = MaterialTheme.typography.bodySmall, color = Color.Gray) }
                                    } else {
                                        items(filteredMembers) { member ->
                                            val fullName = listOf(member.user.firstName, member.user.lastName).filter { it.isNotBlank() }.joinToString(" ").ifBlank { member.user.email }
                                            val isSelected = viewModel.selectedMembers.contains(member.uuid)
                                            TrashedItemCard(
                                                title = fullName,
                                                subtitle = "Rôle : ${member.role} • Supprimé le ${formatDeletedDate(member.deletedAt)}",
                                                iconRes = R.drawable.icon_account,
                                                iconTint = Color(0xFFFBBF24),
                                                iconBg = Color(0xFFFBBF24).copy(alpha = 0.1f),
                                                showCheckbox = true,
                                                isCheckboxChecked = isSelected,
                                                onCheckboxClick = { checked ->
                                                    viewModel.toggleMemberSelection(member.uuid)
                                                },
                                                onRestore = {
                                                    actionType = "member"
                                                    actionUuid = member.uuid
                                                    actionTitle = fullName
                                                    actionOp = "restore"
                                                    showConfirmDialog = true
                                                },
                                                onDeletePermanent = {
                                                    actionType = "member"
                                                    actionUuid = member.uuid
                                                    actionTitle = fullName
                                                    actionOp = "delete"
                                                    showConfirmDialog = true
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            // Section 3: Tags
                            if (viewModel.trashedTags.isNotEmpty()) {
                                item {
                                    TrashSectionHeader(
                                        title = "Tags",
                                        isCollapsed = tagsCollapsed,
                                        onToggle = { tagsCollapsed = !tagsCollapsed }
                                    )
                                }

                                if (!tagsCollapsed) {
                                    if (filteredTags.isEmpty()) {
                                        item { Text("Aucun tag ne correspond à la recherche.", style = MaterialTheme.typography.bodySmall, color = Color.Gray) }
                                    } else {
                                        items(filteredTags) { tag ->
                                            val tagColor = remember(tag.color) {
                                                try {
                                                    Color(android.graphics.Color.parseColor(tag.color))
                                                } catch (e: Exception) {
                                                    Color.Gray
                                                }
                                            }
                                            TrashedItemCard(
                                                title = tag.name,
                                                subtitle = "Supprimé le ${formatDeletedDate(tag.deletedAt)}",
                                                iconRes = R.drawable.outline_tag,
                                                iconTint = tagColor,
                                                iconBg = tagColor.copy(alpha = 0.1f),
                                                onRestore = {
                                                    actionType = "tag"
                                                    actionUuid = tag.uuid
                                                    actionTitle = tag.name
                                                    actionOp = "restore"
                                                    showConfirmDialog = true
                                                },
                                                onDeletePermanent = {
                                                    actionType = "tag"
                                                    actionUuid = tag.uuid
                                                    actionTitle = tag.name
                                                    actionOp = "delete"
                                                    showConfirmDialog = true
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (viewModel.isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.15f))
                            .clickable(
                                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                indication = null
                            ) {},
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = highlightColor)
                    }
                }
            }
        }
    }

    // Confirmation dialog
    if (showConfirmDialog) {
        val opIsRestore = actionOp == "restore"
        DeleteConfirmDialog(
            title = "Confirmation",
            message = if (opIsRestore) {
                "Voulez-vous restaurer \"$actionTitle\" ?"
            } else {
                "Voulez-vous supprimer \"$actionTitle\" définitivement ? Cette action est irréversible."
            },
            confirmText = if (opIsRestore) "Restaurer" else "Supprimer",
            iconRes = if (opIsRestore) R.drawable.outline_info else R.drawable.poubelle_logo,
            iconColor = if (opIsRestore) Color(0xFF0284C7) else Color.Red,
            iconBgColor = if (opIsRestore) Color(0xFFE0F2FE) else Color(0xFFFFF1F2),
            onConfirm = {
                showConfirmDialog = false
                val type = actionType ?: return@DeleteConfirmDialog
                val op = actionOp
                if (op == "restore") {
                    when (type) {
                        "organ" -> viewModel.restoreOrgan(projectUuid, actionUuid, {}, {})
                        "member" -> viewModel.restoreMember(projectUuid, actionUuid, {}, {})
                        "tag" -> viewModel.restoreTag(projectUuid, actionUuid, {}, {})
                        "bulk_members" -> viewModel.restoreSelectedMembers(projectUuid, {}, {})
                    }
                } else {
                    when (type) {
                        "organ" -> viewModel.deleteOrganPermanently(projectUuid, actionUuid, {}, {})
                        "member" -> viewModel.deleteMemberPermanently(projectUuid, actionUuid, {}, {})
                        "tag" -> viewModel.deleteTagPermanently(projectUuid, actionUuid, {}, {})
                    }
                }
            },
            onDismiss = { showConfirmDialog = false }
        )
    }
}






