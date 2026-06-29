package fr.studio.voxel.organ.domain

class ValidateUrlUseCase {
    private val urlRegex = "^(https?://)?[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}(:\\d+)?(/.*)?$".toRegex(RegexOption.IGNORE_CASE)

    operator fun invoke(url: String): Boolean {
        val trimmed = url.trim()
        if (trimmed.isBlank()) return true // On autorise le champ vide si le champ n'est pas obligatoire
        return trimmed.matches(urlRegex)
    }
}
