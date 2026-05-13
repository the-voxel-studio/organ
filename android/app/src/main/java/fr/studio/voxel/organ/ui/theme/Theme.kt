package fr.studio.voxel.organ.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

object AppColorScheme {
    val primary = PastelPink
    val onPrimary = Black
    val secondary = PastelBlue
    val onSecondary = Black
    val tertiary = LightBlue
    val background = White
    val onBackground = Black
    val surface = Beige
    val onSurface = Black
}

    secondary = PastelBlue,
    onSecondary = Black,

    tertiary = LightBlue,

    background = Beige,
    onBackground = Black,

    surface = Beige,
    onSurface = Black
)

@Composable
fun OrganTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = MaterialColorScheme,
        typography = MaterialTypography,
        content = content
    )
}
