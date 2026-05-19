package fr.studio.voxel.organ.network.services

import fr.studio.voxel.organ.ViewModel.Task
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
        @Path("taskUuid") taskUuid: String
    ): Response<Any>

    @DELETE("/api/projects/{projectUuid}/organs/{organUuid}/tasks/{taskUuid}")
    suspend fun deleteTask(
        @Path("projectUuid") projectUuid: String,
        @Path("organUuid") organUuid: String,
        @Path("taskUuid") taskUuid: String
    ): Response<Unit>

    // Task Assignees
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
}

data class AssigneeRequest(
    val userUuid: String
)
