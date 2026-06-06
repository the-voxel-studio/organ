package fr.studio.voxel.organ.ui.organ.trash

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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import fr.studio.voxel.organ.R
import fr.studio.voxel.organ.network.services.*
import fr.studio.voxel.organ.ui.components.*
import fr.studio.voxel.organ.ui.header.Header
import fr.studio.voxel.organ.ui.project.AccessDeniedScreen
import fr.studio.voxel.organ.viewmodel.OrganTrashViewModel
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

@Composable
fun OrganTrashScreen(
    projectUuid: String,
    organUuid: String,
    onBack: () -> Unit,
    onTaskClick: (String) -> Unit,
    viewModel: OrganTrashViewModel = viewModel()
) {
    LaunchedEffect(projectUuid, organUuid) {
        viewModel.loadTrash(projectUuid, organUuid)
    }

    var searchQuery by remember { mutableStateOf("") }
    
    // Collapsing states
    var tasksCollapsed by remember { mutableStateOf(false) }
    var rolesCollapsed by remember { mutableStateOf(false) }
    var membersCollapsed by remember { mutableStateOf(false) }
    var linksCollapsed by remember { mutableStateOf(false) }

    // Action dialog states
    var showConfirmDialog by remember { mutableStateOf(false) }
    var actionType by remember { mutableStateOf<String?>(null) } // "task", "role", "member", "link"
    var actionUuid by remember { mutableStateOf("") }
    var actionIntId by remember { mutableStateOf(-1) }
    var actionTitle by remember { mutableStateOf("") }
    var actionOp by remember { mutableStateOf("") } // "restore", "delete"

    val highlightColor = remember(viewModel.highlightColor) {
        try {
            Color(android.graphics.Color.parseColor(viewModel.highlightColor))
        } catch (e: Exception) {
            Color(0xFFFF7DD4)
        }
    }

    // Filter calculations
    val filteredTasks = remember(viewModel.trashedTasks, searchQuery) {
        val query = searchQuery.lowercase().trim()
        if (query.isBlank()) viewModel.trashedTasks
        else viewModel.trashedTasks.filter { it.title.lowercase().contains(query) }
    }

    val filteredRoles = remember(viewModel.trashedRoles, searchQuery) {
        val query = searchQuery.lowercase().trim()
        if (query.isBlank()) viewModel.trashedRoles
        else viewModel.trashedRoles.filter { it.name.lowercase().contains(query) }
    }

    val filteredMembers = remember(viewModel.trashedMembers, searchQuery) {
        val query = searchQuery.lowercase().trim()
        if (query.isBlank()) viewModel.trashedMembers
        else viewModel.trashedMembers.filter {
            it.user.firstName.lowercase().contains(query) ||
                    it.user.lastName.lowercase().contains(query) ||
                    it.role.name.lowercase().contains(query)
        }
    }

    val filteredLinks = remember(viewModel.trashedLinks, searchQuery) {
        val query = searchQuery.lowercase().trim()
        if (query.isBlank()) viewModel.trashedLinks
        else viewModel.trashedLinks.filter {
            it.url.lowercase().contains(query) ||
                    (it.description != null && it.description.lowercase().contains(query))
        }
    }

    val hasTrashedItems = viewModel.trashedTasks.isNotEmpty() ||
            viewModel.trashedRoles.isNotEmpty() ||
            viewModel.trashedMembers.isNotEmpty() ||
            viewModel.trashedLinks.isNotEmpty()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        if (viewModel.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = highlightColor)
            }
        } else if (viewModel.accessDenied) {
            AccessDeniedScreen(onBack = onBack)
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Header(navigateUp = onBack, canOpenSidebar = false)

                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Back link & Title
                    item {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            BackToLink(
                                label = "Retour à l'Organ",
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
                                    text = "Corbeille - ${viewModel.organTitle}",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                                    color = Color.Black
                                )
                            }

                            Text(
                                text = "Retrouvez ici les éléments supprimés auxquels vous avez accès.",
                                style = MaterialTheme.typography.bodyLarge,
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
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = highlightColor,
                                unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color(0xFFF9F9F9)
                            )
                        )
                    }

                    // Empty state fallback
                    if (!hasTrashedItems) {
                        item {
                            EmptyTrashState(
                                title = "La corbeille de cet Organ est vide",
                                description = "Tout est à sa place ! Aucun élément n'attend d'être restauré."
                            )
                        }
                    } else {
                        // Section 1: Tasks
                        if (viewModel.canManageTasks() && viewModel.trashedTasks.isNotEmpty()) {
                            item {
                                TrashSectionHeader(
                                    title = "Tâches",
                                    isCollapsed = tasksCollapsed,
                                    onToggle = { tasksCollapsed = !tasksCollapsed }
                                )
                            }

                            if (!tasksCollapsed) {
                                if (filteredTasks.isEmpty()) {
                                    item { Text("Aucune tâche ne correspond à la recherche.", style = MaterialTheme.typography.bodyLarge, color = Color.Gray) }
                                } else {
                                    items(filteredTasks) { task ->
                                        TrashedItemCard(
                                            title = task.title,
                                            subtitle = "Supprimée le ${formatDeletedDate(task.deletedAt)}",
                                            iconRes = R.drawable.menu_tache,
                                            iconTint = highlightColor,
                                            iconBg = highlightColor.copy(alpha = 0.1f),
                                            onItemClick = { onTaskClick(task.uuid) },
                                            onRestore = {
                                                actionType = "task"
                                                actionUuid = task.uuid
                                                actionTitle = task.title
                                                actionOp = "restore"
                                                showConfirmDialog = true
                                            },
                                            onDeletePermanent = {
                                                actionType = "task"
                                                actionUuid = task.uuid
                                                actionTitle = task.title
                                                actionOp = "delete"
                                                showConfirmDialog = true
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // Section 2: Roles
                        if (viewModel.canManageRoles() && viewModel.trashedRoles.isNotEmpty()) {
                            item {
                                TrashSectionHeader(
                                    title = "Rôles",
                                    isCollapsed = rolesCollapsed,
                                    onToggle = { rolesCollapsed = !rolesCollapsed }
                                )
                            }

                            if (!rolesCollapsed) {
                                if (filteredRoles.isEmpty()) {
                                    item { Text("Aucun rôle ne correspond à la recherche.", style = MaterialTheme.typography.bodyLarge, color = Color.Gray) }
                                } else {
                                    items(filteredRoles) { role ->
                                        TrashedItemCard(
                                            title = role.name,
                                            subtitle = "Supprimé le ${formatDeletedDate(role.deletedAt)}",
                                            iconEmoji = role.iconData ?: "🎭",
                                            iconBg = highlightColor.copy(alpha = 0.1f),
                                            onRestore = {
                                                actionType = "role"
                                                actionUuid = role.uuid
                                                actionTitle = role.name
                                                actionOp = "restore"
                                                showConfirmDialog = true
                                            },
                                            onDeletePermanent = {
                                                actionType = "role"
                                                actionUuid = role.uuid
                                                actionTitle = role.name
                                                actionOp = "delete"
                                                showConfirmDialog = true
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // Section 3: Members
                        if (viewModel.canManageRoles() && viewModel.trashedMembers.isNotEmpty()) {
                            item {
                                TrashSectionHeader(
                                    title = "Membres",
                                    isCollapsed = membersCollapsed,
                                    onToggle = { membersCollapsed = !membersCollapsed }
                                )
                            }

                            if (!membersCollapsed) {
                                if (filteredMembers.isEmpty()) {
                                    item { Text("Aucun membre ne correspond à la recherche.", style = MaterialTheme.typography.bodyLarge, color = Color.Gray) }
                                } else {
                                    items(filteredMembers) { assignment ->
                                        val fullName = "${assignment.user.firstName} ${assignment.user.lastName}"
                                        TrashedItemCard(
                                            title = fullName,
                                            subtitle = "Rôle précédent : ${assignment.role.name}\nRetiré le ${formatDeletedDate(assignment.deletedAt)}",
                                            iconRes = R.drawable.icon_account,
                                            iconTint = Color(0xFFFBBF24),
                                            iconBg = Color(0xFFFBBF24).copy(alpha = 0.1f),
                                            onRestore = {
                                                actionType = "member"
                                                actionIntId = assignment.uuid
                                                actionTitle = fullName
                                                actionOp = "restore"
                                                showConfirmDialog = true
                                            },
                                            onDeletePermanent = {
                                                actionType = "member"
                                                actionIntId = assignment.uuid
                                                actionTitle = fullName
                                                actionOp = "delete"
                                                showConfirmDialog = true
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // Section 4: Links
                        if (viewModel.canManageLinks() && viewModel.trashedLinks.isNotEmpty()) {
                            item {
                                TrashSectionHeader(
                                    title = "Liens",
                                    isCollapsed = linksCollapsed,
                                    onToggle = { linksCollapsed = !linksCollapsed }
                                )
                            }

                            if (!linksCollapsed) {
                                if (filteredLinks.isEmpty()) {
                                    item { Text("Aucun lien ne correspond à la recherche.", style = MaterialTheme.typography.bodyLarge, color = Color.Gray) }
                                } else {
                                    items(filteredLinks) { link ->
                                        val displayTitle = link.description ?: link.url
                                        TrashedItemCard(
                                            title = displayTitle,
                                            subtitle = (if (link.description != null) link.url + "\n" else "") + "Supprimé le ${formatDeletedDate(link.deletedAt)}",
                                            iconRes = R.drawable.projet_folder, // folder/link icon
                                            iconTint = Color(0xFF355EE4),
                                            iconBg = Color(0xFF355EE4).copy(alpha = 0.1f),
                                            onRestore = {
                                                actionType = "link"
                                                actionUuid = link.uuid
                                                actionTitle = displayTitle
                                                actionOp = "restore"
                                                showConfirmDialog = true
                                            },
                                            onDeletePermanent = {
                                                actionType = "link"
                                                actionUuid = link.uuid
                                                actionTitle = displayTitle
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
                        "task" -> viewModel.restoreTask(projectUuid, organUuid, actionUuid, {}, {})
                        "role" -> viewModel.restoreRole(projectUuid, organUuid, actionUuid, {}, {})
                        "member" -> viewModel.restoreMember(projectUuid, organUuid, actionIntId, {}, {})
                        "link" -> viewModel.restoreLink(projectUuid, organUuid, actionUuid, {}, {})
                    }
                } else {
                    when (type) {
                        "task" -> viewModel.deletePermanentlyTask(projectUuid, organUuid, actionUuid, {}, {})
                        "role" -> viewModel.deletePermanentlyRole(projectUuid, organUuid, actionUuid, {}, {})
                        "member" -> viewModel.deletePermanentlyMember(projectUuid, organUuid, actionIntId, {}, {})
                        "link" -> viewModel.deletePermanentlyLink(projectUuid, organUuid, actionUuid, {}, {})
                    }
                }
            },
            onDismiss = { showConfirmDialog = false }
        )
    }
}






