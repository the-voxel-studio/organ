package fr.studio.voxel.organ.network.services

import com.google.gson.annotations.SerializedName
import fr.studio.voxel.organ.ui.dashboard.ProjectVisual
import retrofit2.Response
import retrofit2.http.*

interface ProjectApiService {
    @GET("/api/projects")
    suspend fun getProjects(): Response<List<Project>>

    @GET("/api/projects/trash")
    suspend fun getTrashedProjects(): Response<List<Project>>

    @POST("/api/projects")
    suspend fun createProject(@Body project: Project): Response<Project>

    @GET("/api/projects/{uuid}")
    suspend fun getProject(@Path("uuid") uuid: String): Response<Project>

    @GET("/api/projects/{uuid}/detailed")
    suspend fun getDetailedProject(@Path("uuid") uuid: String): Response<ProjectDetailedViewResponse>

    @PUT("/api/projects/{uuid}")
    suspend fun updateProject(@Path("uuid") uuid: String, @Body project: Project): Response<Project>

    @PATCH("/api/projects/{uuid}")
    suspend fun patchProject(@Path("uuid") uuid: String, @Body project: Map<String, Any?>): Response<Project>

    @POST("/api/projects/{uuid}/restore")
    suspend fun restoreProject(@Path("uuid") uuid: String): Response<Unit>

    @GET("/api/projects/{uuid}/permissions")
    suspend fun getProjectPermissions(@Path("uuid") uuid: String): Response<ProjectRoleResponse>

    @GET("/api/projects/{uuid}/stats")
    suspend fun getProjectStats(@Path("uuid") uuid: String): Response<Any>

    @DELETE("/api/projects/{uuid}")
    suspend fun deleteProject(@Path("uuid") uuid: String): Response<Unit>

    // Project Members
    @GET("/api/projects/{projectUuid}/members")
    suspend fun getProjectMembers(@Path("projectUuid") projectUuid: String): Response<List<ProjectMember>>

    @GET("/api/projects/{projectUuid}/members/invitations")
    suspend fun getProjectInvitations(@Path("projectUuid") projectUuid: String): Response<List<ProjectInvitation>>

    @GET("/api/projects/{projectUuid}/members/trash")
    suspend fun getTrashedProjectMembers(@Path("projectUuid") projectUuid: String): Response<List<ProjectMember>>

    @POST("/api/projects/{projectUuid}/members/{memberUuid}/restore")
    suspend fun restoreMember(
        @Path("projectUuid") projectUuid: String,
        @Path("memberUuid") memberUuid: String
    ): Response<Unit>

    @POST("/api/projects/{projectUuid}/members/invite")
    suspend fun inviteMember(@Path("projectUuid") projectUuid: String, @Body request: InvitationRequest): Response<Any>

    @PUT("/api/projects/{projectUuid}/members/{memberUuid}")
    suspend fun updateMemberRole(
        @Path("projectUuid") projectUuid: String,
        @Path("memberUuid") memberUuid: String,
        @Body request: UpdateRoleRequest
    ): Response<Any>

    @DELETE("/api/projects/{projectUuid}/members/{memberUuid}")
    suspend fun removeMember(
        @Path("projectUuid") projectUuid: String,
        @Path("memberUuid") memberUuid: String,
        @Query("permanent") permanent: Boolean = false
    ): Response<Unit>

    // Project Drive
    @GET("/api/projects/{projectUuid}/drive-config/list-folders")
    suspend fun listDriveFolders(@Path("projectUuid") projectUuid: String): Response<List<Any>>
}

data class InvitationRequest(
    val email: String,
    val role: String? = null
)

data class ProjectRoleResponse(
    val role: String
)

data class ProjectMemberUser(
    val uuid: String,
    val email: String,
    val firstName: String,
    val lastName: String
)

data class ProjectMember(
    val uuid: String,
    val user: ProjectMemberUser,
    val role: String,
    val deletedAt: String? = null
)

