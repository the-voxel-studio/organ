package fr.studio.voxel.organ.ui.parameter.details.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import fr.studio.voxel.organ.R
import fr.studio.voxel.organ.network.services.UserConnection
import java.time.Duration
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@Composable
fun SessionItem(
    connection: UserConnection,
    onRevoke: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF9F9F9), RoundedCornerShape(16.dp))
            .border(BorderStroke(1.dp, Color(0xFFF0F0F0)), RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Color.White, RoundedCornerShape(12.dp))
                    .border(BorderStroke(1.dp, Color(0xFFECECEC)), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.dashboard_logo),
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${connection.deviceName} • ${connection.browserName}",
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (connection.isCurrent) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .background(Color(0xFFE8F5E9), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "ACTUELLE",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2E7D32)
                            )
                        }
                    }
                }
                Text(
                    text = "${connection.location} • ${connection.ipAddress}",
                    color = Color.Gray,
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "Dernière activité : ${formatRelativeDate(connection.lastUsedAt)}",
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.LightGray,
                    letterSpacing = 0.05.em,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
        IconButton(onClick = onRevoke) {
            Icon(
                painter = painterResource(id = R.drawable.poubelle_logo),
                contentDescription = "Régler la session",
                tint = Color.LightGray,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

fun formatRelativeDate(dateStr: String): String {
    return try {
        val formatter = DateTimeFormatter.ISO_DATE_TIME
        val dateTime = ZonedDateTime.parse(dateStr, formatter)
        val now = ZonedDateTime.now()
        val diffInSeconds = Duration.between(dateTime, now).seconds
        when {
            diffInSeconds < 60 -> "À l'instant"
            diffInSeconds < 3600 -> "Il y a ${diffInSeconds / 60} min"
            diffInSeconds < 86400 -> "Il y a ${diffInSeconds / 3600} h"
            diffInSeconds < 604800 -> "Il y a ${diffInSeconds / 86400} j"
            else -> dateTime.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
        }
    } catch (e: Exception) {
        dateStr.take(10)
    }
}
