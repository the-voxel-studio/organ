package fr.studio.voxel.organ.ui.task.components

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import fr.studio.voxel.organ.R
import fr.studio.voxel.organ.viewmodel.TaskDetailsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskBasicInfoCard(
    viewModel: TaskDetailsViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Card(
        modifier = modifier.fillMaxWidth(),
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
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = canEditStatus) { statusExpanded = true }
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
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = canEditPriority) { priorityExpanded = true }
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = canEditManager) { managerExpanded = true }
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
}
