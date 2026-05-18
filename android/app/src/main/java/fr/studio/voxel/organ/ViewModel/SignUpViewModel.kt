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

    var password by mutableStateOf("")
        private set

    fun updatePassword(newPassword: String) {
        password = newPassword
    }

    fun register() {
        //Récupère les variables pour la BDD
        val finalName = name

        val finalSurname = surname

        val finalPassword = password
    }
}