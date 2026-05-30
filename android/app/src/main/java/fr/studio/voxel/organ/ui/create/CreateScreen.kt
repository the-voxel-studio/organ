package fr.studio.voxel.organ.ui.create

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
// MainViewModel removed
import fr.studio.voxel.organ.viewmodel.CreateViewModel

enum class CreateMode{
    PROJECT, ORGAN
}

enum class IconType(val title: String) {
    IMAGE("Image"),
    EMOJI("Emoji"),
    SVG("SVG"),
    CAMERA("Camera")
}

@Composable
fun Create(
    mode : CreateMode,
    createVM : CreateViewModel = viewModel()
){
    LaunchedEffect(mode) {createVM.initMode(mode) }

    Box() {
        when (createVM.createMode) {

            CreateMode.ORGAN -> {
                StepIdentity(
                    mode = mode,
                    name = createVM.nameInput,
                    onNameChanged = { createVM.updateName(it) },
                    description = createVM.descriptionInput,
                    onDescrChanged = {createVM.updateDescription(it)},
                    createMV = createVM
                )
            }

            CreateMode.PROJECT -> {
                when (createVM.currentStep) {
                    0 -> StepIdentity(
                        mode = mode,
                        name = createVM.nameInput,
                        onNameChanged = { createVM.updateName(it) },
                        description = createVM.descriptionInput,
                        onDescrChanged = {createVM.updateDescription(it)},
                        createMV = createVM
                    )

                    //1 -> StepSpecifications()
                }
            }
        }
    }
}
