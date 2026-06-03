package fr.studio.voxel.organ.ui.project.details.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import fr.studio.voxel.organ.R
import fr.studio.voxel.organ.network.services.ProjectDetailedViewResponse
import fr.studio.voxel.organ.ui.dashboard.ProjectIconBadge
import fr.studio.voxel.organ.ui.dashboard.ProjectVisual

@Composable
fun ProjectDetailIconBadge(
    iconType: String?,
    iconData: String?,
    projectColor: Color,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp
) {
    val visual = remember(iconType, iconData) {
        when (iconType) {
            "EMOJI" -> iconData?.let { ProjectVisual.Emoji(it) }
            "SVG" -> iconData?.let { ProjectVisual.SvgXml(it) }
            "IMAGE", "BLOB" -> iconData?.let { ProjectVisual.Image(it) }
            else -> null
        }
    }
    ProjectIconBadge(
        visual = visual,
        projectColor = projectColor,
        modifier = modifier,
        badgeSize = size,
        iconSize = size * 0.6f
    )
}

@Composable
fun ProjectHeaderSection(
    data: ProjectDetailedViewResponse,
    projectColor: Color,
    canManage: Boolean,
    onEditClick: () -> Unit,
    onTrashClick: () -> Unit,
    onFeaturePlaceholderClick: (String) -> Unit
) {
    val project = data.project

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                ProjectDetailIconBadge(
                    iconType = project.iconType,
                    iconData = project.iconData,
                    projectColor = projectColor,
                    size = 64.dp
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = project.title,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val statusLabel = when (project.status) {
                            "ACTIVE" -> "Actif"
                            "ARCHIVED" -> "Archivé"
                            else -> "Inactif"
                        }
                        val statusBg = when (project.status) {
                            "ACTIVE" -> projectColor.copy(alpha = 0.15f)
                            "ARCHIVED" -> MaterialTheme.colorScheme.surfaceVariant
                            else -> Color(0xFFFBBF24).copy(alpha = 0.15f)
                        }
                        val statusColor = when (project.status) {
                            "ACTIVE" -> projectColor
                            "ARCHIVED" -> MaterialTheme.colorScheme.onSurfaceVariant
                            else -> Color(0xFFD97706)
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(statusBg)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = statusLabel,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = statusColor
                            )
                        }

                        Text(
                            text = "Rôle: ${project.role}",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            data.admin?.let { admin ->
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Administrateur: ${admin.firstName} ${admin.lastName}",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Text(
                    text = admin.email,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }

            // Quick actions line
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (canManage) {
                    OutlinedButton(
                        onClick = onTrashClick,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Corbeille",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                if (canManage) {
                    OutlinedButton(
                        onClick = { onFeaturePlaceholderClick("Statistiques") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = projectColor
                        ),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.outline_info),
                            contentDescription = "Statistiques",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                if (project.role == "ADMIN") {
                    Button(
                        onClick = onEditClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.onSurface,
                            contentColor = MaterialTheme.colorScheme.surface
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Paramètres",
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.surface
                        )
                    }
                }
            }
        }
    }
}
