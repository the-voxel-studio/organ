package fr.studio.voxel.organ.ui.task.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import fr.studio.voxel.organ.R
import fr.studio.voxel.organ.viewmodel.TaskDetailsViewModel
import fr.studio.voxel.organ.network.services.TaskTimelineItem

@Composable
fun TaskTimelineCard(
    viewModel: TaskDetailsViewModel,
    modifier: Modifier = Modifier
) {
    var showTimelineExpanded by remember { mutableStateOf(false) }

    if (viewModel.timeline.isNotEmpty()) {
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
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
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
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
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
                                    val actionText = formatTimelineDetail(item)
                                    val logText = "$author $actionText"
                                    Text(
                                        text = logText,
                                        style = MaterialTheme.typography.bodyLarge,
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
}

private fun getVal(obj: Any?, key: String): Any? {
    if (obj == null) return null
    if (obj is Map<*, *>) {
        if (obj.containsKey(key)) {
            return obj[key]
        }
        return obj
    }
    return obj
}

private fun formatUserValue(valObj: Any?): String {
    if (valObj == null) return "non assigné"
    if (valObj is Map<*, *>) {
        val firstName = valObj["firstName"] as? String ?: ""
        val lastName = valObj["lastName"] as? String ?: ""
        val fullName = "$firstName $lastName".trim()
        if (fullName.isNotEmpty()) return fullName
        val userName = valObj["userName"] as? String
        if (userName != null) return userName
        val name = valObj["name"] as? String
        if (name != null) return name
        val uuid = valObj["uuid"] as? String
        if (uuid != null) return "Utilisateur (UUID: ${uuid.take(8)})"
    }
    return valObj.toString()
}

private fun translateStatus(status: Any?): String {
    return when (status?.toString()?.uppercase()) {
        "TODO" -> "À faire"
        "IN_PROGRESS" -> "En cours"
        "PENDING" -> "En attente"
        "DONE" -> "Terminé"
        "CANCELLED" -> "Annulé"
        else -> status?.toString() ?: "À faire"
    }
}

private fun translatePriority(priority: Any?): String {
    val pStr = priority?.toString() ?: ""
    val pInt = pStr.toDoubleOrNull()?.toInt()
    if (pInt != null) {
        return when {
            pInt <= 3 -> "Basse"
            pInt <= 7 -> "Moyenne"
            else -> "Haute"
        }
    }
    return when (pStr.uppercase()) {
        "LOW" -> "Basse"
        "MEDIUM" -> "Moyenne"
        "HIGH" -> "Haute"
        "CRITICAL" -> "Critique"
        else -> pStr
    }
}

private fun formatAuditDate(dateVal: Any?): String {
    val dateStr = dateVal?.toString() ?: return "aucune"
    if (dateStr.isBlank()) return "aucune"
    return try {
        val clean = dateStr.substringBefore("T")
        val parts = clean.split("-")
        if (parts.size == 3) {
            "${parts[2]}/${parts[1]}/${parts[0]}"
        } else {
            clean
        }
    } catch (e: Exception) {
        dateStr
    }
}

private fun formatFieldLabel(field: String?): String {
    return when (field) {
        "title" -> "le titre"
        "description" -> "la description"
        "dueDate", "expiresAt", "expires_at" -> "l'échéance"
        "startDate", "start_date" -> "la date de début"
        "estimatedHours", "estimated_hours" -> "le temps estimé"
        "priority" -> "la priorité"
        "status" -> "le statut"
        "manager", "managerUuid", "manager_uuid" -> "le responsable"
        else -> "le champ \"$field\""
    }
}

private fun formatValue(valObj: Any?, fieldName: String?): String {
    if (valObj == null || valObj.toString().isEmpty()) {
        if (fieldName == "manager" || fieldName == "managerUuid" || fieldName == "manager_uuid") return "non assigné"
        return "non défini"
    }

    if (fieldName == "manager" || fieldName == "managerUuid" || fieldName == "manager_uuid") {
        return formatUserValue(valObj)
    }

    if (valObj is Map<*, *>) {
        val userName = valObj["userName"] as? String
        if (userName != null) return userName
        val fileName = valObj["fileName"] as? String
        if (fileName != null) return fileName
        val content = valObj["content"] as? String
        if (content != null) return content
        val title = valObj["title"] as? String
        if (title != null) return title
        return valObj.toString()
    }

    if (fieldName == "startDate" || fieldName == "start_date" || fieldName == "expiresAt" || fieldName == "expires_at" || fieldName == "dueDate" || fieldName == "deletedAt") {
        return formatAuditDate(valObj)
    }

    if (fieldName == "status") {
        return translateStatus(valObj)
    }

    if (fieldName == "priority") {
        return translatePriority(valObj)
    }

    return valObj.toString()
}

private fun formatTimelineDetail(item: TaskTimelineItem): String {
    val action = item.actionType
    val field = item.fieldName

    if (item.type == "COMMENT") {
        return "a ajouté un commentaire : \"${item.detail ?: ""}\""
    }

    if (item.type == "ATTACHMENT") {
        return "a ajouté la pièce jointe \"${item.detail ?: ""}\""
    }

    return when (action) {
        "CREATE" -> {
            val title = getVal(item.newValue, "title")?.toString() ?: ""
            val status = getVal(item.newValue, "status")
            "a créé la tâche \"$title\" (Statut : ${translateStatus(status)})"
        }
        "COMMENT_ADD" -> {
            val content = getVal(item.newValue, "content")?.toString() ?: ""
            "a ajouté un commentaire : \"$content\""
        }
        "ATTACHMENT_ADD" -> {
            val fileName = getVal(item.newValue, "fileName")?.toString() ?: ""
            "a ajouté la pièce jointe \"$fileName\""
        }
        "ASSIGNEE_ADD" -> {
            val userName = getVal(item.newValue, "userName")?.toString() ?: "un membre"
            "a assigné la tâche à $userName"
        }
        "ASSIGNEE_REMOVE" -> {
            val userName = getVal(item.oldValue, "userName")?.toString() ?: "un membre"
            "a retiré l'assignation de $userName"
        }
        "HARD_DELETE" -> {
            "a supprimé définitivement la tâche"
        }
        "STATUS_CHANGE" -> {
            val oldStatus = getVal(item.oldValue, "status")
            val newStatus = getVal(item.newValue, "status")
            "a changé le statut de \"${translateStatus(oldStatus)}\" à \"${translateStatus(newStatus)}\""
        }
        "UPDATE" -> {
            if (field == "deletedAt") {
                val deletedAtVal = getVal(item.newValue, "deletedAt")
                if (deletedAtVal != null && deletedAtVal.toString().isNotEmpty()) {
                    "a déplacé la tâche dans la corbeille"
                } else {
                    "a restauré la tâche de la corbeille"
                }
            } else {
                val rawOld = getVal(item.oldValue, field ?: "")
                val rawNew = getVal(item.newValue, field ?: "")
                val oldValStr = formatValue(rawOld, field)
                val newValStr = formatValue(rawNew, field)
                val fieldLabel = formatFieldLabel(field)
                "a modifié $fieldLabel : \"$oldValStr\" ➔ \"$newValStr\""
            }
        }
        else -> item.detail ?: "a modifié la tâche"
    }
}
