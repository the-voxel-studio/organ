package fr.studio.voxel.organ.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.studio.voxel.organ.R
import fr.studio.voxel.organ.ui.dashboard.ProjectIconBadge
import fr.studio.voxel.organ.ui.dashboard.ProjectVisual

@Composable
fun TrashedItemCard(
    title: String,
    subtitle: String,
    iconRes: Int? = null,
    iconEmoji: String? = null,
    iconVisual: ProjectVisual? = null,
    iconTint: Color = Color.Gray,
    iconBg: Color = Color.LightGray.copy(alpha = 0.2f),
    showCheckbox: Boolean = false,
    isCheckboxChecked: Boolean = false,
    onCheckboxClick: ((Boolean) -> Unit)? = null,
    onItemClick: (() -> Unit)? = null,
    onRestore: () -> Unit,
    onDeletePermanent: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
        color = Color.White,
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onItemClick != null) Modifier.clickable { onItemClick() } else Modifier)

    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (showCheckbox) {
                CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
                    Checkbox(
                        checked = isCheckboxChecked,
                        onCheckedChange = onCheckboxClick,
                        colors = CheckboxDefaults.colors(
                            checkedColor = Color.Black,
                            checkmarkColor = Color.White
                        ),
                        modifier = Modifier
                            .size(24.dp)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(iconBg, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (iconVisual != null) {
                    ProjectIconBadge(
                        visual = iconVisual,
                        projectColor = Color.Transparent,
                        badgeSize = 40.dp,
                        iconSize = 24.dp
                    )
                } else if (iconEmoji != null) {
                    Text(iconEmoji, fontSize = 22.sp)
                } else if (iconRes != null) {
                    Icon(
                        painter = painterResource(id = iconRes),
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal
                    ),
                    color = Color.Gray,
                    lineHeight = 16.sp
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onRestore,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Black, contentColor = Color.White),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Text(
                        text = "RESTAURER",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Black
                        )
                    )
                }

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color(0xFFFFF1F2), RoundedCornerShape(10.dp))
                        .clickable { onDeletePermanent() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.poubelle_logo),
                        contentDescription = null,
                        tint = Color.Red,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
