package fr.studio.voxel.organ.ui.project.details.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.studio.voxel.organ.R
import fr.studio.voxel.organ.network.services.ProjectDetailedActivity

@Composable
fun ActivityFeedSection(
    activities: List<ProjectDetailedActivity>,
    projectColor: Color
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF4ADE80).copy(alpha = 0.1f))
                    .padding(8.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.outline_info),
                    contentDescription = null,
                    tint = Color(0xFF22C55E),
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "Dernières activités",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            if (activities.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Aucune activité récente sur les tâches.",
                        style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)),
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                Column(modifier = Modifier.fillMaxWidth()) {
                    activities.forEachIndexed { index, activity ->
                        ActivityItem(
                            activity = activity,
                            projectColor = projectColor
                        )
                        if (index < activities.size - 1) {
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ActivityItem(
    activity: ProjectDetailedActivity,
    projectColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.Top
    ) {
        // User initials
        val initials = remember(activity.userFirstName, activity.userLastName) {
            val f = activity.userFirstName?.firstOrNull()?.uppercase() ?: ""
            val l = activity.userLastName?.firstOrNull()?.uppercase() ?: ""
            if (f.isNotEmpty() || l.isNotEmpty()) "$f$l" else "U"
        }

        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initials,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Black),
                color = projectColor
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            val userFullName = "${activity.userFirstName ?: ""} ${activity.userLastName ?: ""}".trim()
            val text = getActivityText(activity.type, activity.actionType, activity.fieldName)
            val translatedField = translateField(activity.fieldName).lowercase()

            Text(
                text = androidx.compose.ui.text.buildAnnotatedString {
                    append(userFullName)
                    addStyle(
                        style = androidx.compose.ui.text.SpanStyle(fontWeight = FontWeight.Bold),
                        start = 0,
                        end = userFullName.length
                    )
                    append(" $text")
                    if (activity.fieldName != null && activity.type == "HISTORY" && activity.actionType == "UPDATE") {
                        append(" $translatedField")
                    }
                    if (activity.taskTitle != null) {
                        append(" sur ")
                        val start = length
                        append(activity.taskTitle)
                        addStyle(
                            style = androidx.compose.ui.text.SpanStyle(fontWeight = FontWeight.Bold),
                            start = start,
                            end = length
                        )
                    }
                },
                style = MaterialTheme.typography.bodySmall.copy(lineHeight = 16.sp),
                color = MaterialTheme.colorScheme.onSurface
            )

            if (activity.organTitle != null) {
                Text(
                    text = "dans ${activity.organTitle}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = formatTime(activity.createdAt),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )

                val typeLabel = when (activity.type) {
                    "COMMENT" -> "Commentaire"
                    "ATTACHMENT" -> "Fichier"
                    else -> "Activité"
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(projectColor.copy(alpha = 0.1f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = typeLabel,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = projectColor,
                        fontSize = 9.sp
                    )
                }
            }
        }
    }
}
