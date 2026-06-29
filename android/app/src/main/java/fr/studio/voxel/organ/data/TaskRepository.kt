package fr.studio.voxel.organ.data

import fr.studio.voxel.organ.network.ApiClient
import fr.studio.voxel.organ.network.services.*
import fr.studio.voxel.organ.network.getErrorMessageForCode
import okhttp3.MultipartBody

object TaskRepository {
    private val taskService = ApiClient.createService(TaskApiService::class.java)

    suspend fun getTasks(projectUuid: String, organUuid: String): Result<List<Task>> {
        return try {
            val response = taskService.getTasks(projectUuid, organUuid)
            if (response.isSuccessful) {
                Result.success(response.body() ?: emptyList())
            } else {
                Result.failure(Exception(getErrorMessageForCode(response.code(), "Impossible de charger les tâches")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createTask(projectUuid: String, organUuid: String, task: Task): Result<Task> {
        return try {
            val response = taskService.createTask(projectUuid, organUuid, task)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(getErrorMessageForCode(response.code(), "Erreur de création de la tâche")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getTrashedTasks(projectUuid: String, organUuid: String): Result<List<Task>> {
        return try {
            val response = taskService.getTrashedTasks(projectUuid, organUuid)
            if (response.isSuccessful) {
                Result.success(response.body() ?: emptyList())
            } else {
                Result.failure(Exception(getErrorMessageForCode(response.code(), "Impossible de charger la corbeille des tâches")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getTask(projectUuid: String, organUuid: String, taskUuid: String): Result<Task> {
        return try {
            val response = taskService.getTask(projectUuid, organUuid, taskUuid)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(getErrorMessageForCode(response.code(), "Erreur de chargement de la tâche")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateTask(projectUuid: String, organUuid: String, taskUuid: String, task: Task): Result<Task> {
        return try {
            val response = taskService.updateTask(projectUuid, organUuid, taskUuid, task)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(getErrorMessageForCode(response.code(), "Erreur lors de la modification de la tâche")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun restoreTask(projectUuid: String, organUuid: String, taskUuid: String): Result<Unit> {
        return try {
            val response = taskService.restoreTask(projectUuid, organUuid, taskUuid)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(getErrorMessageForCode(response.code(), "Erreur lors de la restauration de la tâche")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getTaskTimeline(projectUuid: String, organUuid: String, taskUuid: String, offset: Int, limit: Int): Result<List<TaskTimelineItem>> {
        return try {
            val response = taskService.getTaskTimeline(projectUuid, organUuid, taskUuid, offset, limit)
            if (response.isSuccessful) {
                Result.success(response.body() ?: emptyList())
            } else {
                Result.failure(Exception(getErrorMessageForCode(response.code(), "Erreur de chargement de l'historique")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteTask(projectUuid: String, organUuid: String, taskUuid: String, permanent: Boolean): Result<Unit> {
        return try {
            val response = taskService.deleteTask(projectUuid, organUuid, taskUuid, permanent)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(getErrorMessageForCode(response.code(), "Erreur lors de la suppression de la tâche")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getTaskPermissions(projectUuid: String, organUuid: String, taskUuid: String): Result<TaskPermissionsResponse> {
        return try {
            val response = taskService.getTaskPermissions(projectUuid, organUuid, taskUuid)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(getErrorMessageForCode(response.code(), "Erreur de récupération des permissions")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Assignees
    suspend fun addAssignee(projectUuid: String, organUuid: String, taskUuid: String, userUuid: String): Result<Unit> {
        return try {
            val response = taskService.addAssignee(projectUuid, organUuid, taskUuid, AssigneeRequest(userUuid))
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(getErrorMessageForCode(response.code(), "Erreur lors de l'assignation")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun removeAssignee(projectUuid: String, organUuid: String, taskUuid: String, userUuid: String): Result<Unit> {
        return try {
            val response = taskService.removeAssignee(projectUuid, organUuid, taskUuid, userUuid)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(getErrorMessageForCode(response.code(), "Erreur lors de la désassignation")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Comments
    suspend fun getComments(projectUuid: String, organUuid: String, taskUuid: String): Result<List<TaskCommentResponse>> {
        return try {
            val response = taskService.getComments(projectUuid, organUuid, taskUuid)
            if (response.isSuccessful) {
                Result.success(response.body() ?: emptyList())
            } else {
                Result.failure(Exception(getErrorMessageForCode(response.code(), "Erreur de chargement des commentaires")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getTrashedComments(projectUuid: String, organUuid: String, taskUuid: String): Result<List<TaskCommentResponse>> {
        return try {
            val response = taskService.getTrashedComments(projectUuid, organUuid, taskUuid)
            if (response.isSuccessful) {
                Result.success(response.body() ?: emptyList())
            } else {
                Result.failure(Exception(getErrorMessageForCode(response.code(), "Erreur de chargement de la corbeille des commentaires")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createComment(projectUuid: String, organUuid: String, taskUuid: String, content: String): Result<TaskCommentResponse> {
        return try {
            val response = taskService.createComment(projectUuid, organUuid, taskUuid, CreateCommentRequest(content))
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(getErrorMessageForCode(response.code(), "Erreur lors de l'ajout du commentaire")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteComment(projectUuid: String, organUuid: String, taskUuid: String, commentUuid: String, permanent: Boolean): Result<Unit> {
        return try {
            val response = taskService.deleteComment(projectUuid, organUuid, taskUuid, commentUuid, permanent)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(getErrorMessageForCode(response.code(), "Erreur lors de la suppression du commentaire")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun restoreComment(projectUuid: String, organUuid: String, taskUuid: String, commentUuid: String): Result<Unit> {
        return try {
            val response = taskService.restoreComment(projectUuid, organUuid, taskUuid, commentUuid)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(getErrorMessageForCode(response.code(), "Erreur lors de la restauration du commentaire")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Attachments
    suspend fun getAttachments(projectUuid: String, organUuid: String, taskUuid: String): Result<List<TaskAttachmentResponse>> {
        return try {
            val response = taskService.getAttachments(projectUuid, organUuid, taskUuid)
            if (response.isSuccessful) {
                Result.success(response.body() ?: emptyList())
            } else {
                Result.failure(Exception(getErrorMessageForCode(response.code(), "Erreur de chargement des pièces jointes")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getTrashedAttachments(projectUuid: String, organUuid: String, taskUuid: String): Result<List<TaskAttachmentResponse>> {
        return try {
            val response = taskService.getTrashedAttachments(projectUuid, organUuid, taskUuid)
            if (response.isSuccessful) {
                Result.success(response.body() ?: emptyList())
            } else {
                Result.failure(Exception(getErrorMessageForCode(response.code(), "Erreur de chargement des pièces jointes supprimées")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun uploadAttachment(projectUuid: String, organUuid: String, taskUuid: String, file: MultipartBody.Part): Result<Any> {
        return try {
            val response = taskService.uploadAttachment(projectUuid, organUuid, taskUuid, file)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(getErrorMessageForCode(response.code(), "Erreur lors du dépôt du fichier")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteAttachment(projectUuid: String, organUuid: String, taskUuid: String, attachmentUuid: String, permanent: Boolean): Result<Unit> {
        return try {
            val response = taskService.deleteAttachment(projectUuid, organUuid, taskUuid, attachmentUuid, permanent)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(getErrorMessageForCode(response.code(), "Erreur lors de la suppression du fichier")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun restoreAttachment(projectUuid: String, organUuid: String, taskUuid: String, attachmentUuid: String): Result<Unit> {
        return try {
            val response = taskService.restoreAttachment(projectUuid, organUuid, taskUuid, attachmentUuid)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(getErrorMessageForCode(response.code(), "Erreur de restauration du fichier")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Links
    suspend fun getTaskLinks(projectUuid: String, organUuid: String, taskUuid: String): Result<List<TaskLinkSummary>> {
        return try {
            val response = taskService.getLinks(projectUuid, organUuid, taskUuid)
            if (response.isSuccessful) {
                Result.success(response.body() ?: emptyList())
            } else {
                Result.failure(Exception(getErrorMessageForCode(response.code(), "Erreur de chargement des liens")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createLink(projectUuid: String, organUuid: String, taskUuid: String, url: String, description: String?): Result<TaskLinkSummary> {
        return try {
            val response = taskService.createLink(projectUuid, organUuid, taskUuid, AddLinkRequest(url, description))
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(getErrorMessageForCode(response.code(), "Erreur de création du lien")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteLink(projectUuid: String, organUuid: String, taskUuid: String, linkUuid: String): Result<Unit> {
        return try {
            val response = taskService.deleteLink(projectUuid, organUuid, taskUuid, linkUuid)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(getErrorMessageForCode(response.code(), "Erreur lors de la suppression du lien")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Tags
    suspend fun addTaskTag(projectUuid: String, organUuid: String, taskUuid: String, tagUuid: String): Result<Any> {
        return try {
            val response = taskService.addTaskTag(projectUuid, organUuid, taskUuid, AddTagRequest(tagUuid))
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(getErrorMessageForCode(response.code(), "Erreur lors de l'ajout du tag")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun removeTaskTag(projectUuid: String, organUuid: String, taskUuid: String, tagUuid: String): Result<Unit> {
        return try {
            val response = taskService.removeTaskTag(projectUuid, organUuid, taskUuid, tagUuid)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(getErrorMessageForCode(response.code(), "Erreur lors du retrait du tag")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Dependencies
    suspend fun getDependencies(projectUuid: String, organUuid: String, taskUuid: String): Result<List<TaskDependencyResponse>> {
        return try {
            val response = taskService.getDependencies(projectUuid, organUuid, taskUuid)
            if (response.isSuccessful) {
                Result.success(response.body() ?: emptyList())
            } else {
                Result.failure(Exception(getErrorMessageForCode(response.code(), "Erreur de chargement des dépendances")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addDependency(projectUuid: String, organUuid: String, taskUuid: String, targetTaskUuid: String): Result<Any> {
        return try {
            val response = taskService.addDependency(projectUuid, organUuid, taskUuid, AddDependencyRequest(targetTaskUuid))
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(getErrorMessageForCode(response.code(), "Erreur lors de l'ajout de la dépendance")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun removeDependency(projectUuid: String, organUuid: String, taskUuid: String, targetTaskUuid: String, permanent: Boolean): Result<Unit> {
        return try {
            val response = taskService.removeDependency(projectUuid, organUuid, taskUuid, targetTaskUuid, permanent)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(getErrorMessageForCode(response.code(), "Erreur lors de la suppression de la dépendance")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
