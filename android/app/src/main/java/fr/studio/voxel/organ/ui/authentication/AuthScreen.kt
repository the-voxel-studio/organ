package fr.studio.voxel.organ.ui.authentication

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import fr.studio.voxel.organ.R
import fr.studio.voxel.organ.ui.OrganScreen
import fr.studio.voxel.organ.viewmodel.AuthViewModel
// MainViewModel removed
import fr.studio.voxel.organ.ui.authentication.components.*
import fr.studio.voxel.organ.ui.header.Header
import fr.studio.voxel.organ.ui.components.PrimaryButton

enum class AuthMode{
    SIGN_IN, SIGN_UP
}

@Composable
fun AuthScreen(
    mode : AuthMode,
    navController: NavController,
    onModeSwitch : (AuthMode) -> Unit,
    viewModel: AuthViewModel = viewModel()
){
    val context = androidx.compose.ui.platform.LocalContext.current
    LaunchedEffect(viewModel.navigateToDashboard) {
        if (viewModel.navigateToDashboard) {
            viewModel.onNavigated()
        }
    }

    LaunchedEffect(viewModel.currentUser) {
        if (viewModel.currentUser != null) {
            navController.navigate(OrganScreen.Dashboard.name) {
                popUpTo(OrganScreen.SignIn.name) { inclusive = true }
            }
        }
    }

    LaunchedEffect(viewModel.registrationSuccess) {
        if(viewModel.registrationSuccess){
            onModeSwitch(AuthMode.SIGN_IN)
            viewModel.onRegistrationHandled()
        }
    }

    val hasError = viewModel.authError != null
    val showPasswordError = mode == AuthMode.SIGN_UP && viewModel.password.isNotEmpty() && !viewModel.isPasswordValid
    val showConfirmError = mode == AuthMode.SIGN_UP && viewModel.confirmPassword.isNotEmpty() && !viewModel.passwordsMatch

    val isAutoLoggingIn = viewModel.isCheckingSession

    if (isAutoLoggingIn) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
    } else {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            color = MaterialTheme.colorScheme.background
        ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp, top = 0.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Header(navigateUp = {})

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp,bottom = 8.dp, start = 16.dp, end = 16.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = if (mode == AuthMode.SIGN_UP) "Crée un compte" else "Bon retour !",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (mode == AuthMode.SIGN_UP) "Rejoignez l'aventure Organ dès aujourd'hui." else "Accédez à votre espace de travail.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.outline
                )

                Spacer(modifier = Modifier.height(24.dp))

                if (mode == AuthMode.SIGN_UP) {
                    SignUpForm(
                        viewModel = viewModel,
                        hasError = hasError,
                        showPasswordError = showPasswordError,
                        showConfirmError = showConfirmError,
                        enabled = !viewModel.isLoading
                    )
                } else {
                    SignInForm(
                        viewModel = viewModel,
                        hasError = hasError,
                        enabled = !viewModel.isLoading
                    )
                }

                // Message d'erreur global (ex: erreur de login ou d'inscription)
                if (hasError) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = viewModel.authError ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelMedium
                    )
                }

                Spacer(modifier = Modifier.height(40.dp))

                var checked by remember { mutableStateOf(false) }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = checked,
                        onCheckedChange = { checked = it },
                        enabled = !viewModel.isLoading,
                        modifier = Modifier.offset(x = (-6).dp).scale(1.4f)
                    )
                    val condition = buildAnnotatedString {
                        append("J'accepte les ".uppercase())
                        withStyle(
                            style = SpanStyle(
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        ) { append("Conditions d'utilisation ".uppercase()) }
                        append("et la ".uppercase())
                        withStyle(
                            style = SpanStyle(
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        ) { append("politique de confidentialité".uppercase()) }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = condition,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                PrimaryButton(
                    onClick = { viewModel.handleAuth(mode) },
                    enabled = checked && !viewModel.isLoading,
                    isLoading = viewModel.isLoading,
                    text = (if (mode == AuthMode.SIGN_UP) "Créer mon compte".uppercase() else "Se connecter").uppercase(),
                    modifier = Modifier.fillMaxWidth(),
                    colorText = MaterialTheme.colorScheme.surface
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HorizontalDivider(
                        modifier = Modifier.weight(1f),
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    )
                    Text(
                        text = if (mode == AuthMode.SIGN_UP) "OU S'INSCRIRE AVEC" else "OU CONTINUER AVEC",
                        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    HorizontalDivider(
                        modifier = Modifier.weight(1f),
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    )
                }
            }

            PrimaryButton(
                text = "Se connecter avec Google",
                onClick = { viewModel.loginWithGoogle(context) },
                enabled = !viewModel.isLoading,
                color = MaterialTheme.colorScheme.background,
                pressedColor = MaterialTheme.colorScheme.surface,
                colorText = MaterialTheme.colorScheme.onSurface,
                icon = R.drawable.logo_google,
                iconTint = Color.Unspecified,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            )

            Spacer(modifier = Modifier.height(32.dp))

            LoginRedirectText(
                onLoginClick = {
                    if (!viewModel.isLoading) {
                        val targetMode = if (mode == AuthMode.SIGN_UP) AuthMode.SIGN_IN else AuthMode.SIGN_UP
                        onModeSwitch(targetMode)
                    }
                },
                normalText = if (mode == AuthMode.SIGN_UP) "Déjà un compte ? " else "Nouveau sur l'application ? ",
                linkText = if (mode == AuthMode.SIGN_UP) "Se connecter" else "S'inscrire"
            )
        }
    }
}
}

