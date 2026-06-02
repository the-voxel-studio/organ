package fr.studio.voxel.organ.ui.task

import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import fr.studio.voxel.organ.R
import fr.studio.voxel.organ.network.services.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Edit
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import java.util.Calendar
import fr.studio.voxel.organ.ui.components.LoadingOverlay
import fr.studio.voxel.organ.ui.components.PrimaryButton
import fr.studio.voxel.organ.ui.theme.MaterialColorScheme
import fr.studio.voxel.organ.viewmodel.TaskDetailsViewModel
import androidx.compose.foundation.layout.ExperimentalLayoutApi

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TaskDetailsScreen(
    projectUuid: String,
    organUuid: String,
    taskUuid: String,
    onBack: () -> Unit,
    onSuccess: () -> Unit,
    viewModel: TaskDetailsViewModel = viewModel()
) {
    val context = LocalContext.current

    LaunchedEffect(projectUuid, organUuid, taskUuid) {
        viewModel.initTask(projectUuid, organUuid, taskUuid)
    }

    LaunchedEffect(viewModel.isSuccess) {
        if (viewModel.isSuccess) {
            onSuccess()
        }
    }

    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var isDeletePermanent by remember { mutableStateOf(false) }

    // Dropdowns / additions state
    var showAddAssigneeMenu by remember { mutableStateOf(false) }
    var showAddTagMenu by remember { mutableStateOf(false) }
    var showAddDependencyMenu by remember { mutableStateOf(false) }

    // Expandable sections states
    var showAssigneesExpanded by remember { mutableStateOf(false) }
    var showTagsExpanded by remember { mutableStateOf(false) }
    var showLinksExpanded by remember { mutableStateOf(false) }
    var showDepsExpanded by remember { mutableStateOf(false) }
    var showAttachmentsExpanded by remember { mutableStateOf(false) }
    var showCommentsExpanded by remember { mutableStateOf(false) }
    var showTimelineExpanded by remember { mutableStateOf(false) }

    var showAttachmentSourceDialog by remember { mutableStateOf(false) }
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempCameraUri != null) {
            val contentResolver = context.contentResolver
            try {
                val bytes = contentResolver.openInputStream(tempCameraUri!!)?.readBytes()
                if (bytes != null) {
                    val mimeType = "image/jpeg"
                    viewModel.uploadAttachment("camera_photo.jpg", mimeType, bytes)
                }
            } catch (e: Exception) {
                Log.e("TASK_DETAILS_SCREEN", "Camera photo read error", e)
            }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val uri = fr.studio.voxel.organ.ui.components.createTempImageUri(context)
            tempCameraUri = uri
            cameraLauncher.launch(uri)
        }
    }

    // Input fields for sub-resources
    var newCommentText by remember { mutableStateOf("") }
    var newLinkUrl by remember { mutableStateOf("") }
    var newLinkDesc by remember { mutableStateOf("") }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val contentResolver = context.contentResolver
            var fileName = "file"
            contentResolver.query(it, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index >= 0) {
                        fileName = cursor.getString(index)
                    }
                }
            }
            try {
                val bytes = contentResolver.openInputStream(it)?.readBytes()
                if (bytes != null) {
                    val mimeType = contentResolver.getType(it) ?: "application/octet-stream"
                    viewModel.uploadAttachment(fileName, mimeType, bytes)
                }
            } catch (e: Exception) {
                Log.e("TASK_DETAILS_SCREEN", "File read error", e)
            }
        }
    }

    val isTrashed = viewModel.taskData?.deletedAt != null

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        LoadingOverlay(
            isLoading = viewModel.isLoading || viewModel.isSaving,
            text = if (viewModel.isSaving) "Enregistrement de la tâche..." else "Chargement..."
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                // PC Modal style Top Bar: croix | titre | supprimer
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Fermer",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isTrashed) "Tâche supprimée" else viewModel.taskData?.title ?: "Modifier la tâche",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Permissions Based Action Buttons
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Restore Button
                        if (isTrashed && viewModel.hasPerm("TASK_RESTORE", viewModel.isTaskOwner())) {
                            Button(
                                onClick = { viewModel.restoreTask() },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text("Restaurer", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }

                        // Soft/Hard delete Button
                        val canHardDelete = isTrashed && viewModel.hasPerm("TASK_HARD_DELETE", viewModel.isTaskOwner())
                        val canSoftDelete = !isTrashed && viewModel.hasPerm("TASK_DELETE", viewModel.isTaskOwner())

                        if (canHardDelete || canSoftDelete) {
                            IconButton(
                                onClick = {
                                    isDeletePermanent = isTrashed
                                    showDeleteConfirmDialog = true
                                }
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.poubelle_logo),
                                    contentDescription = if (isTrashed) "Supprimer définitivement" else "Supprimer la tâche",
                                    tint = Color(0xFFEF4444),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }

                // Scrollable Content Pane
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {

                    // 1. Basic Details Form Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            verticalArrangement = Arrangement.spacedBy(20.dp)
                        ) {
                            Text(
                                text = "Informations de base",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            // Titre
                            Column {
                                Text(
                                    text = "Titre de la tâche *",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                OutlinedTextField(
                                    value = viewModel.title,
                                    onValueChange = { if (viewModel.canEditField("title")) viewModel.title = it },
                                    placeholder = { Text("Ex: Finaliser l'UI") },
                                    singleLine = true,
                                    readOnly = !viewModel.canEditField("title"),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            // Description
                            Column {
                                Text(
                                    text = "Description",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                OutlinedTextField(
                                    value = viewModel.description,
                                    onValueChange = { if (viewModel.canEditField("description")) viewModel.description = it },
                                    placeholder = { Text("Description détaillée de la tâche...") },
                                    minLines = 3,
                                    maxLines = 6,
                                    readOnly = !viewModel.canEditField("description"),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            // Status & Priority
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                // Status
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Statut",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))

                                    var statusExpanded by remember { mutableStateOf(false) }
                                    val canEditStatus = viewModel.canEditField("status")
                                    Box {
                                        OutlinedTextField(
                                            value = when (viewModel.status) {
                                                "TODO" -> "À faire"
                                                "IN_PROGRESS" -> "En cours"
                                                "WAITING" -> "En attente"
                                                "DONE" -> "Terminé"
                                                "CANCELED" -> "Annulé"
                                                else -> viewModel.status
                                            },
                                            onValueChange = {},
                                            readOnly = true,
                                            shape = RoundedCornerShape(10.dp),
                                            trailingIcon = {
                                                if (canEditStatus) {
                                                    IconButton(onClick = { statusExpanded = true }) {
                                                        Icon(
                                                            painter = painterResource(R.drawable.flechedroite_logo),
                                                            contentDescription = "Changer le statut",
                                                            modifier = Modifier.rotate(90f).size(16.dp)
                                                        )
                                                    }
                                                }
                                            },
                                            modifier = Modifier.fillMaxWidth().clickable(enabled = canEditStatus) { statusExpanded = true }
                                        )

                                        if (canEditStatus) {
                                            DropdownMenu(
                                                expanded = statusExpanded,
                                                onDismissRequest = { statusExpanded = false }
                                            ) {
                                                listOf(
                                                    "TODO" to "À faire",
                                                    "IN_PROGRESS" to "En cours",
                                                    "WAITING" to "En attente",
                                                    "DONE" to "Terminé",
                                                    "CANCELED" to "Annulé"
                                                ).forEach { (code, label) ->
                                                    DropdownMenuItem(
                                                        text = { Text(label) },
                                                        onClick = {
                                                            viewModel.status = code
                                                            statusExpanded = false
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                // Priority
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Priorité",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))

                                    var priorityExpanded by remember { mutableStateOf(false) }
                                    val canEditPriority = viewModel.canEditField("priority")
                                    Box {
                                        OutlinedTextField(
                                            value = "${viewModel.priority} - " + when {
                                                viewModel.priority <= 3 -> "Faible"
                                                viewModel.priority <= 7 -> "Moyenne"
                                                else -> "Urgent"
                                            },
                                            onValueChange = {},
                                            readOnly = true,
                                            shape = RoundedCornerShape(10.dp),
                                            trailingIcon = {
                                                if (canEditPriority) {
                                                    IconButton(onClick = { priorityExpanded = true }) {
                                                        Icon(
                                                            painter = painterResource(R.drawable.flechedroite_logo),
                                                            contentDescription = "Changer la priorité",
                                                            modifier = Modifier.rotate(90f).size(16.dp)
                                                        )
                                                    }
                                                }
                                            },
                                            modifier = Modifier.fillMaxWidth().clickable(enabled = canEditPriority) { priorityExpanded = true }
                                        )

                                        if (canEditPriority) {
                                            DropdownMenu(
                                                expanded = priorityExpanded,
                                                onDismissRequest = { priorityExpanded = false }
                                            ) {
                                                (1..10).forEach { p ->
                                                    DropdownMenuItem(
                                                        text = {
                                                            Text("$p - " + when {
                                                                p <= 3 -> "Faible"
                                                                p <= 7 -> "Moyenne"
                                                                else -> "Urgent"
                                                            })
                                                        },
                                                        onClick = {
                                                            viewModel.priority = p
                                                            priorityExpanded = false
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Optional Status message
                            if (viewModel.status != viewModel.taskData?.status) {
                                Column {
                                    Text(
                                        text = "Message de changement de statut",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    OutlinedTextField(
                                        value = viewModel.statusMessage,
                                        onValueChange = { viewModel.statusMessage = it },
                                        placeholder = { Text("Indiquez la raison du changement (facultatif)...") },
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }

                            // Manager
                            Column {
                                Text(
                                    text = "Responsable (Manager)",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                )
                                Spacer(modifier = Modifier.height(6.dp))

                                var managerExpanded by remember { mutableStateOf(false) }
                                val canEditManager = viewModel.canEditField("managerUuid")
                                val selectedManager = viewModel.projectMembers.find { it.user.uuid == viewModel.managerUuid }
                                Box {
                                    OutlinedTextField(
                                        value = selectedManager?.let { "${it.user.firstName} ${it.user.lastName}" } ?: "Aucun responsable",
                                        onValueChange = {},
                                        readOnly = true,
                                        shape = RoundedCornerShape(10.dp),
                                        trailingIcon = {
                                            if (canEditManager) {
                                                IconButton(onClick = { managerExpanded = true }) {
                                                    Icon(
                                                        painter = painterResource(R.drawable.flechedroite_logo),
                                                        contentDescription = "Changer de responsable",
                                                        modifier = Modifier.rotate(90f).size(16.dp)
                                                    )
                                                }
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth().clickable(enabled = canEditManager) { managerExpanded = true }
                                    )

                                    if (canEditManager) {
                                        DropdownMenu(
                                            expanded = managerExpanded,
                                            onDismissRequest = { managerExpanded = false }
                                        ) {
                                            DropdownMenuItem(
                                                text = { Text("Aucun responsable") },
                                                onClick = {
                                                    viewModel.managerUuid = ""
                                                    managerExpanded = false
                                                }
                                            )
                                            viewModel.projectMembers.forEach { member ->
                                                DropdownMenuItem(
                                                    text = { Text("${member.user.firstName} ${member.user.lastName}") },
                                                    onClick = {
                                                        viewModel.managerUuid = member.user.uuid
                                                        managerExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Hours and Dates
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Temps estimé (Heures)",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    OutlinedTextField(
                                        value = viewModel.estimatedHours,
                                        onValueChange = { if (viewModel.canEditField("estimatedHours")) viewModel.estimatedHours = it },
                                        placeholder = { Text("Ex: 12.5") },
                                        readOnly = !viewModel.canEditField("estimatedHours"),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }

                             Row(
                                 modifier = Modifier.fillMaxWidth(),
                                 horizontalArrangement = Arrangement.spacedBy(16.dp)
                             ) {
                                 Column(modifier = Modifier.weight(1f)) {
                                     Text(
                                         text = "Date de début",
                                         style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                         color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                     )
                                     Spacer(modifier = Modifier.height(6.dp))
                                     val canEditStart = viewModel.canEditField("startDate")
                                     OutlinedTextField(
                                         value = formatDateTimeForDisplay(viewModel.startDate),
                                         onValueChange = {},
                                         placeholder = { Text("Choisir une date") },
                                         readOnly = true,
                                         shape = RoundedCornerShape(10.dp),
                                         trailingIcon = {
                                             if (canEditStart) {
                                                 IconButton(onClick = {
                                                     showDateTimePicker(context, viewModel.startDate) { selected ->
                                                         viewModel.startDate = selected
                                                     }
                                                 }) {
                                                     Icon(
                                                         imageVector = Icons.Default.Edit,
                                                         contentDescription = "Modifier la date"
                                                     )
                                                 }
                                             }
                                         },
                                         modifier = Modifier
                                             .fillMaxWidth()
                                             .clickable(enabled = canEditStart) {
                                                 showDateTimePicker(context, viewModel.startDate) { selected ->
                                                     viewModel.startDate = selected
                                                 }
                                             }
                                     )
                                 }

                                 Column(modifier = Modifier.weight(1f)) {
                                     Text(
                                         text = "Date d'échéance",
                                         style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                         color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                     )
                                     Spacer(modifier = Modifier.height(6.dp))
                                     val canEditExpires = viewModel.canEditField("expiresAt")
                                     OutlinedTextField(
                                         value = formatDateTimeForDisplay(viewModel.expiresAt),
                                         onValueChange = {},
                                         placeholder = { Text("Choisir une date") },
                                         readOnly = true,
                                         shape = RoundedCornerShape(10.dp),
                                         trailingIcon = {
                                             if (canEditExpires) {
                                                 IconButton(onClick = {
                                                     showDateTimePicker(context, viewModel.expiresAt) { selected ->
                                                         viewModel.expiresAt = selected
                                                     }
                                                 }) {
                                                     Icon(
                                                         imageVector = Icons.Default.Edit,
                                                         contentDescription = "Modifier la date"
                                                     )
                                                 }
                                             }
                                         },
                                         modifier = Modifier
                                             .fillMaxWidth()
                                             .clickable(enabled = canEditExpires) {
                                                 showDateTimePicker(context, viewModel.expiresAt) { selected ->
                                                     viewModel.expiresAt = selected
                                                 }
                                             }
                                     )
                                 }
                             }
                        }
                    }

                    // 2. Assignees Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            val isOwner = viewModel.isTaskOwner()
                            val canManageAssignees = !isTrashed && (
                                viewModel.hasPerm("TASK_ASSIGN_OTHERS", isOwner) ||
                                viewModel.hasPerm("TASK_ASSIGN_SELF") ||
                                viewModel.hasPerm("TASK_EDIT", isOwner)
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showAssigneesExpanded = !showAssigneesExpanded }
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Membres assignés",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Icon(
                                    painter = painterResource(R.drawable.flechedroite_logo),
                                    contentDescription = if (showAssigneesExpanded) "Réduire" else "Développer",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier
                                        .rotate(if (showAssigneesExpanded) 270f else 90f)
                                        .size(16.dp)
                                )
                            }

                            AnimatedVisibility(
                                visible = showAssigneesExpanded,
                                enter = expandVertically() + fadeIn(),
                                exit = shrinkVertically() + fadeOut()
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    if (viewModel.assignees.isEmpty()) {
                                        Text("Aucun collaborateur assigné", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                                    } else {
                                        FlowRow(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            viewModel.assignees.forEach { assignee ->
                                                AssistChip(
                                                    onClick = {},
                                                    label = { Text("${assignee.firstName} ${assignee.lastName}") },
                                                    trailingIcon = {
                                                        if (canManageAssignees) {
                                                            Icon(
                                                                imageVector = Icons.Default.Close,
                                                                contentDescription = "Retirer",
                                                                tint = Color(0xFFEF4444),
                                                                modifier = Modifier
                                                                    .size(16.dp)
                                                                    .clickable { viewModel.removeAssignee(assignee.uuid) }
                                                            )
                                                        }
                                                    },
                                                    shape = RoundedCornerShape(12.dp)
                                                )
                                            }
                                        }
                                    }

                                    if (canManageAssignees) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Box {
                                            Text(
                                                text = "+ Ajouter",
                                                color = MaterialColorScheme.primary,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier
                                                    .clickable { showAddAssigneeMenu = true }
                                                    .padding(8.dp)
                                            )

                                            val availableMembers = viewModel.projectMembers.filter { pm ->
                                                !viewModel.assignees.any { a -> a.uuid == pm.user.uuid }
                                            }

                                            DropdownMenu(
                                                expanded = showAddAssigneeMenu,
                                                onDismissRequest = { showAddAssigneeMenu = false }
                                            ) {
                                                if (availableMembers.isEmpty()) {
                                                    DropdownMenuItem(
                                                        text = { Text("Aucun autre membre") },
                                                        onClick = { showAddAssigneeMenu = false }
                                                    )
                                                } else {
                                                    availableMembers.forEach { pm ->
                                                        DropdownMenuItem(
                                                            text = { Text("${pm.user.firstName} ${pm.user.lastName}") },
                                                            onClick = {
                                                                viewModel.addAssignee(pm.user.uuid)
                                                                showAddAssigneeMenu = false
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

                    // 3. Tags Section Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            val isOwner = viewModel.isTaskOwner()
                            val canManageTags = !isTrashed && (
                                viewModel.hasPerm("TASK_TAG_MANAGE", isOwner) ||
                                viewModel.hasPerm("TASK_EDIT", isOwner)
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showTagsExpanded = !showTagsExpanded }
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Tags de la tâche",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Icon(
                                    painter = painterResource(R.drawable.flechedroite_logo),
                                    contentDescription = if (showTagsExpanded) "Réduire" else "Développer",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier
                                        .rotate(if (showTagsExpanded) 270f else 90f)
                                        .size(16.dp)
                                )
                            }

                            AnimatedVisibility(
                                visible = showTagsExpanded,
                                enter = expandVertically() + fadeIn(),
                                exit = shrinkVertically() + fadeOut()
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    val taskTags = viewModel.taskData?.tags ?: emptyList()
                                    if (taskTags.isEmpty()) {
                                        Text("Aucun tag associé", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                                    } else {
                                        FlowRow(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            taskTags.forEach { tag ->
                                                val tagColor = try {
                                                    Color(android.graphics.Color.parseColor(if (tag.color.startsWith("#")) tag.color else "#${tag.color}"))
                                                } catch (e: Exception) {
                                                    MaterialColorScheme.primary
                                                }
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(tagColor.copy(alpha = 0.15f))
                                                        .border(1.dp, tagColor.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                                ) {
                                                    Text(
                                                        text = tag.name,
                                                        color = tagColor,
                                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                                    )
                                                    if (canManageTags) {
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Icon(
                                                            imageVector = Icons.Default.Close,
                                                            contentDescription = "Enlever",
                                                            tint = tagColor,
                                                            modifier = Modifier
                                                                .size(12.dp)
                                                                .clickable { viewModel.removeTag(tag.uuid) }
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    if (canManageTags) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Box {
                                            Text(
                                                text = "+ Associer",
                                                color = MaterialColorScheme.primary,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier
                                                    .clickable { showAddTagMenu = true }
                                                    .padding(8.dp)
                                            )

                                            val currentTags = viewModel.taskData?.tags ?: emptyList()
                                            val remainingTags = viewModel.allProjectTags.filter { t ->
                                                !currentTags.any { ct -> ct.uuid == t.uuid }
                                            }

                                            DropdownMenu(
                                                expanded = showAddTagMenu,
                                                onDismissRequest = { showAddTagMenu = false }
                                            ) {
                                                if (remainingTags.isEmpty()) {
                                                    DropdownMenuItem(
                                                        text = { Text("Aucun autre tag") },
                                                        onClick = { showAddTagMenu = false }
                                                    )
                                                } else {
                                                    remainingTags.forEach { tag ->
                                                        DropdownMenuItem(
                                                            text = { Text(tag.name) },
                                                            onClick = {
                                                                viewModel.addTag(tag.uuid)
                                                                showAddTagMenu = false
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

                    // 4. Links Section Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            val isOwner = viewModel.isTaskOwner()
                            val canManageLinks = !isTrashed && (
                                viewModel.hasPerm("TASK_LINK_MANAGE", isOwner) ||
                                viewModel.hasPerm("TASK_EDIT", isOwner)
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showLinksExpanded = !showLinksExpanded }
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Liens associés",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Icon(
                                    painter = painterResource(R.drawable.flechedroite_logo),
                                    contentDescription = if (showLinksExpanded) "Réduire" else "Développer",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier
                                        .rotate(if (showLinksExpanded) 270f else 90f)
                                        .size(16.dp)
                                )
                            }

                            AnimatedVisibility(
                                visible = showLinksExpanded,
                                enter = expandVertically() + fadeIn(),
                                exit = shrinkVertically() + fadeOut()
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    if (viewModel.links.isEmpty()) {
                                        Text("Aucun lien", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                                    } else {
                                        viewModel.links.forEach { link ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = link.description ?: link.url,
                                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                        color = MaterialColorScheme.primary,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    if (link.description != null) {
                                                        Text(
                                                            text = link.url,
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = MaterialTheme.colorScheme.outline,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    }
                                                }
                                                if (canManageLinks) {
                                                    IconButton(onClick = { viewModel.deleteLink(link.uuid) }) {
                                                        Icon(
                                                            imageVector = Icons.Default.Close,
                                                            contentDescription = "Supprimer",
                                                            tint = Color(0xFFEF4444),
                                                            modifier = Modifier.size(20.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    if (canManageLinks) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            OutlinedTextField(
                                                value = newLinkUrl,
                                                onValueChange = { newLinkUrl = it },
                                                placeholder = { Text("https://example.com") },
                                                label = { Text("URL du lien") },
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                            OutlinedTextField(
                                                value = newLinkDesc,
                                                onValueChange = { newLinkDesc = it },
                                                placeholder = { Text("Description (facultatif)") },
                                                label = { Text("Description") },
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                            Button(
                                                onClick = {
                                                    if (newLinkUrl.isNotBlank()) {
                                                        viewModel.addLink(newLinkUrl, if (newLinkDesc.isNotBlank()) newLinkDesc else null)
                                                        newLinkUrl = ""
                                                        newLinkDesc = ""
                                                    }
                                                },
                                                shape = RoundedCornerShape(10.dp)
                                            ) {
                                                Text("Ajouter le lien")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 5. Dependencies Section Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            val isOwner = viewModel.isTaskOwner()
                            val canManageDeps = !isTrashed && (
                                viewModel.hasPerm("TASK_DEPENDENCY_MANAGE", isOwner) ||
                                viewModel.hasPerm("TASK_EDIT", isOwner)
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showDepsExpanded = !showDepsExpanded }
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Dépendances",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Icon(
                                    painter = painterResource(R.drawable.flechedroite_logo),
                                    contentDescription = if (showDepsExpanded) "Réduire" else "Développer",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier
                                        .rotate(if (showDepsExpanded) 270f else 90f)
                                        .size(16.dp)
                                )
                            }

                            AnimatedVisibility(
                                visible = showDepsExpanded,
                                enter = expandVertically() + fadeIn(),
                                exit = shrinkVertically() + fadeOut()
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    if (viewModel.dependencies.isEmpty()) {
                                        Text("Aucune dépendance", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                                    } else {
                                        viewModel.dependencies.forEach { dep ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = dep.title,
                                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(6.dp))
                                                            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                                    ) {
                                                        Text(dep.status, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                    if (canManageDeps) {
                                                        IconButton(onClick = { viewModel.removeDependency(dep.dependsOnTaskUuid) }) {
                                                            Icon(
                                                                imageVector = Icons.Default.Close,
                                                                contentDescription = "Retirer dépendance",
                                                                tint = Color(0xFFEF4444),
                                                                modifier = Modifier.size(20.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    if (canManageDeps) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Box {
                                            Text(
                                                text = "+ Dépendance",
                                                color = MaterialColorScheme.primary,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier
                                                    .clickable { showAddDependencyMenu = true }
                                                    .padding(8.dp)
                                            )

                                            val availableTasks = viewModel.organTasks.filter { ot ->
                                                !viewModel.dependencies.any { d -> d.dependsOnTaskUuid == ot.uuid }
                                            }

                                            DropdownMenu(
                                                expanded = showAddDependencyMenu,
                                                onDismissRequest = { showAddDependencyMenu = false }
                                            ) {
                                                if (availableTasks.isEmpty()) {
                                                    DropdownMenuItem(
                                                        text = { Text("Aucune tâche disponible") },
                                                        onClick = { showAddDependencyMenu = false }
                                                    )
                                                } else {
                                                    availableTasks.forEach { ot ->
                                                        DropdownMenuItem(
                                                            text = { Text(ot.title) },
                                                            onClick = {
                                                                viewModel.addDependency(ot.uuid)
                                                                showAddDependencyMenu = false
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

                    // 6. Attachments Section Card (Fichiers)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            val isOwner = viewModel.isTaskOwner()
                            val canAddAttachment = !isTrashed && viewModel.hasPerm("ATTACHMENT_ADD", isOwner)
                            val canDeleteAttachment = !isTrashed && viewModel.hasPerm("ATTACHMENT_DELETE", isOwner)

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showAttachmentsExpanded = !showAttachmentsExpanded }
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Fichiers joints",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Icon(
                                    painter = painterResource(R.drawable.flechedroite_logo),
                                    contentDescription = if (showAttachmentsExpanded) "Réduire" else "Développer",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier
                                        .rotate(if (showAttachmentsExpanded) 270f else 90f)
                                        .size(16.dp)
                                )
                            }

                            AnimatedVisibility(
                                visible = showAttachmentsExpanded,
                                enter = expandVertically() + fadeIn(),
                                exit = shrinkVertically() + fadeOut()
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    if (viewModel.attachments.isEmpty()) {
                                        Text("Aucun fichier joint", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                                    } else {
                                        viewModel.attachments.forEach { att ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = att.fileName,
                                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Text(
                                                        text = "${att.fileSize / 1024} Ko • Par ${att.uploadedBy.firstName}",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.outline
                                                    )
                                                }
                                                if (canDeleteAttachment) {
                                                    IconButton(onClick = { viewModel.deleteAttachment(att.uuid) }) {
                                                        Icon(
                                                            imageVector = Icons.Default.Close,
                                                            contentDescription = "Supprimer",
                                                            tint = Color(0xFFEF4444),
                                                            modifier = Modifier.size(20.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    if (canAddAttachment) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "+ Joindre",
                                            color = MaterialColorScheme.primary,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier
                                                .clickable { showAttachmentSourceDialog = true }
                                                .padding(8.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 7. Comments Section Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showCommentsExpanded = !showCommentsExpanded }
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Commentaires",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Icon(
                                    painter = painterResource(R.drawable.flechedroite_logo),
                                    contentDescription = if (showCommentsExpanded) "Réduire" else "Développer",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier
                                        .rotate(if (showCommentsExpanded) 270f else 90f)
                                        .size(16.dp)
                                )
                            }

                            AnimatedVisibility(
                                visible = showCommentsExpanded,
                                enter = expandVertically() + fadeIn(),
                                exit = shrinkVertically() + fadeOut()
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    val isOwner = viewModel.isTaskOwner()
                                    val canAddComment = !isTrashed && viewModel.hasPerm("COMMENT_CREATE", isOwner)

                                    if (viewModel.comments.isEmpty()) {
                                        Text("Aucun commentaire", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                                    } else {
                                        viewModel.comments.forEach { comment ->
                                            val currentUserUuid = fr.studio.voxel.organ.data.UserRepository.currentUser?.uuid
                                            val canDelete = !isTrashed && (
                                                    viewModel.hasPerm("COMMENT_DELETE_ALL") ||
                                                            (comment.user.uuid == currentUserUuid && viewModel.hasPerm("COMMENT_DELETE_OWN"))
                                                    )
                                            Row(
                                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.Top
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Text(
                                                            text = "${comment.user.firstName} ${comment.user.lastName}",
                                                            fontWeight = FontWeight.Bold,
                                                            style = MaterialTheme.typography.bodyMedium
                                                        )
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Text(
                                                            text = comment.createdAt.take(10),
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = MaterialTheme.colorScheme.outline
                                                        )
                                                    }
                                                    Text(
                                                        text = comment.content,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                }

                                                if (canDelete) {
                                                    IconButton(onClick = { viewModel.deleteComment(comment.uuid) }) {
                                                        Icon(
                                                            imageVector = Icons.Default.Close,
                                                            contentDescription = "Supprimer",
                                                            tint = Color(0xFFEF4444),
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    if (canAddComment) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            OutlinedTextField(
                                                value = newCommentText,
                                                onValueChange = { newCommentText = it },
                                                placeholder = { Text("Écrire un commentaire...") },
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier.weight(1f)
                                            )
                                            Button(
                                                onClick = {
                                                    if (newCommentText.isNotBlank()) {
                                                        viewModel.addComment(newCommentText)
                                                        newCommentText = ""
                                                    }
                                                },
                                                shape = RoundedCornerShape(10.dp)
                                            ) {
                                                Text("Publier")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 8. Historical Timeline Section Card
                    if (viewModel.timeline.isNotEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { showTimelineExpanded = !showTimelineExpanded }
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Historique d'activité",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Icon(
                                        painter = painterResource(R.drawable.flechedroite_logo),
                                        contentDescription = if (showTimelineExpanded) "Réduire" else "Développer",
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier
                                            .rotate(if (showTimelineExpanded) 270f else 90f)
                                            .size(16.dp)
                                    )
                                }

                                AnimatedVisibility(
                                    visible = showTimelineExpanded,
                                    enter = expandVertically() + fadeIn(),
                                    exit = shrinkVertically() + fadeOut()
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        viewModel.timeline.forEach { item ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                                verticalAlignment = Alignment.Top
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(8.dp)
                                                        .clip(CircleShape)
                                                        .background(
                                                            when (item.type) {
                                                                "COMMENT" -> Color(0xFF355EE4)
                                                                "ATTACHMENT" -> Color(0xFFEF4444)
                                                                else -> Color(0xFF94A3B8)
                                                            }
                                                        )
                                                        .align(Alignment.CenterVertically)
                                                )
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Column {
                                                    val author = item.userName ?: "Système"
                                                    val logText = when (item.type) {
                                                        "COMMENT" -> "$author a commenté : \"${item.detail}\""
                                                        "ATTACHMENT" -> "$author a joint le fichier : \"${item.detail}\""
                                                        else -> {
                                                            val field = when (item.fieldName) {
                                                                "status" -> "le statut"
                                                                "priority" -> "la priorité"
                                                                "title" -> "le titre"
                                                                "description" -> "la description"
                                                                "manager" -> "le responsable"
                                                                else -> item.fieldName ?: "la tâche"
                                                            }
                                                            "$author a modifié $field (${item.actionType})"
                                                        }
                                                    }
                                                    Text(
                                                        text = logText,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                    Text(
                                                        text = item.createdAt.take(16).replace("T", " "),
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.outline
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 9. Task Trash / Restoration Section Card (Corbeille de la tâche)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.toggleTrash() }
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Corbeille de la tâche",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Icon(
                                    painter = painterResource(R.drawable.flechedroite_logo),
                                    contentDescription = if (viewModel.isTrashOpen) "Réduire" else "Développer",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier
                                        .rotate(if (viewModel.isTrashOpen) 270f else 90f)
                                        .size(16.dp)
                                )
                            }

                            AnimatedVisibility(
                                visible = viewModel.isTrashOpen,
                                enter = expandVertically() + fadeIn(),
                                exit = shrinkVertically() + fadeOut()
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    // Trashed Comments
                                    Text("Commentaires supprimés :", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                                    if (viewModel.trashedComments.isEmpty()) {
                                        Text("Aucun", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                                    } else {
                                        viewModel.trashedComments.forEach { comment ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = comment.content,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                Row {
                                                    // Restore comment
                                                    IconButton(onClick = { viewModel.restoreComment(comment.uuid) }) {
                                                        Icon(
                                                            painter = painterResource(id = R.drawable.flechedroite_logo),
                                                            contentDescription = "Restaurer",
                                                            tint = Color(0xFF10B981),
                                                            modifier = Modifier.size(16.dp).rotate(180f)
                                                        )
                                                    }
                                                    // Hard delete comment
                                                    IconButton(onClick = { viewModel.deleteComment(comment.uuid, true) }) {
                                                        Icon(
                                                            imageVector = Icons.Default.Close,
                                                            contentDescription = "Effacer définitivement",
                                                            tint = Color(0xFFEF4444),
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Trashed Attachments
                                    Text("Fichiers joints supprimés :", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                                    if (viewModel.trashedAttachments.isEmpty()) {
                                        Text("Aucun", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                                    } else {
                                        viewModel.trashedAttachments.forEach { att ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = att.fileName,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                Row {
                                                    IconButton(onClick = { viewModel.restoreAttachment(att.uuid) }) {
                                                        Icon(
                                                            painter = painterResource(id = R.drawable.flechedroite_logo),
                                                            contentDescription = "Restaurer",
                                                            tint = Color(0xFF10B981),
                                                            modifier = Modifier.size(16.dp).rotate(180f)
                                                        )
                                                    }
                                                    IconButton(onClick = { viewModel.deleteAttachment(att.uuid, true) }) {
                                                        Icon(
                                                            imageVector = Icons.Default.Close,
                                                            contentDescription = "Effacer définitivement",
                                                            tint = Color(0xFFEF4444),
                                                            modifier = Modifier.size(16.dp)
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
                }

                // Sticky Footer
                if (!isTrashed) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                            .padding(24.dp)
                    ) {
                        PrimaryButton(
                            text = "Enregistrer les modifications",
                            onClick = { viewModel.saveTask() },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }

    // Delete confirmation dialog
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = {
                Text(
                    text = if (isDeletePermanent) "Supprimer définitivement" else "Supprimer la tâche",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Text(
                    text = if (isDeletePermanent)
                        "Cette action est irréversible. La tâche et toutes ses données associées seront définitivement perdues."
                    else
                        "La tâche sera déplacée vers la corbeille de l'Organ."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteTask(isDeletePermanent)
                        showDeleteConfirmDialog = false
                    }
                ) {
                    Text("Supprimer", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Annuler")
                }
            }
        )
    }

    // Attachment Source Dialog
    if (showAttachmentSourceDialog) {
        AlertDialog(
            onDismissRequest = { showAttachmentSourceDialog = false },
            title = {
                Text(
                    text = "Ajouter un fichier joint",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Choisissez la source de votre fichier :")
                    
                    Button(
                        onClick = {
                            showAttachmentSourceDialog = false
                            cameraPermissionLauncher.launch("android.permission.CAMERA")
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.PhotoCamera, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Prendre une photo")
                    }

                    Button(
                        onClick = {
                            showAttachmentSourceDialog = false
                            filePickerLauncher.launch("*/*")
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = null) // We can use Close or another icon
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Choisir un fichier")
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showAttachmentSourceDialog = false }) {
                    Text("Annuler")
                }
            }
        )
    }
}

// Helper functions for date selection and formatting
fun formatDateTimeForDisplay(dateTimeStr: String): String {
    if (dateTimeStr.isBlank()) return ""
    // Input format: YYYY-MM-DD HH:mm
    // Output format: DD/MM/YYYY HH:mm
    return try {
        val parts = dateTimeStr.split(" ")
        if (parts.size == 2) {
            val dateParts = parts[0].split("-")
            val timeParts = parts[1].split(":")
            if (dateParts.size == 3 && timeParts.size == 2) {
                "${dateParts[2]}/${dateParts[1]}/${dateParts[0]} ${timeParts[0]}:${timeParts[1]}"
            } else {
                dateTimeStr
            }
        } else {
            dateTimeStr
        }
    } catch (e: Exception) {
        dateTimeStr
    }
}

fun showDateTimePicker(
    context: android.content.Context,
    currentValue: String,
    onDateTimeSelected: (String) -> Unit
) {
    val calendar = Calendar.getInstance()
    if (currentValue.isNotBlank()) {
        try {
            val parts = currentValue.split(" ")
            if (parts.size == 2) {
                val dateParts = parts[0].split("-")
                val timeParts = parts[1].split(":")
                calendar.set(Calendar.YEAR, dateParts[0].toInt())
                calendar.set(Calendar.MONTH, dateParts[1].toInt() - 1)
                calendar.set(Calendar.DAY_OF_MONTH, dateParts[2].toInt())
                calendar.set(Calendar.HOUR_OF_DAY, timeParts[0].toInt())
                calendar.set(Calendar.MINUTE, timeParts[1].toInt())
            }
        } catch (e: Exception) {
            // fallback
        }
    }

    DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            TimePickerDialog(
                context,
                { _, hourOfDay, minute ->
                    val formattedMonth = String.format("%02d", month + 1)
                    val formattedDay = String.format("%02d", dayOfMonth)
                    val formattedHour = String.format("%02d", hourOfDay)
                    val formattedMinute = String.format("%02d", minute)
                    onDateTimeSelected("$year-$formattedMonth-$formattedDay $formattedHour:$formattedMinute")
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                true
            ).show()
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    ).show()
}

// FlowRow implementation for layouts
@Composable
fun FlowRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable () -> Unit
) {
    androidx.compose.foundation.layout.FlowRow(
        modifier = modifier,
        horizontalArrangement = horizontalArrangement,
        verticalArrangement = verticalArrangement
    ) {
        content()
    }
}
