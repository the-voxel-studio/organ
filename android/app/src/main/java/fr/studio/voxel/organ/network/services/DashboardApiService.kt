package fr.studio.voxel.organ.network.services

import retrofit2.Response
import retrofit2.http.GET

interface DashboardApiService {
    @GET("/api/dashboard")
    suspend fun getDashboardData(): Response<DashboardResponse>
}

data class DashboardResponse(
    val projects: List<Project>,
    val tasks: List<Task>
)