data class ProjectInvitation(
    val uuid: String,
    val email: String,
    val role: String,
    val createdAt: String,
    val expiresAt: String
)

data class UpdateRoleRequest(
    val role: String
)

data class Project(
    val id: Int?,
    val uuid: String,
    val title: String,
    val description: String?,
    val color: String?,

    @SerializedName("status")
    val state: String?,

    @SerializedName("iconType")
    val iconType: String?,

    @SerializedName("iconData")
    val iconData: String?,

    @SerializedName("createdAt")
    val dateCreation: String?,

    @SerializedName("deletedAt")
    val dateSuppression: String?,

    // Relations (si votre API les renvoie dans le même objet)
    val memberIds: List<Int>? = emptyList(),
    val organs: List<Organ>? = emptyList(),
    val role: String? = null
) {
    /**
     * Helper calculé : Transforme les colonnes icon_type/data en objet ProjectVisual
     * que vos composants Compose (ProjectIconBadge) comprennent déjà.
     */
    val visual: ProjectVisual?
        get() = when (iconType) {
            "EMOJI" -> iconData?.let { ProjectVisual.Emoji(it) }
            "SVG" -> iconData?.let { ProjectVisual.SvgXml(it) }
            "IMAGE", "BLOB" -> iconData?.let { ProjectVisual.Image(it) }
            else -> null
        }
}

data class ProjectDetailedAdmin(
    val firstName: String,
    val lastName: String,
    val email: String
)

data class ProjectDetailedOrgan(
    val uuid: String,
    val title: String,
    val description: String?,
    val iconType: String?,
    val iconData: String?,
    val highlightColor: String?,
    val activeTasksCount: Int
) {
    val visual: ProjectVisual?
        get() = when (iconType) {
            "EMOJI" -> iconData?.let { ProjectVisual.Emoji(it) }
            "SVG" -> iconData?.let { ProjectVisual.SvgXml(it) }
            "IMAGE", "BLOB" -> iconData?.let { ProjectVisual.Image(it) }
            else -> null
        }
}

data class ProjectDetailedMemberUser(
    val uuid: String,
    val email: String,
    val firstName: String,
    val lastName: String
)

data class ProjectDetailedMember(
    val uuid: String,
    val user: ProjectDetailedMemberUser,
    val globalRole: String
)

data class ProjectDetailedActivity(
    val type: String,
    val detail: String?,
    @SerializedName("created_at")
    val createdAt: String?,
    @SerializedName("field_name")
    val fieldName: String?,
    @SerializedName("action_type")
    val actionType: String?,
    @SerializedName("user_first_name")
    val userFirstName: String?,
    @SerializedName("user_last_name")
    val userLastName: String?,
    @SerializedName("task_title")
    val taskTitle: String?,
    @SerializedName("task_uuid")
    val taskUuid: String?,
    @SerializedName("organ_title")
    val organTitle: String?,
    val old_value: com.google.gson.JsonElement?,
    val new_value: com.google.gson.JsonElement?
)

data class ProjectDetailedProject(
    val uuid: String,
    val title: String,
    val description: String?,
    val status: String?,
    val color: String?,
    val iconType: String?,
    val iconData: String?,
    val createdAt: String?,
    val role: String?
) {
    val visual: ProjectVisual?
        get() = when (iconType) {
            "EMOJI" -> iconData?.let { ProjectVisual.Emoji(it) }
            "SVG" -> iconData?.let { ProjectVisual.SvgXml(it) }
            "IMAGE", "BLOB" -> iconData?.let { ProjectVisual.Image(it) }
            else -> null
        }
}

data class ProjectDetailedViewResponse(
    val project: ProjectDetailedProject,
    val admin: ProjectDetailedAdmin?,
    val organs: List<ProjectDetailedOrgan>,
    val members: List<ProjectDetailedMember>,
    val activities: List<ProjectDetailedActivity>
)
