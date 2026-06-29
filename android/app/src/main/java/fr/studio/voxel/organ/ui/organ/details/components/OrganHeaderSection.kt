package fr.studio.voxel.organ.ui.organ.details.components

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import fr.studio.voxel.organ.network.services.Organ
import fr.studio.voxel.organ.ui.dashboard.ProjectIconBadge
import fr.studio.voxel.organ.ui.dashboard.ProjectVisual

@Composable
fun OrganDetailIconBadge(
    iconType: String?,
    iconData: String?,
    highlightColor: Color,
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
        projectColor = highlightColor,
        modifier = modifier,
        badgeSize = size,
        iconSize = size * 0.6f
    )
}

@Composable
fun OrganHeaderSection(
    organ: Organ,
    projectTitle: String,
    projectColor: Color,
    highlightColor: Color,
    canManage: Boolean,
    canViewTrash: Boolean,
    onEditClick: () -> Unit,
    onTrashClick: () -> Unit
) {
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
                OrganDetailIconBadge(
                    iconType = organ.iconType,
                    iconData = organ.iconData,
                    highlightColor = highlightColor,
                    size = 64.dp
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = organ.title,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(projectColor.copy(alpha = 0.12f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = projectTitle,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = projectColor
                            )
                        }
                    }
                }
            }

            organ.description?.let { desc ->
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(12.dp))

                // Linkified Description Text
                ClickableDescriptionText(
                    text = desc,
                    highlightColor = highlightColor,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (canManage || canViewTrash) {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (canViewTrash) {
                        OutlinedButton(
                            onClick = onTrashClick,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.height(40.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Corbeille",
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        if (canManage) {
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                    }

                    if (canManage) {
                        Button(
                            onClick = onEditClick,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.onSurface,
                                contentColor = MaterialTheme.colorScheme.surface
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.height(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Configuration",
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Configuration", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}
