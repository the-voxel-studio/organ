package fr.studio.voxel.organ.viewmodel

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import fr.studio.voxel.organ.ui.create.CreateMode
import fr.studio.voxel.organ.ui.create.IconType
import android.net.Uri

class CreateViewModel : ViewModel() {

    var createMode by mutableStateOf(CreateMode.PROJECT)
        private set

    //Gestion des étapes
    var currentStep by mutableIntStateOf(0)
        private set

    //Données partagées
    var nameInput by mutableStateOf("")
        private set
    var descriptionInput by mutableStateOf("")
        private set
    var optionSelected by mutableStateOf(false)
        private set

    var selectedColorInput by mutableStateOf(Color(0xFFFF7DD4))
        private set
    var currentIconTab by mutableStateOf(IconType.IMAGE)
        private set

    var selectedImageUri by mutableStateOf<Uri?>(null)
        private set

    var selectedEmoji by mutableStateOf("")
        private set

    var selectedSvgCode by mutableStateOf("")
        private set

    fun updateSelectedColor(color: Color) {
        selectedColorInput = color
    }

    // Initialisation (simulée ici, ou configurée via ta navigation)
    fun initMode(mode: CreateMode) {
        this.createMode = mode
        this.currentStep = 0
    }

    fun updateName(name: String) { nameInput = name }
    fun updateDescription(description: String) { descriptionInput = description }
    fun updateOption(checked: Boolean) { optionSelected = checked }
    fun updateIconTab(tab: IconType) { currentIconTab = tab }
    fun updateImageUri(uri: Uri?) { selectedImageUri = uri }
    fun updateEmoji(emoji: String) { selectedEmoji = emoji }
    fun updateSvgCode(code: String) { selectedSvgCode = code }

    // Navigation dynamique selon le type
    fun nextStep() {
        if (createMode == CreateMode.ORGAN && currentStep < 1) {
            currentStep++
        } else {
            // Fin du questionnaire (Sauvegarde en BDD...)
            submit()
        }
    }

    fun previousStep() {
        if (currentStep > 0) currentStep--
    }

    private fun submit() { /* Logique finale */ }
}