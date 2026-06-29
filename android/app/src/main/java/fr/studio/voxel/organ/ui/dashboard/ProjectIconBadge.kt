package fr.studio.voxel.organ.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale

/**
 * Structure permettant de définir le visuel d'un projet :
 * soit un Émoji (texte), soit une Icône SVG locale (ID de ressource).
 */
sealed class ProjectVisual {
    data class Emoji(val text: String) : ProjectVisual()
    data class SvgIcon(val resId: Int) : ProjectVisual()
    data class SvgXml(val xmlContent: String?) : ProjectVisual()
    data class Image(val urlOrData: String?) : ProjectVisual()
}

/**
 * Composant d'affichage de l'icône du projet avec son fond coloré.
 * @param visual Le type de visuel à afficher (Emoji ou SvgIcon)
 * @param projectColor La couleur de fond du badge
 * @param modifier Modificateurs optionnels pour la mise en page externe
 * @param badgeSize La taille globale du badge (fond)
 * @param iconSize La taille de l'icône ou de l'émoji à l'intérieur
 */
@Composable
fun ProjectIconBadge(
    visual: ProjectVisual?,
    projectColor : Color,
    modifier: Modifier = Modifier,
    badgeSize: Dp = 56.dp,
    iconSize: Dp = 32.dp
) {
    val context = LocalContext.current
    val imageLoader = remember {
        coil.ImageLoader.Builder(context)
            .components {
                add(coil.decode.SvgDecoder.Factory())
            }
            .build()
    }

    Box(
        modifier = modifier
            .size(badgeSize)
            .clip(RoundedCornerShape(16.dp))
            .background(if (visual!=null) MaterialTheme.colorScheme.background else projectColor)
            .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center
    ) {
        when (visual) {
            is ProjectVisual.Emoji -> {
                Text(
                    text = visual.text,
                    fontSize = (iconSize.value * 0.75).sp
                )
            }
            is ProjectVisual.SvgIcon -> {
                Icon(
                    painter = painterResource(id = visual.resId),
                    contentDescription = "Icône du projet",
                    modifier = Modifier.size(iconSize),
                    tint = Color.Unspecified
                )
            }
            is ProjectVisual.SvgXml -> {
                val modelBytes = remember(visual.xmlContent) {
                    visual.xmlContent?.toByteArray(Charsets.UTF_8)
                }
                AsyncImage(
                    model = modelBytes,
                    imageLoader = imageLoader,
                    contentDescription = "SVG du projet",
                    modifier = Modifier.size(iconSize)
                )
            }
            is ProjectVisual.Image -> {
                val model = remember(visual.urlOrData) {
                    val data = visual.urlOrData
                    if (data != null) {
                        if (data.startsWith("data:")) {
                            try {
                                val base64Content = data.substringAfter("base64,")
                                android.util.Base64.decode(base64Content, android.util.Base64.DEFAULT)
                            } catch (e: Exception) {
                                null
                            }
                        } else if (data.startsWith("/") || data.startsWith("uploads/")) {
                            val cleanPath = if (data.startsWith("/")) data else "/$data"
                            "http://10.0.2.2:8001$cleanPath"
                        } else if (data.startsWith("http")) {
                            data.replace("localhost:8000", "10.0.2.2:8001")
                                .replace("127.0.0.1:8000", "10.0.2.2:8001")
                        } else {
                            try {
                                android.util.Base64.decode(data, android.util.Base64.DEFAULT)
                            } catch (e: Exception) {
                                data
                            }
                        }
                    } else {
                        null
                    }
                }
                AsyncImage(
                    model = model,
                    imageLoader = imageLoader,
                    contentDescription = "Image du projet",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            null -> {
                // le badge est un carré de couleur
            }
        }
    }
}