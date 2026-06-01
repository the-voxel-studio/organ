package fr.studio.voxel.organ.network.services

import retrofit2.Response
import retrofit2.http.*

interface OrganLinkApiService {
    @GET("/api/projects/{projectUuid}/organs/{organUuid}/links")
    suspend fun getLinks(
        @Path("projectUuid") projectUuid: String,
        @Path("organUuid") organUuid: String
    ): Response<List<OrganLinkSummary>>

    @POST("/api/projects/{projectUuid}/organs/{organUuid}/links")
    suspend fun createLink(
        @Path("projectUuid") projectUuid: String,
        @Path("organUuid") organUuid: String,
        @Body request: CreateOrganLinkRequest
    ): Response<OrganLinkSummary>

    @PUT("/api/projects/{projectUuid}/organs/{organUuid}/links/{linkUuid}")
    suspend fun updateLink(
        @Path("projectUuid") projectUuid: String,
        @Path("organUuid") organUuid: String,
        @Path("linkUuid") linkUuid: String,
        @Body request: UpdateOrganLinkRequest
    ): Response<OrganLinkSummary>

    @DELETE("/api/projects/{projectUuid}/organs/{organUuid}/links/{linkUuid}")
    suspend fun deleteLink(
        @Path("projectUuid") projectUuid: String,
        @Path("organUuid") organUuid: String,
        @Path("linkUuid") linkUuid: String,
        @Query("permanent") permanent: Boolean
    ): Response<Unit>
}

data class OrganLinkSummary(
    val uuid: String,
    val url: String,
    val description: String?
)

data class CreateOrganLinkRequest(
    val url: String,
    val description: String? = null
)

data class UpdateOrganLinkRequest(
    val url: String? = null,
    val description: String? = null
)
