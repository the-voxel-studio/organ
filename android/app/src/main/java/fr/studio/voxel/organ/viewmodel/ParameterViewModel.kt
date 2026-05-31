package fr.studio.voxel.organ.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.studio.voxel.organ.data.ProjectRepository
import fr.studio.voxel.organ.data.UserRepository
import fr.studio.voxel.organ.network.services.User
import fr.studio.voxel.organ.network.services.UserConnection
import kotlinx.coroutines.launch

class ParameterViewModel : ViewModel() {

    // Current User Session
    val currentUser: User?
        get() = UserRepository.currentUser

    // Connection Sessions List
    var connections by mutableStateOf<List<UserConnection>>(emptyList())
        private set

    // Form inputs state
    var firstName by mutableStateOf("")
    var lastName by mutableStateOf("")
    var email by mutableStateOf("")

    var currentPassword by mutableStateOf("")
    var newPassword by mutableStateOf("")

    // Visibility toggles for passwords
    var showCurrentPassword by mutableStateOf(false)
    var showNewPassword by mutableStateOf(false)

    // UI Loading & Message states
    var isLoading by mutableStateOf(false)
        private set

    var profileSuccessMsg by mutableStateOf<String?>(null)
    var profileErrorMsg by mutableStateOf<String?>(null)

    var passwordSuccessMsg by mutableStateOf<String?>(null)
    var passwordErrorMsg by mutableStateOf<String?>(null)

    var googleSuccessMsg by mutableStateOf<String?>(null)
    var googleErrorMsg by mutableStateOf<String?>(null)

    var connectionsErrorMsg by mutableStateOf<String?>(null)

    init {
        // Initialize profile form values with current user data
        currentUser?.let {
            firstName = it.firstName
            lastName = it.lastName
            email = it.email
        }
        loadConnections()
    }

    fun loadConnections() {
        viewModelScope.launch {
            connectionsErrorMsg = null
            val result = UserRepository.getConnections()
            result.onSuccess { list ->
                connections = list
            }.onFailure { e ->
                connectionsErrorMsg = e.localizedMessage ?: "Impossible de charger les sessions actives"
            }
        }
    }

    fun updateProfile() {
        if (firstName.isBlank() || lastName.isBlank() || email.isBlank()) {
            profileErrorMsg = "Tous les champs du profil sont obligatoires"
            return
        }
        viewModelScope.launch {
            isLoading = true
            profileSuccessMsg = null
            profileErrorMsg = null
            val result = UserRepository.updateProfile(firstName, lastName, email)
            result.onSuccess {
                profileSuccessMsg = "Profil mis à jour avec succès"
            }.onFailure { e ->
                profileErrorMsg = e.localizedMessage ?: "Une erreur est survenue"
            }
            isLoading = false
        }
    }

    fun updatePassword() {
        if (currentPassword.isBlank() || newPassword.isBlank()) {
            passwordErrorMsg = "Le mot de passe actuel et le nouveau mot de passe sont requis"
            return
        }
        if (newPassword.length < 8) {
            passwordErrorMsg = "Le nouveau mot de passe doit faire au moins 8 caractères"
            return
        }
        viewModelScope.launch {
            isLoading = true
            passwordSuccessMsg = null
            passwordErrorMsg = null
            val result = UserRepository.updatePassword(currentPassword, newPassword)
            result.onSuccess {
                passwordSuccessMsg = "Mot de passe mis à jour avec succès"
                currentPassword = ""
                newPassword = ""
            }.onFailure { e ->
                passwordErrorMsg = e.localizedMessage ?: "Une erreur est survenue"
            }
            isLoading = false
        }
    }

    fun revokeSession(uuid: String) {
        viewModelScope.launch {
            isLoading = true
            val result = UserRepository.revokeConnection(uuid)
            result.onSuccess {
                loadConnections()
            }.onFailure { e ->
                connectionsErrorMsg = e.localizedMessage ?: "Impossible de déconnecter la session"
            }
            isLoading = false
        }
    }

    fun deleteAccount(onSuccess: () -> Unit) {
        viewModelScope.launch {
            isLoading = true
            val result = UserRepository.deleteAccount()
            result.onSuccess {
                ProjectRepository.clear()
                onSuccess()
            }.onFailure { e ->
                profileErrorMsg = e.localizedMessage ?: "Erreur lors de la suppression du compte"
            }
            isLoading = false
        }
    }

    fun linkGoogle(context: android.content.Context) {
        viewModelScope.launch {
            isLoading = true
            googleSuccessMsg = null
            googleErrorMsg = null

            var idToken: String? = null
            try {
                val credentialManager = androidx.credentials.CredentialManager.create(context)
                val googleIdOption = com.google.android.libraries.identity.googleid.GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId("apps.googleusercontent.com") // Placeholder pour un google Id
                    .setAutoSelectEnabled(false)
                    .build()

                val request = androidx.credentials.GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                val result = credentialManager.getCredential(context, request)
                val credential = result.credential
                if (credential.type == com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdTokenCredential = com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.createFrom(credential.data)
                    idToken = googleIdTokenCredential.idToken
                }
            } catch (e: androidx.credentials.exceptions.GetCredentialException) {
                googleErrorMsg = "Liaison Google annulée ou indisponible."
            } catch (e: Exception) {
                googleErrorMsg = "Erreur Google : ${e.localizedMessage}"
            }

            if (idToken != null) {
                val result = UserRepository.linkGoogle(idToken)
                result.onSuccess {
                    googleSuccessMsg = "Compte Google lié avec succès !"
                }.onFailure { e ->
                    googleErrorMsg = e.localizedMessage ?: "Erreur de liaison Google"
                }
            }
            isLoading = false
        }
    }

    fun logout() {
        UserRepository.clear()
        ProjectRepository.clear()
    }
}
