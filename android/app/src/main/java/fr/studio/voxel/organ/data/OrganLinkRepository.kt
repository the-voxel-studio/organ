package fr.studio.voxel.organ.data

import fr.studio.voxel.organ.network.ApiClient
import fr.studio.voxel.organ.network.services.*
import fr.studio.voxel.organ.network.getErrorMessageForCode

object OrganLinkRepository {
    private val linkService = ApiClient.createService(OrganLinkApiService::class.java)

    suspend fun getLinks(projectUuid: String, organUuid: String): Result<List<OrganLinkSummary>> {
        return try {
            val response = linkService.getLinks(projectUuid, organUuid)
            if (response.isSuccessful) {
                Result.success(response.body() ?: emptyList())
            } else {
                Result.failure(Exception(getErrorMessageForCode(response.code(), "Erreur de chargement des liens")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createLink(projectUuid: String, organUuid: String, url: String, description: String?): Result<OrganLinkSummary> {
        return try {
            val response = linkService.createLink(projectUuid, organUuid, CreateOrganLinkRequest(url, description))
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(getErrorMessageForCode(response.code(), "Erreur lors de la création du lien")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateLink(projectUuid: String, organUuid: String, linkUuid: String, url: String?, description: String?): Result<OrganLinkSummary> {
        return try {
            val response = linkService.updateLink(projectUuid, organUuid, linkUuid, UpdateOrganLinkRequest(url, description))
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(getErrorMessageForCode(response.code(), "Erreur lors de la mise à jour du lien")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteLink(projectUuid: String, organUuid: String, linkUuid: String, permanent: Boolean): Result<Unit> {
        return try {
            val response = linkService.deleteLink(projectUuid, organUuid, linkUuid, permanent)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(getErrorMessageForCode(response.code(), "Erreur lors de la suppression du lien")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getTrashedLinks(projectUuid: String, organUuid: String): Result<List<TrashedOrganLinkSummary>> {
        return try {
            val response = linkService.getTrashedLinks(projectUuid, organUuid)
            if (response.isSuccessful) {
                Result.success(response.body() ?: emptyList())
            } else {
                Result.failure(Exception(getErrorMessageForCode(response.code(), "Erreur de chargement des liens supprimés")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun restoreLink(projectUuid: String, organUuid: String, linkUuid: String): Result<Unit> {
        return try {
            val response = linkService.restoreLink(projectUuid, organUuid, linkUuid)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(getErrorMessageForCode(response.code(), "Erreur lors de la restauration du lien")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
