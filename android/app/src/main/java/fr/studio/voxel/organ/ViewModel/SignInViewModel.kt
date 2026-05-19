package fr.studio.voxel.organ.ViewModel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SignInViewModel : ViewModel() {
    var identifierInput by mutableStateOf("")
        private set

    var passwordInput by mutableStateOf("")
        private set

    var loginError by mutableStateOf<String?>(null)
        private set

    var isLoading by mutableStateOf(false)
        private set

    fun updateIdentifier(newValue: String) {
        identifierInput = newValue
        loginError = null //efface l'erreur quand l'utilisateur corrige
    }

    fun updatePassword(newValue: String) {
        passwordInput = newValue
        loginError = null
    }

    fun performLogin() {
        if (identifierInput.isBlank() || passwordInput.isBlank()) {
            loginError = "Veuillez remplir tous les champs."
            return
        }

        viewModelScope.launch {
            isLoading = true
            loginError = null

            // On envoie l'identifiant ET le mot de passe ensemble à ta bdd/API
            val isSuccess = checkUserCredentials(identifierInput, passwordInput)

            if (isSuccess) {
                // Connecté ! Tu peux déclencher ta navigation vers l'accueil ici
            } else {
                // Message d'erreur flou volontaire pour la sécurité
                loginError = "Identifiant ou mot de passe incorrect."
            }
            isLoading = false
        }
    }

    private suspend fun checkUserCredentials(identifier: String, pass: String): Boolean {
        kotlinx.coroutines.delay(500) // Simulation de l'appel réseau/BDD
        // Ici tu mettras ta vraie logique (ex: return authRepository.signIn(identifier, pass))
        return identifier == "test@gmail.com" && pass == "123456"
    }
}