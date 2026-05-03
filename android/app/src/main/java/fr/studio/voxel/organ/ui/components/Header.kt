package fr.studio.voxel.organ.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import fr.studio.voxel.organ.R
import fr.studio.voxel.organ.ui.theme.AppColorScheme
import fr.studio.voxel.organ.ui.theme.AppTypography

@Composable
fun Header(
    modifier: Modifier = Modifier,
    canOpenSidebar: Boolean = false,
    isSideBarClosed: Boolean = false,
    navigateUp: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 20.dp)
    ) {
        if (canOpenSidebar) {
            IconButton(
                onClick = navigateUp,
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Menu",
                    tint = AppColorScheme.onSurface
                )
            }
        }
        Row(
            modifier = Modifier.align(Alignment.Center),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            AnimatedLogo()

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "Organ",
                style = AppTypography.titleLarge,
                color = AppColorScheme.onSurface,
            )
        }

        Row(
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            if (isSideBarClosed) {
                Image(
                    painter = painterResource(R.drawable.poubelle_logo),
                    contentDescription = "poubelle",
                    modifier = Modifier
                        .size(24.dp)
                        .clickable {
                            /*TODO*/
                        }
                )
                Spacer(modifier = Modifier.width(24.dp))
                Image(
                    painter = painterResource(R.drawable.notification_logo),
                    contentDescription = "notification",
                    modifier = Modifier
                        .size(24.dp)
                        .clickable {
                            /*TODO*/
                        }
                )
            }
        }
    }
}
