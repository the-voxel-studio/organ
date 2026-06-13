package fr.studio.voxel.organ.domain

class ValidatePasswordUseCase {
    operator fun invoke(password: String): Boolean {
        return password.length >= 8
    }
}
