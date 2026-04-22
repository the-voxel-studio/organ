package fr.studio.voxel.organ.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    //Permet de détecter les interactions (clic, pressé, etc.)
    val interactionSource = remember { MutableInteractionSource() }

    //  État : est-ce que le bouton est en train d’être pressé ?
    val isPressed by interactionSource.collectIsPressedAsState()

    //  Animation du scale (taille du bouton)
    // Quand on clique → 0.95 (réduction)
    // Sinon → taille normale (1f)
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        label = "scaleAnim"
    )

    //  Couleur dynamique du bouton
    val backgroundColor = if (isPressed) {
        MaterialTheme.colorScheme.secondary
    } else {
        MaterialTheme.colorScheme.primary
    }

    ElevatedButton(
        onClick = onClick,

        modifier = modifier
            //  Applique l'effet de zoom (scale)
            .graphicsLayer(
                scaleX = scale,
                scaleY = scale
            ),

        //  Forme arrondie du bouton
        shape = RoundedCornerShape(12.dp),

        //  Couleurs personnalisées
        colors = ButtonDefaults.elevatedButtonColors(
            containerColor = backgroundColor
        ),

        //  Important : on injecte notre interactionSource
        interactionSource = interactionSource,

        //  Gestion de l’ombre (effet enfoncé)
        elevation = ButtonDefaults.elevatedButtonElevation(
            defaultElevation = 6.dp,   // bouton normal
            pressedElevation = 2.dp    // bouton enfoncé
        )
    ) {
        Text(
            text = text,

            style = MaterialTheme.typography.labelLarge,

            color = MaterialTheme.colorScheme.onPrimary
        )
    }
}