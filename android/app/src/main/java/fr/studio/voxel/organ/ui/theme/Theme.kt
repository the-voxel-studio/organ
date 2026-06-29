package fr.studio.voxel.organ.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

object AppColorScheme {
    val primary = PastelPink
    val primaryContainer = LightPink
    val onPrimary = Black
    val secondary = DarkBlue
    val secondaryContainer = LightBlue
    val onSecondary = Black
    val tertiary = LightBlue
    val background = Beige
    val onBackground = Black
    val surface = White
    val onSurface = Black
}

val MaterialColorScheme = lightColorScheme(
    primary = AppColorScheme.primary,
    primaryContainer = AppColorScheme.primaryContainer,
    onPrimary = AppColorScheme.onPrimary,
    secondary = AppColorScheme.secondary,
    secondaryContainer = AppColorScheme.secondaryContainer,
    onSecondary = AppColorScheme.onSecondary,
    tertiary = AppColorScheme.tertiary,
    background = AppColorScheme.background,
    onBackground = AppColorScheme.onBackground,
    surface = AppColorScheme.surface,
    onSurface = AppColorScheme.onSurface
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