package fr.studio.voxel.organ.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ProjectStateSticker(
    modifier: Modifier = Modifier,
    state: String?,
    color: Color
){
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(0.1f))
            .border(1.dp, color,RoundedCornerShape(8.dp))
            .padding(8.dp),
    ) {
        if (state != null) {
            Text(
                text = state,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = color
            )
        }
    }
}