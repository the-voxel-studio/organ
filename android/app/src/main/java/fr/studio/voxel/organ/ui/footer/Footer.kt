package fr.studio.voxel.organ.ui.footer

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import fr.studio.voxel.organ.R
import fr.studio.voxel.organ.ui.components.IconButtonPressable

@Composable
fun Footer(
    modifier: Modifier = Modifier,
    utilisateur: String,
    onModifButtonClicked : () -> Unit = {}
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Row(
            modifier = modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ){
            /*IconButtonPressable(
                icon = R.drawable.icon_account,
                contentDescription = "Account",
                onClick = { /* TODO */ }
            )*/
            UserAvatar(
                name = utilisateur,
                size = 40.dp
            )

            Spacer(modifier = Modifier.width(24.dp))

            Text(
                text = utilisateur,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(24.dp))

            IconButtonPressable(
                icon = R.drawable.icon_settings,
                contentDescription = "Settings",
                onClick = onModifButtonClicked,
                modifier = Modifier.size(40.dp)
            )
        }
    }
}
