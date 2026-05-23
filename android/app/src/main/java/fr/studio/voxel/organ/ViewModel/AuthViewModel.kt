package fr.studio.voxel.organ.ViewModel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.studio.voxel.organ.OrganScreen
import fr.studio.voxel.organ.network.ApiClient
import fr.studio.voxel.organ.network.services.AuthApiService
import fr.studio.voxel.organ.network.services.LoginRequest
import fr.studio.voxel.organ.network.services.RegisterRequest
import fr.studio.voxel.organ.ui.AuthMode
import fr.studio.voxel.organ.ui.AuthScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {

    private val authService = ApiClient.createService(AuthApiService::class.java)

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
                   navigateToDashboard = true
               }else{
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
                authError = "Problème réseau: ${e.localizedMessage}"
            } finally {
                isLoading = false
            }
        }
    }
}