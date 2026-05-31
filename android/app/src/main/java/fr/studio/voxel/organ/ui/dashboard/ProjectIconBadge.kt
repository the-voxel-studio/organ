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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
    data class SvgXml(val xmlContent: String?) : ProjectVisual() // Nouveau cas
    data class Image(val urlOrData: String?) : ProjectVisual()
}

/**
 * Composant d'affichage de l'icône du projet avec son fond coloré.
 * @param visual Le type de visuel à afficher (Emoji ou SvgIcon)
 * @param backgroundColor La couleur de fond du badge
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
                    // Adapte la taille du texte à celle de l'icône
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
                AsyncImage(
                    model = visual.xmlContent?.toByteArray(),
                    contentDescription = "SVG du projet",
                    modifier = Modifier.size(iconSize)
                )
            }
            is ProjectVisual.Image -> {
                AsyncImage(
                    model = visual.urlOrData,
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