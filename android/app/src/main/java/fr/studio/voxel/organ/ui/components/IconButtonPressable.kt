package fr.studio.voxel.organ.ui.components

import androidx.annotation.Nullable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

@Composable
fun IconButtonPressable(
    icon: Int, // R.drawable.xxx
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Gestion des interactions
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Animation de scale (effet enfoncé)
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.85f else 1f,
        label = "iconScale"
    )

    Image(
        painter = painterResource(id = icon),
        contentDescription = contentDescription,

        modifier = modifier
            .size(32.dp)
            .graphicsLayer(
                scaleX = scale,
                scaleY = scale
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null, // enlève ripple
                onClick = onClick
            )
    )
}
