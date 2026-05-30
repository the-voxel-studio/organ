package fr.studio.voxel.organ.viewmodel

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.studio.voxel.organ.data.UserRepository
import fr.studio.voxel.organ.network.ApiClient
import fr.studio.voxel.organ.network.services.AuthApiService
import fr.studio.voxel.organ.network.services.LoginRequest
import fr.studio.voxel.organ.network.services.RegisterRequest
import fr.studio.voxel.organ.network.services.User
import fr.studio.voxel.organ.ui.authentication.AuthMode
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {

    private val authService = ApiClient.createService(AuthApiService::class.java)

    val currentUser: User?
        get() = UserRepository.currentUser

    val isCheckingSession: Boolean
        get() = UserRepository.isCheckingSession

    //Commun
    var mail by mutableStateOf("")
        private set
    var password by mutableStateOf("")
        private set
    var isLoading by mutableStateOf(false)
        private set
    var authError by mutableStateOf<String?>(null)
        private set

    //Pour SignUp
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

    //Setters
    fun updateMail(newValue: String) { mail = newValue; authError = null }
    fun updatePassword(newValue: String) { password = newValue; authError = null }
    fun updateName(newValue: String) { name = newValue }
    fun updateSurname(newValue: String) { surname = newValue }
    fun updateConfirmPassword(newValue: String) { confirmPassword = newValue }

    //Validation de SignUp
    val passwordsMatch: Boolean
        get() = password == confirmPassword && confirmPassword.isNotEmpty()

    val isPasswordValid: Boolean
        get() = password.length >= 9

    //Autres actions
    fun handleAuth(mode: AuthMode){
        if(mode == AuthMode.SIGN_IN){
            performLogin()
        }else{
            register()
        }
    }

    var navigateToDashboard by mutableStateOf(false)
        private set

    private fun performLogin(){
        if(mail.isBlank() || password.isBlank()){
            authError = "Veuillez remplir tous les champs"
            return
        }
        viewModelScope.launch {
            isLoading = true
            authError = null
           try{
               val response = authService.login(LoginRequest(mail, password))

               if(response.isSuccessful){
                   UserRepository.fetchCurrentUser()
                   navigateToDashboard = true
               } else if (response.code() == 401) {
                   authError = "Le mail ou le mot de passe sont incorrects"
               } else {
                   authError = "Erreur: ${response.code()}"
               }
           }catch (e: Exception){
               authError = "Problème réseau"
           } finally {
               isLoading = false
           }
        }
    }

    fun onNavigated(){
        navigateToDashboard = false
    }

    var registrationSuccess by mutableStateOf(false)
        private set

    fun onRegistrationHandled(){
        registrationSuccess = false
    }

    private fun register(){
        if (!isPasswordValid || !passwordsMatch || name.isBlank() || surname.isBlank()){
            authError = "Veuillez vérifier les champs."
            return
        }
        viewModelScope.launch {
            isLoading = true
            authError = null
            try{
                val response = authService.register(
                    RegisterRequest(email = mail , firstName =  name, lastName = surname, password = password)
                )
                if(response.isSuccessful){
                    registrationSuccess = true
                }else{
                    authError = "Erreur lors de l'inscription."
                }
            } catch (e: Exception){
                authError = "Erreur : ${e.localizedMessage}"
                Log.e("API_ERROR", "Détail : ", e)
            } finally {
                isLoading = false
            }
        }
    }

    fun loginWithGoogle(context: android.content.Context) {
        viewModelScope.launch {
            isLoading = true
            authError = null
            
            var idToken: String? = null
            try {
                val credentialManager = androidx.credentials.CredentialManager.create(context)
                val googleIdOption = com.google.android.libraries.identity.googleid.GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId("805077826497-lu17a6mrre44jl5t4p20nfo9gqf9sddn.apps.googleusercontent.com")
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
                authError = "Authentification Google annulée ou indisponible."
                Log.e("GOOGLE_AUTH", "GetCredentialException", e)
            } catch (e: Exception) {
                authError = "Erreur Google : ${e.localizedMessage}"
                Log.e("GOOGLE_AUTH", "Exception", e)
            }

            if (idToken != null) {
                try {
                    val response = authService.googleLogin(fr.studio.voxel.organ.network.services.GoogleLoginRequest(idToken = idToken))
                    if (response.isSuccessful) {
                        UserRepository.fetchCurrentUser()
                        navigateToDashboard = true
                    } else {
                        val errorBody = response.errorBody()?.string() ?: ""
                        authError = "Erreur d'authentification Google : ${response.code()}"
                    }
                } catch (e: Exception) {
                    authError = "Problème réseau lors de la connexion Google"
                }
            }
            isLoading = false
        }
    }
}