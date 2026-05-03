package fr.studio.voxel.organ.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import fr.studio.voxel.organ.R
import fr.studio.voxel.organ.ui.theme.AppColorScheme
import fr.studio.voxel.organ.ui.theme.AppTypography

@Composable
fun Footer(
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButtonPressable(
                icon = R.drawable.icon_account,
                contentDescription = "Account",
                onClick = { /* TODO */ }
            )

            Spacer(modifier = Modifier.width(24.dp))

            Text(
                text = "Nom Prénom",
                style = AppTypography.labelLarge,
                color = AppColorScheme.onSurface,
            )

            Spacer(modifier = Modifier.width(24.dp))

            IconButtonPressable(
                icon = R.drawable.icon_settings,
                contentDescription = "Settings",
                onClick = { /* TODO */ },
                modifier = Modifier.size(32.dp)
            )
        }
    }
}
