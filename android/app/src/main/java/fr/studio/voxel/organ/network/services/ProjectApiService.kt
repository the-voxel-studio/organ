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
    suspend fun getDetailedProject(@Path("uuid") uuid: String): Response<Any>

    @PUT("/api/projects/{uuid}")
    suspend fun updateProject(@Path("uuid") uuid: String, @Body project: Project): Response<Project>

    @PATCH("/api/projects/{uuid}")
    suspend fun patchProject(@Path("uuid") uuid: String, @Body project: Map<String, Any?>): Response<Project>

    @POST("/api/projects/{uuid}/restore")
    suspend fun restoreProject(@Path("uuid") uuid: String): Response<Unit>

    @GET("/api/projects/{uuid}/permissions")
    suspend fun getProjectPermissions(@Path("uuid") uuid: String): Response<List<String>>

    @GET("/api/projects/{uuid}/stats")
    suspend fun getProjectStats(@Path("uuid") uuid: String): Response<Any>

    @DELETE("/api/projects/{uuid}")
    suspend fun deleteProject(@Path("uuid") uuid: String): Response<Unit>

    // Project Members
    @GET("/api/projects/{projectUuid}/members")
    suspend fun getProjectMembers(@Path("projectUuid") projectUuid: String): Response<List<Any>>

    @GET("/api/projects/{projectUuid}/members/trash")
    suspend fun getTrashedProjectMembers(@Path("projectUuid") projectUuid: String): Response<List<Any>>

    @POST("/api/projects/{projectUuid}/members/invite")
    suspend fun inviteMember(@Path("projectUuid") projectUuid: String, @Body request: InvitationRequest): Response<Any>

    // Project Drive
    @GET("/api/projects/{projectUuid}/drive-config/list-folders")
    suspend fun listDriveFolders(@Path("projectUuid") projectUuid: String): Response<List<Any>>
}

data class InvitationRequest(
    val email: String,
    val role: String? = null
)

data class Project(
    val id: Int?,
    val uuid: String,
    val title: String,
    val description: String?,
    val color: String,

    @SerializedName("status")
    val state: String?,

    @SerializedName("iconType")
    val iconType: String?,

    @SerializedName("iconData")
    val iconData: String?,

    @SerializedName("createdAt")
    val dateCreation: String,

    @SerializedName("deletedAt")
    val dateSuppression: String?,

    // Relations (si votre API les renvoie dans le même objet)
    val memberIds: List<Int>? = emptyList(),
    val organs: List<Organ>? = emptyList()
) {
    /**
     * Helper calculé : Transforme les colonnes icon_type/data en objet ProjectVisual
     * que vos composants Compose (ProjectIconBadge) comprennent déjà.
     */
    val visual: ProjectVisual?
        get() = when (iconType) {
            "EMOJI" -> iconData?.let { ProjectVisual.Emoji(it) }
            "SVG" -> iconData.let { ProjectVisual.SvgXml(it) }
            else -> null
        }
}
