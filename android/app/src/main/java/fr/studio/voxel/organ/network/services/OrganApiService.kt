package fr.studio.voxel.organ.network.services

import fr.studio.voxel.organ.ViewModel.Organ
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
    ): Response<List<String>>

    @GET("/api/projects/{projectUuid}/organs/{organUuid}/members")
    suspend fun getOrganMembers(
        @Path("projectUuid") projectUuid: String,
        @Path("organUuid") organUuid: String
    ): Response<List<Any>>

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
    ): Response<List<Any>>

    @POST("/api/projects/{projectUuid}/organs/{organUuid}/roles/{roleUuid}/assign")
    suspend fun assignRole(
        @Path("projectUuid") projectUuid: String,
        @Path("organUuid") organUuid: String,
        @Path("roleUuid") roleUuid: String,
        @Body request: AssignRoleRequest
    ): Response<Any>
}

data class AssignRoleRequest(
    val userUuid: String
)
