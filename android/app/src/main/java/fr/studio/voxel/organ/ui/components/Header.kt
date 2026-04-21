package fr.studio.voxel.organ.ui.components

import android.os.Build.VERSION.SDK_INT
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import fr.studio.voxel.organ.R

@Composable
fun Header() {
    val context = LocalContext.current

    // Configuration de l'ImageLoader pour supporter le format GIF
    val imageLoader = remember {
        ImageLoader.Builder(context)
            .components {
                if (SDK_INT >= 28) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .build()
    }

    Row {
        AsyncImage(
            model = R.drawable.logo_gif,
            contentDescription = "Logo animé",
            imageLoader = imageLoader,
            modifier = Modifier.size(100.dp) // Ajustez la taille selon vos besoins
        )
    }
}
