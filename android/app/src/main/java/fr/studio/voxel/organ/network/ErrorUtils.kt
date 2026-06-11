package fr.studio.voxel.organ.network

fun getErrorMessageForCode(code: Int, defaultMessage: String = "Erreur lors du chargement"): String {
    return when(code) {
        400 -> "Requête incorrecte ($code). Veuillez vérifier vos saisies."
        401 -> "Non authentifié ($code). Veuillez vous reconnecter."
        403 -> "Accès refusé ($code). Vous n'avez pas les autorisations nécessaires pour accéder à cette ressource."
        404 -> "Ressource introuvable ($code). La ressource demandée n'existe pas ou a été supprimée."
        500 -> "Erreur interne du serveur ($code). Le serveur rencontre actuellement un problème technique."
        else -> "$defaultMessage ($code)."
    }
}
