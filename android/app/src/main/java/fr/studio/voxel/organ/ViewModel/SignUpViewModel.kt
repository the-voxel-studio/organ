package fr.studio.voxel.organ.ViewModel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

class SignUpViewModel : ViewModel (
){
    var name by mutableStateOf("")
        private set

    fun updateName(newName: String) {
        name = newName
    }

    var surname by mutableStateOf("")
        private set

    fun updateSurname(newSurname: String) {
        surname = newSurname
    }

    var mail by mutableStateOf("")
        private set

    fun updateMail(newMail: String) {
        mail = newMail
    }

    var password by mutableStateOf("")
        private set

    fun updatePassword(newPassword: String) {
        password = newPassword
    }

    var confirmPassword by mutableStateOf("")
        private set

    fun updateConfirmPassword(newConfirm: String) {
        confirmPassword = newConfirm
    }

    val passwordsMatch: Boolean
        get() = password == confirmPassword && confirmPassword.isNotEmpty()

    val isPasswordValid: Boolean
        get() = password.length >= 9

    fun register() {
        //Récupère les variables pour la BDD
        val finalName = name

        val finalSurname = surname

        val finalMail = mail

        val finalPassword = password
    }
}