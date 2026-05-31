package fr.studio.voxel.organ.network.services

import fr.studio.voxel.organ.ui.dashboard.ProjectVisual
import retrofit2.Response
import retrofit2.http.*

interface OrganApiService {
    @GET("/api/projects/{projectUuid}/organs")
    suspend fun getOrgans(@Path("projectUuid") projectUuid: String): Response<List<Organ>>

    @POST("/api/projects/{projectUuid}/organs")
    suspend fun createOrgan(@Path("projectUuid") projectUuid: String, @Body organ: Organ): Response<Organ>

    @GET("/api/projects/{projectUuid}/organs/trash")
    suspend fun getTrashedOrgans(@Path("projectUuid") projectUuid: String): Response<List<Organ>>

    @GET("/api/projects/{projectUuid}/organs/{organUuid}")
    suspend fun getOrgan(
        @Path("projectUuid") projectUuid: String,
        @Path("organUuid") organUuid: String
    ): Response<Organ>

    @PUT("/api/projects/{projectUuid}/organs/{organUuid}")
    suspend fun updateOrgan(
        @Path("projectUuid") projectUuid: String,
        @Path("organUuid") organUuid: String,
        @Body organ: Organ
    ): Response<Organ>

    @POST("/api/projects/{projectUuid}/organs/{organUuid}/restore")
    suspend fun restoreOrgan(
        @Path("projectUuid") projectUuid: String,
        @Path("organUuid") organUuid: String
    ): Response<Unit>

    @GET("/api/projects/{projectUuid}/organs/{organUuid}/permissions")
    suspend fun getOrganPermissions(
        @Path("projectUuid") projectUuid: String,
        @Path("organUuid") organUuid: String
    ): Response<OrganPermissionsResponse>

    @DELETE("/api/projects/{projectUuid}/organs/{organUuid}")
    suspend fun deleteOrgan(
        @Path("projectUuid") projectUuid: String,
        @Path("organUuid") organUuid: String
    ): Response<Unit>

    // Organ Roles
    @GET("/api/projects/{projectUuid}/organs/{organUuid}/roles")
    suspend fun getOrganRoles(
        @Path("projectUuid") projectUuid: String,
        @Path("organUuid") organUuid: String
    ): Response<List<OrganRoleResponse>>

    @POST("/api/projects/{projectUuid}/organs/{organUuid}/roles")
    suspend fun createOrganRole(
        @Path("projectUuid") projectUuid: String,
        @Path("organUuid") organUuid: String,
        @Body role: CreateRoleRequest
    ): Response<OrganRoleResponse>

    @PUT("/api/projects/{projectUuid}/organs/{organUuid}/roles/{roleUuid}")
    suspend fun updateOrganRole(
        @Path("projectUuid") projectUuid: String,
        @Path("organUuid") organUuid: String,
        @Path("roleUuid") roleUuid: String,
        @Body role: CreateRoleRequest
    ): Response<OrganRoleResponse>

    @DELETE("/api/projects/{projectUuid}/organs/{organUuid}/roles/{roleUuid}")
    suspend fun deleteOrganRole(
        @Path("projectUuid") projectUuid: String,
        @Path("organUuid") organUuid: String,
        @Path("roleUuid") roleUuid: String
    ): Response<Unit>

    @POST("/api/projects/{projectUuid}/organs/{organUuid}/roles/{roleUuid}/assign")
    suspend fun assignRole(
        @Path("projectUuid") projectUuid: String,
        @Path("organUuid") organUuid: String,
        @Path("roleUuid") roleUuid: String,
        @Body request: AssignRoleRequest
    ): Response<Any>

    @DELETE("/api/projects/{projectUuid}/organs/{organUuid}/roles/{roleUuid}/unassign/{userUuid}")
    suspend fun unassignRole(
        @Path("projectUuid") projectUuid: String,
        @Path("organUuid") organUuid: String,
        @Path("roleUuid") roleUuid: String,
        @Path("userUuid") userUuid: String
    ): Response<Unit>

    @GET("/api/permissions/available")
    suspend fun getAvailablePermissions(): Response<List<AvailablePermission>>
}

data class AssignRoleRequest(
    val userUuid: String
)

data class OrganPermissionsResponse(
    val permissions: List<String>
)

data class AvailablePermission(
    val name: String
)

data class OrganRoleResponse(
    val uuid: String,
    val name: String,
    val iconType: String,
    val iconData: String?,
    val permissions: List<String>,
    val members: List<OrganRoleMember>
)

data class OrganRoleMember(
    val uuid: String,
    val firstName: String,
    val lastName: String,
    val email: String
)

data class CreateRoleRequest(
    val name: String,
    val iconType: String,
    val iconData: String?,
    val permissions: List<String>
)

data class Organ(
    val id: Int?,
    val uuid: String,
    val projectId: Int,
    val title: String,
    val description: String?,
    val iconType : String?,
    val iconData: String?,
    val highlightColor: String,
    val createdAt: String,
    val deletedAt: String?
){
    val visual: ProjectVisual?
        get() = when (iconType) {
            "EMOJI" -> iconData?.let { ProjectVisual.Emoji(it) }
            "SVG" -> iconData?.let { ProjectVisual.SvgXml(it) }
            "IMAGE", "BLOB" -> iconData?.let { ProjectVisual.Image(it) }
            else -> null
        }
}
