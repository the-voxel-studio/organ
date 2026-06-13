package fr.studio.voxel.organ.viewmodel

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.studio.voxel.organ.data.UserRepository
import fr.studio.voxel.organ.domain.LoginUseCase
import fr.studio.voxel.organ.domain.RegisterUseCase
import fr.studio.voxel.organ.domain.ValidateEmailUseCase
import fr.studio.voxel.organ.domain.ValidatePasswordUseCase
import fr.studio.voxel.organ.network.services.User
import fr.studio.voxel.organ.ui.authentication.AuthMode
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {

    private val loginUseCase = LoginUseCase()
    private val registerUseCase = RegisterUseCase()
    private val validateEmailUseCase = ValidateEmailUseCase()
    private val validatePasswordUseCase = ValidatePasswordUseCase()

    val currentUser: User?
        get() = UserRepository.currentUser

    val isCheckingSession: Boolean
        get() = UserRepository.isCheckingSession

    // Commun
    var mail by mutableStateOf("")
        private set
    var password by mutableStateOf("")
        private set
    var isLoading by mutableStateOf(false)
        private set
    var authError by mutableStateOf<String?>(null)
        private set

    // Pour SignUp
    var name by mutableStateOf("")
        private set
    var surname by mutableStateOf("")
        private set
    var confirmPassword by mutableStateOf("")
        private set

    init {
        checkSession()
    }

    fun checkSession() {
        viewModelScope.launch {
            UserRepository.checkSession()
        }
    }

    // Setters
    fun updateMail(newValue: String) { mail = newValue; authError = null }
    fun updatePassword(newValue: String) { password = newValue; authError = null }
    fun updateName(newValue: String) { name = newValue; authError = null }
    fun updateSurname(newValue: String) { surname = newValue; authError = null }
    fun updateConfirmPassword(newValue: String) { confirmPassword = newValue; authError = null }

    // Validation de SignUp
    val passwordsMatch: Boolean
        get() = password == confirmPassword && confirmPassword.isNotEmpty()

    val isPasswordValid: Boolean
        get() = validatePasswordUseCase(password)

    // Autres actions
    fun handleAuth(mode: AuthMode) {
        if (mode == AuthMode.SIGN_IN) {
            performLogin()
        } else {
            register()
        }
    }

    var navigateToDashboard by mutableStateOf(false)
        private set

    private fun performLogin() {
        viewModelScope.launch {
            isLoading = true
            authError = null
            loginUseCase(mail, password)
                .onSuccess {
                    navigateToDashboard = true
                }
                .onFailure { e ->
                    authError = e.message ?: "Une erreur est survenue lors de la connexion"
                }
            isLoading = false
        }
    }

    fun onNavigated() {
        navigateToDashboard = false
    }

    var registrationSuccess by mutableStateOf(false)
        private set

    fun onRegistrationHandled() {
        registrationSuccess = false
    }

    private fun register() {
        viewModelScope.launch {
            isLoading = true
            authError = null
            registerUseCase(
                mail = mail,
                firstName = name,
                lastName = surname,
                pass = password,
                confirmPass = confirmPassword
            ).onSuccess {
                navigateToDashboard = true
            }.onFailure { e ->
                if (e is UserRepository.AutoLoginFailedException) {
                    registrationSuccess = true
                } else {
                    authError = e.message ?: "Erreur lors de l'inscription."
                }
            }
            isLoading = false
        }
    }

    fun loginWithGoogle(context: android.content.Context) {
        viewModelScope.launch {
            isLoading = true
            authError = null
            
            var idToken: String? = null
            try {
                val credentialManager = androidx.credentials.CredentialManager.create(context)
                val googleIdOption = com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption.Builder(
                    fr.studio.voxel.organ.network.ApiClient.GOOGLE_SERVER_CLIENT_ID
                ).build()

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
                val sha = getAppSignatureSHA1(context)
                authError = "Erreur Google : ${e.localizedMessage}\nSHA-1 réel de l'app :\n$sha"
                Log.e("GOOGLE_AUTH", "GetCredentialException: ${e.localizedMessage}, SHA-1: $sha", e)
            } catch (e: Exception) {
                val sha = getAppSignatureSHA1(context)
                authError = "Erreur Google : ${e.localizedMessage}\nSHA-1 réel de l'app :\n$sha"
                Log.e("GOOGLE_AUTH", "Exception: ${e.localizedMessage}, SHA-1: $sha", e)
            }

            if (idToken != null) {
                UserRepository.loginWithGoogle(idToken)
                    .onSuccess {
                        navigateToDashboard = true
                    }
                    .onFailure { e ->
                        authError = e.message ?: "Problème réseau lors de la connexion Google"
                    }
            }
            isLoading = false
        }
    }

    private fun getAppSignatureSHA1(context: android.content.Context): String {
        try {
            val packageInfo = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    android.content.pm.PackageManager.GET_SIGNING_CERTIFICATES
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(
                    context.packageName,
                    android.content.pm.PackageManager.GET_SIGNATURES
                )
            }
            val signatures = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                packageInfo.signingInfo?.apkContentsSigners
            } else {
                @Suppress("DEPRECATION")
                packageInfo.signatures
            }
            if (signatures != null && signatures.isNotEmpty()) {
                val md = java.security.MessageDigest.getInstance("SHA-1")
                val publicKey = md.digest(signatures[0].toByteArray())
                return publicKey.joinToString(":") { String.format("%02X", it) }
            }
        } catch (e: Exception) {
            android.util.Log.e("SIGNATURE_CHECK", "Error getting signature", e)
        }
        return "Signature inconnue"
    }
}