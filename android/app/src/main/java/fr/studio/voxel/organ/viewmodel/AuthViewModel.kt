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
import fr.studio.voxel.organ.network.getErrorMessageForCode

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
    fun updateName(newValue: String) { name = newValue; authError = null }
    fun updateSurname(newValue: String) { surname = newValue; authError = null }
    fun updateConfirmPassword(newValue: String) { confirmPassword = newValue; authError = null }

    //Validation de SignUp
    val passwordsMatch: Boolean
        get() = password == confirmPassword && confirmPassword.isNotEmpty()

    val isPasswordValid: Boolean
        get() = password.length >= 8

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
                    authError = "Le mail ou le mot de passe sont incorrects."
                } else {
                    val errorBody = response.errorBody()?.string()
                    val rawError = try {
                        val json = org.json.JSONObject(errorBody ?: "")
                        json.optString("error", json.optString("message", ""))
                    } catch (e: Exception) {
                        ""
                    }
                    authError = if (!rawError.isNullOrBlank()) {
                        rawError
                    } else {
                        getErrorMessageForCode(response.code(), "Une erreur est survenue lors de la connexion")
                    }
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
        if (name.isBlank()) {
            authError = "Le prénom est requis."
            return
        }
        if (surname.isBlank()) {
            authError = "Le nom est requis."
            return
        }
        if (mail.isBlank()) {
            authError = "L'adresse e-mail est requise."
            return
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(mail.trim()).matches()) {
            authError = "L'adresse e-mail n'est pas valide."
            return
        }
        if (password.isEmpty()) {
            authError = "Le mot de passe est requis."
            return
        }
        if (!isPasswordValid) {
            authError = "Le mot de passe doit faire au moins 8 caractères."
            return
        }
        if (confirmPassword.isEmpty()) {
            authError = "Veuillez confirmer votre mot de passe."
            return
        }
        if (!passwordsMatch) {
            authError = "Les mots de passe ne correspondent pas."
            return
        }

        viewModelScope.launch {
            isLoading = true
            authError = null
            try{
                val response = authService.register(
                    RegisterRequest(email = mail.trim(), firstName = name.trim(), lastName = surname.trim(), password = password)
                )
                if(response.isSuccessful){
                    val loginResponse = authService.login(LoginRequest(mail.trim(), password))
                    if(loginResponse.isSuccessful){
                        UserRepository.fetchCurrentUser()
                        navigateToDashboard = true
                    } else {
                        registrationSuccess = true
                    }
                }else{
                    val errorBody = response.errorBody()?.string()
                    val rawError = try {
                        val bodyStr = errorBody ?: ""
                        val json = org.json.JSONObject(bodyStr)
                        if (json.has("detail")) {
                            json.optString("detail")
                        } else if (json.has("violations")) {
                            val violations = json.getJSONArray("violations")
                            val messages = mutableListOf<String>()
                            for (i in 0 until violations.length()) {
                                val violation = violations.getJSONObject(i)
                                val field = violation.optString("propertyPath")
                                val title = violation.optString("title")
                                messages.add("$field: $title")
                            }
                            messages.joinToString("\n")
                        } else {
                            json.optString("error", "Erreur lors de l'inscription.")
                        }
                    } catch (e: Exception) {
                        try {
                            val jsonArray = org.json.JSONArray(errorBody ?: "")
                            val messages = mutableListOf<String>()
                            for (i in 0 until jsonArray.length()) {
                                val obj = jsonArray.getJSONObject(i)
                                val msg = obj.optString("message") ?: obj.optString("title")
                                if (msg.isNotEmpty()) messages.add(msg)
                            }
                            messages.joinToString("\n")
                        } catch (e2: Exception) {
                            "Erreur lors de l'inscription."
                        }
                    }

                    authError = when(rawError) {
                        "User already exists" -> "Cet utilisateur existe déjà."
                        "Password must be at least 8 characters" -> "Le mot de passe doit faire au moins 8 caractères."
                        else -> rawError
                    }
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