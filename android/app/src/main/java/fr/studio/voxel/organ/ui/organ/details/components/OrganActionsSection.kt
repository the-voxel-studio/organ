package fr.studio.voxel.organ.ui.organ.details.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun OrganActionsSection(
    highlightColor: Color,
    activeView: String,
    onViewChange: (String) -> Unit,
    showLinks: Boolean,
    onToggleLinks: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                .padding(3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(11.dp))
                    .background(if (activeView == "kanban") highlightColor else Color.Transparent)
                    .clickable { onViewChange("kanban") }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Kanban",
                    color = if (activeView == "kanban") Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 16.sp)
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(11.dp))
                    .background(if (activeView == "list") highlightColor else Color.Transparent)
                    .clickable { onViewChange("list") }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Liste",
                    color = if (activeView == "list") Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 16.sp)
                )
            }
        }

        IconButton(
            onClick = onToggleLinks,
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    if (showLinks) highlightColor.copy(alpha = 0.15f)
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
        ) {
            Icon(
                imageVector = Icons.Default.Link,
                contentDescription = "Liens",
                modifier = Modifier.size(20.dp),
                tint = if (showLinks) highlightColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}
