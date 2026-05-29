package fr.studio.voxel.organ.ui.parameter

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import fr.studio.voxel.organ.ui.components.PrimaryButton

@Composable
fun Parameter(
    onDeleteButtonClicked :() -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp, horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        PrimaryButton(
            text = "Se déconnecter",
            onClick = onDeleteButtonClicked,
            modifier = Modifier,
            colorText  = MaterialTheme.colorScheme.surface,
            color  = Color.Red,
            pressedColor = Color.Red.copy(0.3f),
        )
    }
}