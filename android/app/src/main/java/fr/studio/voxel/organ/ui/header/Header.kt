package fr.studio.voxel.organ.ui.header

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import fr.studio.voxel.organ.R
import fr.studio.voxel.organ.ui.components.IconButtonPressable
import fr.studio.voxel.organ.ui.theme.AppColorScheme

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
            .padding(bottom = 20.dp)
    ) {
        if (canOpenSidebar) {
            IconButton(
                modifier = Modifier.align(Alignment.CenterStart),
                onClick = navigateUp
            ) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Menu",
                    tint = AppColorScheme.onSurface,
                    modifier = Modifier.size(40.dp)
                )
            }
        }
        Row(
            modifier = Modifier.align(Alignment.Center),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {

            AnimatedLogo(size = 48.dp)

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "Organ",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Row(
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            if (isSideBarClosed) {
                IconButtonPressable(
                    icon = R.drawable.poubelle_logo,
                    contentDescription = "poubelle",
                    modifier = Modifier
                        .size(24.dp),
                    onClick = { /*TODO*/ }
                )
                Spacer(modifier = Modifier.width(24.dp))
                IconButtonPressable(
                    icon = R.drawable.notification_logo,
                    contentDescription = "notification",
                    modifier = Modifier
                        .size(24.dp),
                    onClick = { /*TODO*/ }
                )
            }
        }
    }
}
