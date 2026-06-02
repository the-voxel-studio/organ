package fr.studio.voxel.organ.network.services

import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

interface TaskApiService {
    @GET("/api/projects/{projectUuid}/organs/{organUuid}/tasks")
    suspend fun getTasks(
        @Path("projectUuid") projectUuid: String,
        @Path("organUuid") organUuid: String
    ): Response<List<Task>>

    @POST("/api/projects/{projectUuid}/organs/{organUuid}/tasks")
    suspend fun createTask(
        @Path("projectUuid") projectUuid: String,
        @Path("organUuid") organUuid: String,
        @Body task: Task
    ): Response<Task>

    @GET("/api/projects/{projectUuid}/organs/{organUuid}/tasks/trash")
    suspend fun getTrashedTasks(
        @Path("projectUuid") projectUuid: String,
        @Path("organUuid") organUuid: String
    ): Response<List<Task>>

    @GET("/api/projects/{projectUuid}/organs/{organUuid}/tasks/{taskUuid}")
    suspend fun getTask(
        @Path("projectUuid") projectUuid: String,
        @Path("organUuid") organUuid: String,
        @Path("taskUuid") taskUuid: String
    ): Response<Task>

    @PUT("/api/projects/{projectUuid}/organs/{organUuid}/tasks/{taskUuid}")
    suspend fun updateTask(
        @Path("projectUuid") projectUuid: String,
        @Path("organUuid") organUuid: String,
        @Path("taskUuid") taskUuid: String,
        @Body task: Task
    ): Response<Task>

    @POST("/api/projects/{projectUuid}/organs/{organUuid}/tasks/{taskUuid}/restore")
    suspend fun restoreTask(
        @Path("projectUuid") projectUuid: String,
        @Path("organUuid") organUuid: String,
        @Path("taskUuid") taskUuid: String
    ): Response<Unit>

    @GET("/api/projects/{projectUuid}/organs/{organUuid}/tasks/{taskUuid}/timeline")
    suspend fun getTaskTimeline(
        @Path("projectUuid") projectUuid: String,
        @Path("organUuid") organUuid: String,
        @Path("taskUuid") taskUuid: String,
        @Query("offset") offset: Int,
        @Query("limit") limit: Int
    ): Response<List<TaskTimelineItem>>

    @DELETE("/api/projects/{projectUuid}/organs/{organUuid}/tasks/{taskUuid}")
    suspend fun deleteTask(
        @Path("projectUuid") projectUuid: String,
        @Path("organUuid") organUuid: String,
        @Path("taskUuid") taskUuid: String,
        @Query("permanent") permanent: Boolean
    ): Response<Unit>

    @GET("/api/projects/{projectUuid}/organs/{organUuid}/tasks/{taskUuid}/permissions")
    suspend fun getTaskPermissions(
        @Path("projectUuid") projectUuid: String,
        @Path("organUuid") organUuid: String,
        @Path("taskUuid") taskUuid: String
    ): Response<TaskPermissionsResponse>

    // Assignees
    @POST("/api/projects/{projectUuid}/organs/{organUuid}/tasks/{taskUuid}/assignees")
    suspend fun addAssignee(
        @Path("projectUuid") projectUuid: String,
        @Path("organUuid") organUuid: String,
        @Path("taskUuid") taskUuid: String,
        @Body request: AssigneeRequest
    ): Response<Any>

    @DELETE("/api/projects/{projectUuid}/organs/{organUuid}/tasks/{taskUuid}/assignees/{userUuid}")
    suspend fun removeAssignee(
        @Path("projectUuid") projectUuid: String,
        @Path("organUuid") organUuid: String,
        @Path("taskUuid") taskUuid: String,
        @Path("userUuid") userUuid: String
    ): Response<Unit>

    // Comments
    @GET("/api/projects/{projectUuid}/organs/{organUuid}/tasks/{taskUuid}/comments")
    suspend fun getComments(
        @Path("projectUuid") projectUuid: String,
        @Path("organUuid") organUuid: String,
        @Path("taskUuid") taskUuid: String
    ): Response<List<TaskCommentResponse>>

    @GET("/api/projects/{projectUuid}/organs/{organUuid}/tasks/{taskUuid}/comments/trash")
    suspend fun getTrashedComments(
        @Path("projectUuid") projectUuid: String,
        @Path("organUuid") organUuid: String,
        @Path("taskUuid") taskUuid: String
    ): Response<List<TaskCommentResponse>>

