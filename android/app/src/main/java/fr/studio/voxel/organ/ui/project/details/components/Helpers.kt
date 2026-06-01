package fr.studio.voxel.organ.ui.project.details.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp

fun formatDate(isoDate: String?): String {
    if (isoDate == null) return "-"
    return try {
        val clean = isoDate.substringBefore("T")
        val parts = clean.split("-")
        if (parts.size == 3) {
            "${parts[2]}/${parts[1]}/${parts[0]}"
        } else {
            clean
        }
    } catch (e: Exception) {
        "-"
    }
}

fun formatTime(isoDate: String?): String {
    if (isoDate == null) return ""
    return try {
        val timePart = isoDate.substringAfter("T").substringBefore(".")
        val parts = timePart.split(":")
        if (parts.size >= 2) {
            "${parts[0]}:${parts[1]}"
        } else {
            timePart
        }
    } catch (e: Exception) {
        ""
    }
}

fun getActivityText(type: String, actionType: String?, fieldName: String?): String {
    if (type == "COMMENT") {
        return "a ajouté un commentaire"
    } else if (type == "ATTACHMENT") {
        return "a ajouté une pièce jointe"
    } else if (type == "HISTORY") {
        return when (actionType) {
            "CREATE" -> "a créé la tâche"
            "UPDATE" -> {
                when (fieldName) {
                    "status" -> "a changé le statut"
                    "priority" -> "a changé la priorité"
                    else -> "a modifié ${translateField(fieldName)}"
                }
            }
            "STATUS_CHANGE" -> "a changé le statut"
            "PRIORITY_CHANGE" -> "a changé la priorité"
            "COMMENT_ADD", "COMMENT_CREATE" -> "a ajouté un commentaire"
            "ATTACHMENT_ADD" -> "a ajouté une pièce jointe"
            "LINK_ADD" -> "a ajouté un lien"
            "ASSIGNEE_ADD" -> "a assigné la tâche"
            "ASSIGNEE_REMOVE" -> "a retiré l'assignation"
            "RESTORE" -> "a restauré"
            else -> "a mis à jour la tâche"
        }
    }
    return "a modifié la tâche"
}

fun translateField(field: String?): String {
    if (field == null) return ""
    return when (field) {
        "title" -> "le titre"
        "description" -> "la description"
        "status", "status_message", "statusMessage" -> "le statut"
        "priority" -> "la priorité"
        "estimated_hours", "estimatedHours" -> "le temps estimé"
        "start_date", "startDate" -> "la date de début"
        "expires_at", "expiresAt" -> "l'échéance"
        else -> field
    }
}

@Composable
fun ClickableDescriptionText(
    text: String,
    highlightColor: Color,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val urlPattern = remember {
        java.util.regex.Pattern.compile(
            "(https?://[\\w\\d:#@%/;\$()~_?\\+-=\\\\.&]+)",
            java.util.regex.Pattern.CASE_INSENSITIVE
        )
    }

    val annotatedString = remember(text) {
        buildAnnotatedString {
            val matcher = urlPattern.matcher(text)
            var lastIdx = 0
            while (matcher.find()) {
                val start = matcher.start()
                val end = matcher.end()

                // Normal text
                append(text.substring(lastIdx, start))

                // Link text
                val linkUrl = text.substring(start, end)
                val linkStart = length
                append(linkUrl)
                addStyle(
                    style = SpanStyle(
                        color = highlightColor,
                        textDecoration = TextDecoration.Underline,
                        fontWeight = FontWeight.Bold
                    ),
                    start = linkStart,
                    end = length
                )
                addStringAnnotation(
                    tag = "URL",
                    annotation = linkUrl,
                    start = linkStart,
                    end = length
                )

                lastIdx = end
            }
            if (lastIdx < text.length) {
                append(text.substring(lastIdx))
            }
        }
    }

    ClickableText(
        text = annotatedString,
        style = MaterialTheme.typography.bodyMedium.copy(
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
            lineHeight = 22.sp
        ),
        modifier = modifier,
        onClick = { offset ->
            annotatedString.getStringAnnotations(tag = "URL", start = offset, end = offset)
                .firstOrNull()?.let { annotation ->
                    try {
                        val cleanUrl = if (!annotation.item.startsWith("http://") && !annotation.item.startsWith("https://")) {
                            "https://${annotation.item}"
                        } else {
                            annotation.item
                        }
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(cleanUrl))
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        // ignore
                    }
                }
        }
    )
}
