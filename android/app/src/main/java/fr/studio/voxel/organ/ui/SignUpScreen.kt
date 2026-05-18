package fr.studio.voxel.organ.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import fr.studio.voxel.organ.ViewModel.SignUpViewModel
import fr.studio.voxel.organ.ui.components.HeaderComponents.Header
import fr.studio.voxel.organ.ui.components.PrimaryButton

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

                Spacer(modifier = Modifier.height(24.dp))



                val createAccount = "Créer mon compte"

                PrimaryButton(
                    onClick = { viewModel.register() },
                    text = createAccount.uppercase(),
                    modifier = Modifier
                        .fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface
                )
            }
        }
    }
}