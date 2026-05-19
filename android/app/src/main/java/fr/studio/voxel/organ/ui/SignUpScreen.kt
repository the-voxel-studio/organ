package fr.studio.voxel.organ.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import fr.studio.voxel.organ.R
import fr.studio.voxel.organ.ViewModel.SignUpViewModel
import fr.studio.voxel.organ.ui.components.HeaderComponents.Header
import fr.studio.voxel.organ.ui.components.PrimaryButton
import fr.studio.voxel.organ.ui.components.SignUpComponents.LoginRedirectText

@Composable
fun SignUp(
    viewModel: SignUpViewModel = viewModel()
){
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        color = MaterialTheme.colorScheme.background
    ){
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Header(navigateUp = {})

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp, horizontal = 16.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text= "Crée un compte",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 36.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text= "Rejoignez l'aventure Organ dès aujourd'hui",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.outline
                )

                Spacer(modifier = Modifier.height(24.dp))

                val prenom = "Prénom"

                Text(
                    text = prenom.uppercase(),
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface.copy(0.8f)
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = viewModel.name, // Lit la valeur du ViewModel
                    onValueChange = { viewModel.updateName(it) }, // Envoie la valeur au ViewModel
                    placeholder = { Text("Jean") },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 64.dp)
                        .border(width = 3.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp)),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedContainerColor = MaterialTheme.colorScheme.outline.copy(0.1f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.outline.copy(0.1f)
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "NOM",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface.copy(0.8f)
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = viewModel.surname,
                    onValueChange = { viewModel.updateSurname(it) },
                    placeholder = { Text("Dupont") },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 64.dp)
                        .border(width = 3.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp)),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedContainerColor = MaterialTheme.colorScheme.outline.copy(0.1f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.outline.copy(0.1f)
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "ADRESSE E-MAIL",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface.copy(0.8f)
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = viewModel.mail,
                    onValueChange = { viewModel.updateMail(it) },
                    placeholder = { Text("nom@exemple.com") },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 64.dp)
                        .border(width = 3.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp)),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedContainerColor = MaterialTheme.colorScheme.outline.copy(0.1f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.outline.copy(0.1f)
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "MOT DE PASSE",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface.copy(0.8f)
                )

                Spacer(modifier = Modifier.height(8.dp))

                val showPasswordError = viewModel.password.isNotEmpty() && !viewModel.isPasswordValid

                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = viewModel.password,
                        visualTransformation = PasswordVisualTransformation(),
                        onValueChange = { viewModel.updatePassword(it) },
                        isError = showPasswordError,
                        placeholder = { Text("•••••••••") },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                            .border(
                                width = 3.dp,
                                color = if (showPasswordError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline,
                                shape = RoundedCornerShape(8.dp)
                            ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.outline.copy(0.1f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.outline.copy(0.1f),
                            errorContainerColor = MaterialTheme.colorScheme.outline.copy(0.1f),

                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            errorBorderColor = Color.Transparent
                        )
                    )

                    if (showPasswordError) {
                        Text(
                            text = "Taille minimale de 9 caractères",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "CONFIRMER LE MOT DE PASSE",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface.copy(0.8f)
                )

                Spacer(modifier = Modifier.height(8.dp))

                val isError = viewModel.confirmPassword.isNotEmpty() && !viewModel.passwordsMatch

                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = viewModel.confirmPassword,
                        visualTransformation = PasswordVisualTransformation(),
                        onValueChange = { viewModel.updateConfirmPassword(it) },
                        isError = isError,
                        placeholder = { Text("•••••••••") },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                            .border(
                                width = 3.dp,
                                color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline,
                                shape = RoundedCornerShape(8.dp)
                            ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.outline.copy(0.1f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.outline.copy(0.1f),
                            errorContainerColor = MaterialTheme.colorScheme.outline.copy(0.1f),

                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            errorBorderColor = Color.Transparent
                        )
                    )

                    if (isError) {
                        Text(
                            text = "Les mots de passe ne correspondent pas",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))

                var checked by remember { mutableStateOf(false) }

                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ){
                    Checkbox(checked = checked,
                        onCheckedChange = {newValue -> checked = newValue },
                        modifier = Modifier
                            .offset(x = (-6).dp)
                            .scale(1.4f)
                    )

                    val condition = buildAnnotatedString{
                        append("J'accepte les ".uppercase())

                        withStyle(style = SpanStyle(
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        ) {
                            append("Conditions d'utilisation ".uppercase())
                        }

                        append("et la ".uppercase())

                        withStyle(style = SpanStyle(
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        ) {
                            append("politique de confidentialité".uppercase())
                        }
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

                Spacer(modifier = Modifier.height(40.dp))

                val createAccount = "Créer mon compte"

                PrimaryButton(
                    onClick = { viewModel.register() },
                    text = createAccount.uppercase(),
                    modifier = Modifier
                        .fillMaxWidth(),
                    colorText = MaterialTheme.colorScheme.surface
                )

                Spacer(modifier = Modifier.height(40.dp))

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
                        text = "OU S'INSCRIRE AVEC",
                        style = MaterialTheme.typography.bodyLarge,
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
                onClick = {/*TODO*/},
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
                    // Mets ici ta logique pour ouvrir l'écran de connexion !
                    // Exemple : navController.navigate("login_screen")
                },
                normalText = "Déjà un compte ? ",
                linkText = "Se connecter"
            )
        }
    }
}