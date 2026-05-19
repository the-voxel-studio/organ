package fr.studio.voxel.organ.ViewModel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {
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
    fun handleAuth(mode: fr.studio.voxel.organ.ui.AuthMode){
        if(mode == fr.studio.voxel.organ.ui.AuthMode.SIGN_IN){
            performLogin()
        }else{
            register()
        }
    }

    private fun performLogin(){
        if(mail.isBlank() || password.isBlank()){
            authError = "Veuillez remplir tous les champs"
            return
        }
        viewModelScope.launch {
            isLoading = true
            authError = null
            val isSuccess = checkUserCredentials(mail,password)
            if (isSuccess){
                //Direction Dashboard
            } else{
                authError = "Identifiant ou mot de passe incorrect."
            }
            isLoading = false
        }
    }

    private fun register(){
        if (!isPasswordValid || !passwordsMatch || name.isBlank() || surname.isBlank()){
            authError = "Veuillez vérifier les champs."
            return
        }
        //Ajouter Logique pour avoir acces bdd et api
    }

    private suspend fun checkUserCredentials(identitfier : String, pass: String) : Boolean{
        delay(500)
        return identitfier == "test@gmail.com" && pass == "123456789"
    }
}