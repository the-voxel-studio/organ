package fr.studio.voxel.organ.domain

import fr.studio.voxel.organ.data.UserRepository

class RegisterUseCase(
    private val validateEmail: ValidateEmailUseCase = ValidateEmailUseCase(),
    private val validatePassword: ValidatePasswordUseCase = ValidatePasswordUseCase(),
    private val userRepository: UserRepository = UserRepository
) {
    suspend operator fun invoke(
        mail: String,
        firstName: String,
        lastName: String,
        pass: String,
        confirmPass: String
    ): Result<Unit> {
        if (firstName.isBlank()) {
            return Result.failure(IllegalArgumentException("Le prénom est requis."))
        }
        if (lastName.isBlank()) {
            return Result.failure(IllegalArgumentException("Le nom est requis."))
        }
        if (mail.isBlank()) {
            return Result.failure(IllegalArgumentException("L'adresse e-mail est requise."))
        }
        if (!validateEmail(mail)) {
            return Result.failure(IllegalArgumentException("L'adresse e-mail n'est pas valide."))
        }
        if (pass.isEmpty()) {
            return Result.failure(IllegalArgumentException("Le mot de passe est requis."))
        }
        if (!validatePassword(pass)) {
            return Result.failure(IllegalArgumentException("Le mot de passe doit faire au moins 8 caractères."))
        }
        if (confirmPass.isEmpty()) {
            return Result.failure(IllegalArgumentException("Veuillez confirmer votre mot de passe."))
        }
        if (pass != confirmPass) {
            return Result.failure(IllegalArgumentException("Les mots de passe ne correspondent pas."))
        }

        return userRepository.register(mail.trim(), firstName.trim(), lastName.trim(), pass)
    }
}
