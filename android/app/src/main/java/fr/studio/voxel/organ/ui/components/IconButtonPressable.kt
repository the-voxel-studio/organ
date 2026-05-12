package fr.studio.voxel.organ.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

@Composable
fun IconButtonPressable(
    icon: Int, // R.drawable.xxx
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
) {
    // Gestion des interactions
    val isPressed by interactionSource.collectIsPressedAsState()

    // Animation de scale (effet enfoncé)
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.85f else 1f,
        label = "iconScale"
    )

    Icon(
        painter = painterResource(id = icon),
        contentDescription = contentDescription,
        tint = tint,

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
