package fr.studio.voxel.organ.domain

class ValidateEmailUseCase {
    private val emailRegex = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$".toRegex()

    operator fun invoke(email: String): Boolean {
        return email.trim().matches(emailRegex)
    }
}
