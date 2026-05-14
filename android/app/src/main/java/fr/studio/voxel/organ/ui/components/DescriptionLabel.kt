package fr.studio.voxel.organ.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.studio.voxel.organ.ui.theme.MaterialColorScheme

@Composable
fun DescriptionLabel(
    content: String,
    date : String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialColorScheme.surface,
        border = BorderStroke(1.dp, MaterialColorScheme.onSurface),
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = modifier.padding(8.dp)
        ) {

            Text(
                text = content,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Normal
                ),
                color = Color.DarkGray
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 50.dp),
                verticalAlignment = Alignment.CenterVertically,
                //horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val createVal = "Projet créé le "
                Text(
                    text = createVal.uppercase(),
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 18.sp
                    ),
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text = date,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )

            }
        }
    }
}

/*Exemple d'appel
    SimpleBlock("Description", "blablabla oui il faut faire ça ça et puis ça")
 */
