package fr.studio.voxel.organ.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import fr.studio.voxel.organ.ui.theme.AppColorScheme
import fr.studio.voxel.organ.ui.theme.AppTypography

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        label = "scaleAnim"
    )

    val backgroundColor = if (isPressed) {
        AppColorScheme.secondary
    } else {
        AppColorScheme.primary
    }

    ElevatedButton(
        onClick = onClick,
        modifier = modifier
            .graphicsLayer(
                scaleX = scale,
                scaleY = scale
            ),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.elevatedButtonColors(
            containerColor = backgroundColor
        ),
        interactionSource = interactionSource,
        elevation = ButtonDefaults.elevatedButtonElevation(
            defaultElevation = 6.dp,
            pressedElevation = 2.dp
        )
    ) {
        Text(
            text = text,
            style = AppTypography.labelLarge,
            color = AppColorScheme.onPrimary
        )
    }
}
