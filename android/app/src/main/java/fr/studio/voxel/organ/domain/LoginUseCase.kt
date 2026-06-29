package fr.studio.voxel.organ.domain

import fr.studio.voxel.organ.data.UserRepository

class LoginUseCase(
    private val validateEmail: ValidateEmailUseCase = ValidateEmailUseCase(),
    private val validatePassword: ValidatePasswordUseCase = ValidatePasswordUseCase(),
    private val userRepository: UserRepository = UserRepository
) {
    suspend operator fun invoke(mail: String, pass: String): Result<Unit> {
        if (mail.isBlank() || pass.isBlank()) {
            return Result.failure(IllegalArgumentException("Veuillez remplir tous les champs"))
        }
        if (!validateEmail(mail)) {
            return Result.failure(IllegalArgumentException("L'adresse e-mail n'est pas valide."))
        }
        return userRepository.login(mail.trim(), pass)
    }
}
