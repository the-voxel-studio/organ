package fr.studio.voxel.organ.ui.notification

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import fr.studio.voxel.organ.ui.OrganScreen
import fr.studio.voxel.organ.ui.header.Header
import fr.studio.voxel.organ.viewmodel.NotificationViewModel

@Composable
fun NotificationScreen(
    navController: NavHostController,
    viewModel: NotificationViewModel = viewModel()
) {
    val notifications = viewModel.notifications
    val unreadCount = viewModel.unreadCount
    val isLoading = viewModel.isLoading
    val error = viewModel.error

    LaunchedEffect(Unit) {
        viewModel.loadNotifications()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFBFBFB))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Header(
                navigateUp = { navController.popBackStack() },
                canOpenSidebar = true
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Notifications",
                        style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Black),
                        color = Color.Black
                    )
                    if (unreadCount > 0) {
                        Text(
                            text = "$unreadCount non lue(s)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFF27B9B), // Bubblegum/pink
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (notifications.any { !it.isRead && !it.isRealInvite }) {
                    TextButton(
                        onClick = { viewModel.markAllAsRead() }
                    ) {
                        Text(
                            text = "TOUT MARQUER COMME LU",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            ),
                            color = Color(0xFFF27B9B)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (error != null) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            if (notifications.isEmpty() && !isLoading) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(bottom = 64.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = null,
                        tint = Color.LightGray,
                        modifier = Modifier.size(80.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "AUCUNE NOTIFICATION",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        ),
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    notifications.forEach { notification ->
                        NotificationCard(
                            notification = notification,
                            onMarkAsRead = { viewModel.markAsRead(notification.uuid) },
                            onDelete = { viewModel.deleteNotification(notification.uuid) },
                            onAcceptInvite = {
                                viewModel.acceptInvitation(notification.uuid) { projectUuid ->
                                    navController.navigate("${OrganScreen.Project.name}/$projectUuid") {
                                        // Pop the notification screen so back stack is clean
                                        popUpTo(OrganScreen.Notification.name) { inclusive = true }
                                    }
                                }
                            },
                            onRefuseInvite = { viewModel.refuseInvitation(notification.uuid) }
                        )
                    }
                }
            }
        }

        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = Color(0xFFF27B9B)
            )
        }
    }
}

@Composable
fun NotificationCard(
    notification: fr.studio.voxel.organ.network.services.NotificationResponse,
    onMarkAsRead: () -> Unit,
    onDelete: () -> Unit,
    onAcceptInvite: () -> Unit,
    onRefuseInvite: () -> Unit
) {
    val isUnread = !notification.isRead
    val isInvite = notification.isRealInvite

    val cardBg = if (isUnread) {
        Color(0xFFFFF5F7) // Very light pink to match Angular text-black / bg-gradient from pink-50/20
    } else {
        Color.White
    }

    val borderColor = if (isUnread) {
        Color(0xFFF27B9B).copy(alpha = 0.2f) // Light bubblegum border
    } else {
        Color(0xFFEEEEEE)
    }

    Surface(
        color = cardBg,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = isUnread && !isInvite) { onMarkAsRead() }
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isUnread) {
                        Box(
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .size(8.dp)
                                .background(Color(0xFFF27B9B), CircleShape)
                        )
                    }

                    Text(
                        text = notification.message,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = if (isUnread) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 13.sp
                        ),
                        color = if (isUnread) Color.Black else Color.Gray,
                        modifier = Modifier.weight(1f)
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Supprimer la notification",
                        tint = Color.LightGray,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            if (isInvite) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = onAcceptInvite,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF27B9B)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        Text(
                            text = "Accepter",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                    }

                    OutlinedButton(
                        onClick = onRefuseInvite,
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Black),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        Text(
                            text = "Refuser",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }
    }
}
