package fr.studio.voxel.organ.network.services

import retrofit2.Response
import retrofit2.http.*

interface TagApiService {
    @GET("/api/projects/{projectUuid}/tags")
    suspend fun getTags(
        @Path("projectUuid") projectUuid: String
    ): Response<List<TagResponse>>

    @POST("/api/projects/{projectUuid}/tags")
    suspend fun createTag(
        @Path("projectUuid") projectUuid: String,
        @Body req: CreateTagRequest
    ): Response<TagResponse>

    @PUT("/api/projects/{projectUuid}/tags/{tagUuid}")
    suspend fun updateTag(
        @Path("projectUuid") projectUuid: String,
        @Path("tagUuid") tagUuid: String,
        @Body req: UpdateTagRequest
    ): Response<TagMessageResponse>

    @DELETE("/api/projects/{projectUuid}/tags/{tagUuid}")
    suspend fun deleteTag(
        @Path("projectUuid") projectUuid: String,
        @Path("tagUuid") tagUuid: String,
        @Query("permanent") permanent: Boolean = false
    ): Response<Unit>

    @GET("/api/projects/{projectUuid}/tags/trash")
    suspend fun getTrashedTags(
        @Path("projectUuid") projectUuid: String
    ): Response<List<TagResponse>>

    @POST("/api/projects/{projectUuid}/tags/{tagUuid}/restore")
    suspend fun restoreTag(
        @Path("projectUuid") projectUuid: String,
        @Path("tagUuid") tagUuid: String
    ): Response<Unit>
}

data class TagResponse(
    val uuid: String,
    val name: String,
    val color: String,
    val deletedAt: String? = null
)

data class CreateTagRequest(
    val name: String,
    val color: String
)

data class UpdateTagRequest(
    val name: String,
    val color: String
)

data class TagMessageResponse(
    val message: String
)