    @POST("/api/projects/{projectUuid}/organs/{organUuid}/tasks/{taskUuid}/comments")
    suspend fun createComment(
        @Path("projectUuid") projectUuid: String,
        @Path("organUuid") organUuid: String,
        @Path("taskUuid") taskUuid: String,
        @Body request: CreateCommentRequest
    ): Response<TaskCommentResponse>

    @DELETE("/api/projects/{projectUuid}/organs/{organUuid}/tasks/{taskUuid}/comments/{commentUuid}")
    suspend fun deleteComment(
        @Path("projectUuid") projectUuid: String,
        @Path("organUuid") organUuid: String,
        @Path("taskUuid") taskUuid: String,
        @Path("commentUuid") commentUuid: String,
        @Query("permanent") permanent: Boolean
    ): Response<Unit>

    @POST("/api/projects/{projectUuid}/organs/{organUuid}/tasks/{taskUuid}/comments/{commentUuid}/restore")
    suspend fun restoreComment(
        @Path("projectUuid") projectUuid: String,
        @Path("organUuid") organUuid: String,
        @Path("taskUuid") taskUuid: String,
        @Path("commentUuid") commentUuid: String
    ): Response<Unit>

    // Attachments
    @GET("/api/projects/{projectUuid}/organs/{organUuid}/tasks/{taskUuid}/attachments")
    suspend fun getAttachments(
        @Path("projectUuid") projectUuid: String,
        @Path("organUuid") organUuid: String,
        @Path("taskUuid") taskUuid: String
    ): Response<List<TaskAttachmentResponse>>

    @GET("/api/projects/{projectUuid}/organs/{organUuid}/tasks/{taskUuid}/attachments/trash")
    suspend fun getTrashedAttachments(
        @Path("projectUuid") projectUuid: String,
        @Path("organUuid") organUuid: String,
        @Path("taskUuid") taskUuid: String
    ): Response<List<TaskAttachmentResponse>>

    @Multipart
    @POST("/api/projects/{projectUuid}/organs/{organUuid}/tasks/{taskUuid}/attachments")
    suspend fun uploadAttachment(
        @Path("projectUuid") projectUuid: String,
        @Path("organUuid") organUuid: String,
        @Path("taskUuid") taskUuid: String,
        @Part file: MultipartBody.Part
    ): Response<Any>

    @DELETE("/api/projects/{projectUuid}/organs/{organUuid}/tasks/{taskUuid}/attachments/{attachmentUuid}")
    suspend fun deleteAttachment(
        @Path("projectUuid") projectUuid: String,
        @Path("organUuid") organUuid: String,
        @Path("taskUuid") taskUuid: String,
        @Path("attachmentUuid") attachmentUuid: String,
        @Query("permanent") permanent: Boolean
    ): Response<Unit>

    @POST("/api/projects/{projectUuid}/organs/{organUuid}/tasks/{taskUuid}/attachments/{attachmentUuid}/restore")
    suspend fun restoreAttachment(
        @Path("projectUuid") projectUuid: String,
        @Path("organUuid") organUuid: String,
        @Path("taskUuid") taskUuid: String,
        @Path("attachmentUuid") attachmentUuid: String
    ): Response<Unit>

    // Links
    @GET("/api/projects/{projectUuid}/organs/{organUuid}/tasks/{taskUuid}/links")
    suspend fun getLinks(
        @Path("projectUuid") projectUuid: String,
        @Path("organUuid") organUuid: String,
        @Path("taskUuid") taskUuid: String
    ): Response<List<TaskLinkSummary>>

    @POST("/api/projects/{projectUuid}/organs/{organUuid}/tasks/{taskUuid}/links")
    suspend fun createLink(
        @Path("projectUuid") projectUuid: String,
        @Path("organUuid") organUuid: String,
        @Path("taskUuid") taskUuid: String,
        @Body request: AddLinkRequest
    ): Response<TaskLinkSummary>

    @DELETE("/api/projects/{projectUuid}/organs/{organUuid}/tasks/{taskUuid}/links/{linkUuid}")
    suspend fun deleteLink(
        @Path("projectUuid") projectUuid: String,
        @Path("organUuid") organUuid: String,
        @Path("taskUuid") taskUuid: String,
        @Path("linkUuid") linkUuid: String
    ): Response<Unit>

