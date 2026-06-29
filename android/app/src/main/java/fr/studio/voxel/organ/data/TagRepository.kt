package fr.studio.voxel.organ.data

import fr.studio.voxel.organ.network.ApiClient
import fr.studio.voxel.organ.network.services.*
import fr.studio.voxel.organ.network.getErrorMessageForCode

object TagRepository {
    private val tagService = ApiClient.createService(TagApiService::class.java)

    suspend fun getTags(projectUuid: String): Result<List<TagResponse>> {
        return try {
            val response = tagService.getTags(projectUuid)
            if (response.isSuccessful) {
                Result.success(response.body() ?: emptyList())
            } else {
                Result.failure(Exception(getErrorMessageForCode(response.code(), "Impossible de charger les tags")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createTag(projectUuid: String, name: String, color: String): Result<TagResponse> {
        return try {
            val response = tagService.createTag(projectUuid, CreateTagRequest(name, color))
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(getErrorMessageForCode(response.code(), "Erreur lors de la création du tag")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateTag(projectUuid: String, tagUuid: String, name: String, color: String): Result<TagMessageResponse> {
        return try {
            val response = tagService.updateTag(projectUuid, tagUuid, UpdateTagRequest(name, color))
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(getErrorMessageForCode(response.code(), "Erreur lors de la mise à jour du tag")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteTag(projectUuid: String, tagUuid: String, permanent: Boolean = false): Result<Unit> {
        return try {
            val response = tagService.deleteTag(projectUuid, tagUuid, permanent)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(getErrorMessageForCode(response.code(), "Erreur lors de la suppression du tag")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getTrashedTags(projectUuid: String): Result<List<TagResponse>> {
        return try {
            val response = tagService.getTrashedTags(projectUuid)
            if (response.isSuccessful) {
                Result.success(response.body() ?: emptyList())
            } else {
                Result.failure(Exception(getErrorMessageForCode(response.code(), "Impossible de charger la corbeille des tags")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun restoreTag(projectUuid: String, tagUuid: String): Result<Unit> {
        return try {
            val response = tagService.restoreTag(projectUuid, tagUuid)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(getErrorMessageForCode(response.code(), "Erreur lors de la restauration du tag")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
