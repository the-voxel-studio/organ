package fr.studio.voxel.organ.ui.parameter.details

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import fr.studio.voxel.organ.R
import fr.studio.voxel.organ.ui.components.LoadingOverlay
import fr.studio.voxel.organ.ui.header.Header
import fr.studio.voxel.organ.ui.parameter.details.components.*
import fr.studio.voxel.organ.viewmodel.ParameterViewModel

@Composable
fun Parameter(
    navController: NavHostController,
    parameterVM: ParameterViewModel = viewModel(),
    onLoggedOut: () -> Unit = {}
) {
    var revokeTargetUuid by remember { mutableStateOf<String?>(null) }
    var isDeleteAccountDialogOpen by remember { mutableStateOf(false) }

    val context = androidx.compose.ui.platform.LocalContext.current
    val user = parameterVM.currentUser
    val isGoogleAuth = user?.authWithGoogle == true

    LoadingOverlay(
        isLoading = parameterVM.isLoading,
        text = "Traitement en cours..."
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFFBFBFB))
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Header(
                navigateUp = { navController.navigate(fr.studio.voxel.organ.ui.OrganScreen.Sidebar.name) },
                canOpenSidebar = true
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 8.dp)
            ) {
                Text(
                    text = "Paramètres",
                    style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Black),
                    color = Color.Black
                )
                Text(
                    text = "Gérez vos informations personnelles et vos préférences de compte.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
                )

                // ------------------ PROFILE CARD ------------------
                SettingsCard(
                    iconRes = R.drawable.icon_account,
                    iconColor = MaterialTheme.colorScheme.primary,
                    iconBgColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    title = "Profil",
                    description = "Mettez à jour vos informations personnelles."
                ) {
                    parameterVM.profileSuccessMsg?.let { AlertSuccess(it) }
                    parameterVM.profileErrorMsg?.let { AlertError(it) }

                    SettingsField(
                        label = "Prénom",
                        value = parameterVM.firstName,
                        onValueChange = { parameterVM.firstName = it },
                        placeholder = "Jean"
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    SettingsField(
                        label = "Nom",
                        value = parameterVM.lastName,
                        onValueChange = { parameterVM.lastName = it },
                        placeholder = "Dupont"
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Disable editing email if connected to Google
                    SettingsField(
                        label = "Email",
                        value = parameterVM.email,
                        onValueChange = { parameterVM.email = it },
                        placeholder = "jean.dupont@example.com",
                        enabled = !isGoogleAuth
                    )
                    
                    if (isGoogleAuth) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "L'adresse e-mail d'un compte Google ne peut pas être modifiée.",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.outline
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = { parameterVM.updateProfile() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text(
                            text = "Enregistrer",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                // ------------------ SECURITY CARD ------------------
                SettingsCard(
                    iconRes = R.drawable.icon_settings,
                    iconColor = Color(0xFF355EE4), // Highlight blue
                    iconBgColor = Color(0xFF355EE4).copy(alpha = 0.1f),
                    title = "Sécurité",
                    description = if (isGoogleAuth) "Votre compte est géré par Google." else "Gérez votre mot de passe et connexions."
                ) {
                    parameterVM.googleSuccessMsg?.let { AlertSuccess(it) }
                    parameterVM.googleErrorMsg?.let { AlertError(it) }

                    // Google Connection row
                    if (isGoogleAuth) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFE8F5E9), RoundedCornerShape(16.dp))
                                .border(BorderStroke(1.dp, Color(0xFFC8E6C9)), RoundedCornerShape(16.dp))
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(Color.White, RoundedCornerShape(8.dp))
                                        .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "G",
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF4285F4),
                                        fontSize = 18.sp
                                    )
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(
                                        text = "Compte Google",
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = "Compte lié avec succès",
                                        color = Color(0xFF2E7D32),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFFE8F5E9), RoundedCornerShape(8.dp))
                                    .border(BorderStroke(1.dp, Color(0xFFC8E6C9)), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "LIÉ",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2E7D32)
                                )
                            }
                        }
                    } else {
                        // Display unlinked google row and password updates
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFF9F9F9), RoundedCornerShape(16.dp))
                                .border(BorderStroke(1.dp, Color(0xFFF0F0F0)), RoundedCornerShape(16.dp))
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(Color.White, RoundedCornerShape(8.dp))
                                        .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "G",
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Gray,
                                        fontSize = 18.sp
                                    )
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(
                                        text = "Compte Google",
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = "Non lié",
                                        color = Color.Gray,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                            Button(
                                onClick = { parameterVM.linkGoogle(context) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "Lier",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                        HorizontalDivider(color = Color(0xFFEEEEEE))
                        Spacer(modifier = Modifier.height(24.dp))

                        parameterVM.passwordSuccessMsg?.let { AlertSuccess(it) }
                        parameterVM.passwordErrorMsg?.let { AlertError(it) }

                        SettingsField(
                            label = "Mot de passe actuel",
                            value = parameterVM.currentPassword,
                            onValueChange = { parameterVM.currentPassword = it },
                            placeholder = "••••••••",
                            isPassword = true,
                            showPasswordToggle = parameterVM.showCurrentPassword,
                            onPasswordToggleClick = { parameterVM.showCurrentPassword = !parameterVM.showCurrentPassword }
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        SettingsField(
                            label = "Nouveau mot de passe",
                            value = parameterVM.newPassword,
                            onValueChange = { parameterVM.newPassword = it },
                            placeholder = "••••••••",
                            isPassword = true,
                            showPasswordToggle = parameterVM.showNewPassword,
                            onPasswordToggleClick = { parameterVM.showNewPassword = !parameterVM.showNewPassword }
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = { parameterVM.updatePassword() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Text(
                                text = "Mettre à jour le mot de passe",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                // ------------------ SESSIONS CARD ------------------
                SettingsCard(
                    iconRes = R.drawable.dashboard_logo,
                    iconColor = MaterialTheme.colorScheme.primary,
                    iconBgColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    title = "Sessions actives",
                    description = "Appareils actuellement connectés à votre compte."
                ) {
                    parameterVM.connectionsErrorMsg?.let { AlertError(it) }

                    if (parameterVM.connections.isEmpty()) {
                        Text(
                            text = "Aucune session active",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        parameterVM.connections.forEach { conn ->
                            SessionItem(
                                connection = conn,
                                onRevoke = { revokeTargetUuid = conn.uuid }
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                }

                // ------------------ DISCONNECT CARD (VERTICAL LAYOUT) ------------------
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                    ) {
                        Text(
                            text = "Session",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.Black
                        )
                        Text(
                            text = "Déconnectez-vous de l'appareil actuel.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                        )
                        Button(
                            onClick = {
                                parameterVM.logout()
                                onLoggedOut()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Text(
                                text = "Se déconnecter",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                // ------------------ DANGER ZONE ------------------
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                    border = BorderStroke(1.dp, Color(0xFFFEE2E2)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Start
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(Color(0xFFFEE2E2), RoundedCornerShape(16.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.poubelle_logo),
                                    contentDescription = null,
                                    tint = Color(0xFFDC2626),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = "Supprimer le compte",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFF991B1B)
                                )
                                Text(
                                    text = "Cette action est irréversible. Toutes vos données seront perdues.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFB91C1C).copy(alpha = 0.7f)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = { isDeleteAccountDialogOpen = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFDC2626),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Text(
                                text = "Supprimer mon compte Organ",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(48.dp))
            }
        }
    }

    // Revocation Confirmation Dialog
    ConfirmationDialog(
        isOpen = revokeTargetUuid != null,
        title = "Déconnecter l'appareil ?",
        message = "Cet appareil sera immédiatement déconnecté. Vous devrez vous reconnecter pour accéder de nouveau à votre compte sur ce terminal.",
        confirmLabel = "Déconnecter",
        cancelLabel = "Annuler",
        isDanger = true,
        onConfirm = {
            revokeTargetUuid?.let { uuid ->
                parameterVM.revokeSession(uuid)
            }
            revokeTargetUuid = null
        },
        onCancel = { revokeTargetUuid = null }
    )

    // Delete Account Confirmation Dialog
    ConfirmationDialog(
        isOpen = isDeleteAccountDialogOpen,
        title = "Désactiver le compte ?",
        message = "Votre compte sera immédiatement désactivé pour une période de 30 jours.\n\nPendant ce délai, vos données resteront conservées mais vous ne serez plus visible. À l'issue de ces 30 jours, votre compte et toutes ses données seront définitivement supprimés.",
        confirmLabel = "Confirmer la désactivation",
        cancelLabel = "Annuler",
        isDanger = true,
        onConfirm = {
            isDeleteAccountDialogOpen = false
            parameterVM.deleteAccount(onSuccess = onLoggedOut)
        },
        onCancel = { isDeleteAccountDialogOpen = false }
    )
}
