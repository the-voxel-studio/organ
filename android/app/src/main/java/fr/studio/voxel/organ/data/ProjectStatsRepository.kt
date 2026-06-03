package fr.studio.voxel.organ.data

import android.util.Log
import fr.studio.voxel.organ.network.ApiClient
import fr.studio.voxel.organ.network.services.*
import retrofit2.Response

object ProjectStatsRepository {
    private val projectService = ApiClient.createService(ProjectApiService::class.java)

    suspend fun getDetailedProject(projectUuid: String): Response<ProjectDetailedViewResponse> {
        return projectService.getDetailedProject(projectUuid)
    }

    suspend fun getProjectStats(projectUuid: String, days: Int): Response<ProjectStatsResponse> {
        return projectService.getProjectStats(projectUuid, days)
    }

    suspend fun fetchAllAuditLogs(
        projectUuid: String,
        startDate: String?,
        endDate: String?,
        maxLogs: Int = 1000
    ): List<ProjectAuditLogItem> {
        val allLogs = mutableListOf<ProjectAuditLogItem>()
        fetchLogsRecursively(
            projectUuid = projectUuid,
            offset = 0,
            startDate = startDate,
            endDate = endDate,
            accumulator = allLogs,
            maxLogs = maxLogs
        )
        return allLogs
    }

    private suspend fun fetchLogsRecursively(
        projectUuid: String,
        offset: Int,
        startDate: String?,
        endDate: String?,
        accumulator: MutableList<ProjectAuditLogItem>,
        maxLogs: Int
    ) {
        val limit = 150
        try {
            val res = projectService.getProjectAuditLogs(
                uuid = projectUuid,
                limit = limit,
                offset = offset,
                startDate = startDate,
                endDate = endDate
            )
            if (res.isSuccessful) {
                val responseData = res.body()
                if (responseData != null && responseData.logs.isNotEmpty()) {
                    accumulator.addAll(responseData.logs)

                    val nextOffset = offset + limit
                    if (nextOffset < responseData.total && accumulator.size < maxLogs) {
                        fetchLogsRecursively(
                            projectUuid = projectUuid,
                            offset = nextOffset,
                            startDate = startDate,
                            endDate = endDate,
                            accumulator = accumulator,
                            maxLogs = maxLogs
                        )
                    }
                }
            } else {
                Log.e("ProjectStatsRepository", "Audit logs request failed: ${res.message()}")
            }
        } catch (e: Exception) {
            Log.e("ProjectStatsRepository", "Error fetching audit logs recursively", e)
        }
    }
}
