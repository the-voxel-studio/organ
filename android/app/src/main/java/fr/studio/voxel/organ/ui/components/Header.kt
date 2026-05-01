package fr.studio.voxel.organ.ui.components

import android.os.Build.VERSION.SDK_INT
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import fr.studio.voxel.organ.R
import kotlin.Boolean

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
            Icon(Icons.Default.Menu, contentDescription = "Menu")
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
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
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
                            {/*TODO*/}
                        }
                )

                Spacer(modifier = Modifier.width(24.dp))

                Image(
                    painter = painterResource(R.drawable.notification_logo),
                    contentDescription = "notification",
                    modifier = Modifier
                        .size(24.dp)
                        .clickable {
                            {/*TODO*/}
                        }
                )
            }
        }
    }
}