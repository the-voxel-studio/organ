package fr.studio.voxel.organ.data

import fr.studio.voxel.organ.network.ApiClient
import fr.studio.voxel.organ.network.services.*
import fr.studio.voxel.organ.network.getErrorMessageForCode

object OrganRepository {
    private val organService = ApiClient.createService(OrganApiService::class.java)

    suspend fun getOrgans(projectUuid: String): Result<List<Organ>> {
        return try {
            val response = organService.getOrgans(projectUuid)
            if (response.isSuccessful) {
                Result.success(response.body() ?: emptyList())
            } else {
                Result.failure(Exception(getErrorMessageForCode(response.code(), "Impossible de charger les Organs")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createOrgan(projectUuid: String, organ: Organ): Result<Organ> {
        return try {
            val response = organService.createOrgan(projectUuid, organ)
            if (response.isSuccessful && response.body() != null) {
                val created = response.body()!!
                ProjectRepository.addOrUpdateOrgan(projectUuid, created)
                Result.success(created)
            } else {
                Result.failure(Exception(getErrorMessageForCode(response.code(), "Erreur lors de la création de l'Organ")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getTrashedOrgans(projectUuid: String): Result<List<Organ>> {
        return try {
            val response = organService.getTrashedOrgans(projectUuid)
            if (response.isSuccessful) {
                Result.success(response.body() ?: emptyList())
            } else {
                Result.failure(Exception(getErrorMessageForCode(response.code(), "Impossible de charger la corbeille des Organs")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getOrgan(projectUuid: String, organUuid: String): Result<Organ> {
        return try {
            val response = organService.getOrgan(projectUuid, organUuid)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(getErrorMessageForCode(response.code(), "Erreur lors du chargement de l'Organ")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateOrgan(projectUuid: String, organUuid: String, organ: Organ): Result<Organ> {
        return try {
            val response = organService.updateOrgan(projectUuid, organUuid, organ)
            if (response.isSuccessful && response.body() != null) {
                val updated = response.body()!!
                ProjectRepository.addOrUpdateOrgan(projectUuid, updated)
                Result.success(updated)
            } else {
                Result.failure(Exception(getErrorMessageForCode(response.code(), "Erreur lors de la mise à jour de l'Organ")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun restoreOrgan(projectUuid: String, organUuid: String): Result<Unit> {
        return try {
            val response = organService.restoreOrgan(projectUuid, organUuid)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(getErrorMessageForCode(response.code(), "Erreur lors de la restauration de l'Organ")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getOrganPermissions(projectUuid: String, organUuid: String): Result<OrganPermissionsResponse> {
        return try {
            val response = organService.getOrganPermissions(projectUuid, organUuid)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(getErrorMessageForCode(response.code(), "Erreur lors de la récupération des permissions")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteOrgan(projectUuid: String, organUuid: String, permanent: Boolean = false): Result<Unit> {
        return try {
            val response = organService.deleteOrgan(projectUuid, organUuid, permanent)
            if (response.isSuccessful) {
                if (!permanent) {
                    ProjectRepository.removeOrgan(projectUuid, organUuid)
                }
                Result.success(Unit)
            } else {
                Result.failure(Exception(getErrorMessageForCode(response.code(), "Erreur lors de la suppression de l'Organ")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getOrganRoles(projectUuid: String, organUuid: String): Result<List<OrganRoleResponse>> {
        return try {
            val response = organService.getOrganRoles(projectUuid, organUuid)
            if (response.isSuccessful) {
                Result.success(response.body() ?: emptyList())
            } else {
                Result.failure(Exception(getErrorMessageForCode(response.code(), "Impossible de charger les rôles de l'Organ")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createOrganRole(projectUuid: String, organUuid: String, name: String, iconType: String, iconData: String?, permissions: List<String>): Result<OrganRoleResponse> {
        return try {
            val response = organService.createOrganRole(projectUuid, organUuid, CreateRoleRequest(name, iconType, iconData, permissions))
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(getErrorMessageForCode(response.code(), "Erreur lors de la création du rôle")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateOrganRole(projectUuid: String, organUuid: String, roleUuid: String, name: String, iconType: String, iconData: String?, permissions: List<String>): Result<OrganRoleResponse> {
        return try {
            val response = organService.updateOrganRole(projectUuid, organUuid, roleUuid, CreateRoleRequest(name, iconType, iconData, permissions))
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(getErrorMessageForCode(response.code(), "Erreur lors de la mise à jour du rôle")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteOrganRole(projectUuid: String, organUuid: String, roleUuid: String, permanent: Boolean = false): Result<Unit> {
        return try {
            val response = organService.deleteOrganRole(projectUuid, organUuid, roleUuid, permanent)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(getErrorMessageForCode(response.code(), "Erreur lors de la suppression du rôle")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun assignRole(projectUuid: String, organUuid: String, roleUuid: String, userUuid: String): Result<Unit> {
        return try {
            val response = organService.assignRole(projectUuid, organUuid, roleUuid, AssignRoleRequest(userUuid))
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(getErrorMessageForCode(response.code(), "Erreur lors de l'assignation du rôle")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun unassignRole(projectUuid: String, organUuid: String, roleUuid: String, userUuid: String, permanent: Boolean = false): Result<Unit> {
        return try {
            val response = organService.unassignRole(projectUuid, organUuid, roleUuid, userUuid, permanent)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(getErrorMessageForCode(response.code(), "Erreur lors de la désassignation du rôle")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getTrashedRoles(projectUuid: String, organUuid: String): Result<List<OrganRoleResponse>> {
        return try {
            val response = organService.getTrashedRoles(projectUuid, organUuid)
            if (response.isSuccessful) {
                Result.success(response.body() ?: emptyList())
            } else {
                Result.failure(Exception(getErrorMessageForCode(response.code(), "Erreur de chargement des rôles supprimés")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getTrashedMembers(projectUuid: String, organUuid: String): Result<List<TrashedRoleMemberResponse>> {
        return try {
            val response = organService.getTrashedMembers(projectUuid, organUuid)
            if (response.isSuccessful) {
                Result.success(response.body() ?: emptyList())
            } else {
                Result.failure(Exception(getErrorMessageForCode(response.code(), "Erreur de chargement des membres supprimés")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun restoreRole(projectUuid: String, organUuid: String, roleUuid: String): Result<Unit> {
        return try {
            val response = organService.restoreRole(projectUuid, organUuid, roleUuid)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(getErrorMessageForCode(response.code(), "Erreur lors de la restauration du rôle")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun restoreMember(projectUuid: String, organUuid: String, uorId: Int): Result<Unit> {
        return try {
            val response = organService.restoreMember(projectUuid, organUuid, uorId)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(getErrorMessageForCode(response.code(), "Erreur lors de la restauration du membre")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAvailablePermissions(): Result<List<AvailablePermission>> {
        return try {
            val response = organService.getAvailablePermissions()
            if (response.isSuccessful) {
                Result.success(response.body() ?: emptyList())
            } else {
                Result.failure(Exception(getErrorMessageForCode(response.code(), "Erreur de chargement des permissions")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