    // Tags
    @POST("/api/projects/{projectUuid}/organs/{organUuid}/tasks/{taskUuid}/tags")
    suspend fun addTaskTag(
        @Path("projectUuid") projectUuid: String,
        @Path("organUuid") organUuid: String,
        @Path("taskUuid") taskUuid: String,
        @Body request: AddTagRequest
    ): Response<Any>

    @DELETE("/api/projects/{projectUuid}/organs/{organUuid}/tasks/{taskUuid}/tags/{tagUuid}")
    suspend fun removeTaskTag(
        @Path("projectUuid") projectUuid: String,
        @Path("organUuid") organUuid: String,
        @Path("taskUuid") taskUuid: String,
        @Path("tagUuid") tagUuid: String
    ): Response<Unit>

    // Dependencies
    @GET("/api/projects/{projectUuid}/organs/{organUuid}/tasks/{taskUuid}/dependencies")
    suspend fun getDependencies(
        @Path("projectUuid") projectUuid: String,
        @Path("organUuid") organUuid: String,
        @Path("taskUuid") taskUuid: String
    ): Response<List<TaskDependencyResponse>>

    @POST("/api/projects/{projectUuid}/organs/{organUuid}/tasks/{taskUuid}/dependencies")
    suspend fun addDependency(
        @Path("projectUuid") projectUuid: String,
        @Path("organUuid") organUuid: String,
        @Path("taskUuid") taskUuid: String,
        @Body request: AddDependencyRequest
    ): Response<Any>

    @DELETE("/api/projects/{projectUuid}/organs/{organUuid}/tasks/{taskUuid}/dependencies/{targetTaskUuid}")
    suspend fun removeDependency(
        @Path("projectUuid") projectUuid: String,
        @Path("organUuid") organUuid: String,
        @Path("taskUuid") taskUuid: String,
        @Path("targetTaskUuid") targetTaskUuid: String,
        @Query("permanent") permanent: Boolean
    ): Response<Unit>
}

data class AssigneeRequest(
    val userUuid: String
)

data class Task(
    val id: Int?,
    val uuid: String,
    val organId: Int?,
    val createdBy: User?,
    val manager: User?,
    val title: String,
    val description: String?,
    val priority: Int,
    val status: String,
    val estimatedHours: String?,
    val startDate: String?,
    val expiresAt: String?,
    val createdAt: String?,
    val updatedAt: String?,
    val validedAt: String?,
    val deletedAt: String?,
    val projectName: String? = null,
    val organName: String? = null,
    val projectUuid: String? = null,
    val organUuid: String? = null,
    val statusMessage: String? = null,
    val assignees: List<User>? = null,
    val tags: List<TaskTagSummary>? = null,
    val links: List<TaskLinkSummary>? = null,
    val commentCount: Int? = null,
    val attachmentCount: Int? = null,
    val dependencyCount: Int? = null
)

data class TaskTagSummary(
    val uuid: String,
    val name: String,
    val color: String
)

data class TaskLinkSummary(
    val uuid: String,
    val url: String,
    val description: String? = null
)

data class TaskPermissionsResponse(
    val permissions: List<String>,
    val editableFields: List<String>,
    val isProjectAdmin: Boolean,
    val taskOwnership: TaskOwnership
)

data class TaskOwnership(
    val isManager: Boolean,
    val isAssignee: Boolean,
    val isCreator: Boolean
)

data class TaskCommentResponse(
    val uuid: String,
    val content: String,
    val user: User,
    val createdAt: String,
    val deletedAt: String? = null
)

data class CreateCommentRequest(
    val content: String
)

data class TaskAttachmentResponse(
    val uuid: String,
    val fileName: String,
    val fileSize: Long,
    val fileType: String,
    val filePath: String,
    val uploadedBy: User,
    val createdAt: String,
    val deletedAt: String? = null
)

data class AddLinkRequest(
    val url: String,
    val description: String?
)

data class AddTagRequest(
    val tagUuid: String
)

data class AddDependencyRequest(
    val dependsOnTaskUuid: String
)

data class TaskDependencyResponse(
    val dependsOnTaskUuid: String,
    val title: String,
    val status: String
)

data class TaskTimelineItem(
    val type: String, // COMMENT, ATTACHMENT, HISTORY
    val detail: String?,
    val actionType: String?,
    val fieldName: String?,
    val oldValue: Any?,
    val newValue: Any?,
    val createdAt: String,
    val userName: String?,
    val userUuid: String?
)
