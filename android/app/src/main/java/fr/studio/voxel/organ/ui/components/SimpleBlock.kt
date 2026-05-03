package fr.studio.voxel.organ.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import fr.studio.voxel.organ.ui.theme.AppColorScheme
import fr.studio.voxel.organ.ui.theme.AppTypography

@Composable
fun SimpleBlock(
    title: String,
    content: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth() //Pour que le texte retourne automatiquement à la ligne si trop long
            .border(1.dp, AppColorScheme.onSurface)
            .background(AppColorScheme.surface)
            .padding(12.dp)
    ) {
        Text(
            text = title,
            textDecoration = TextDecoration.Underline,
            modifier = Modifier.align(Alignment.Start),
            style = AppTypography.labelLarge,
            color = AppColorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = content,
            style = AppTypography.labelSmall,
            color = AppColorScheme.onSurface
        )
    }
}

/*Exemple d'appel
    SimpleBlock("Description", "blablabla oui il faut faire ça ça et puis ça")
 */
