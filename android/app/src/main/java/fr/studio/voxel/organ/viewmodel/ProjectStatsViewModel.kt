package fr.studio.voxel.organ.viewmodel

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.studio.voxel.organ.data.ProjectStatsRepository
import fr.studio.voxel.organ.domain.AuditLogProcessor
import fr.studio.voxel.organ.network.services.*
import kotlinx.coroutines.launch

data class MemberActivityStats(
    val uuid: String,
    val firstName: String,
    val lastName: String,
    val email: String,
    val role: String,
    var totalActions: Int = 0,
    var createdTasks: Int = 0,
    var statusChanges: Int = 0,
    var comments: Int = 0,
    var attachments: Int = 0,
    var updates: Int = 0,
    var consultations: Int = 0,
    var totalModifications: Int = 0
)

class ProjectStatsViewModel : ViewModel() {

    // Common states
    var projectTitle by mutableStateOf("")
        private set
    var highlightColor by mutableStateOf("#FF7EB6")
        private set
    var userRole by mutableStateOf("MEMBER")
        private set
    var isProjectAdmin by mutableStateOf(false)
        private set
    var isLoading by mutableStateOf(true)
    var errorMessage by mutableStateOf<String?>(null)
    var accessDenied by mutableStateOf(false)

    // Tab state
    var activeTab by mutableStateOf("stats") // "stats" or "audit"

    // Stats tab states
    var statsDays by mutableStateOf(7)
        private set
    var statsData by mutableStateOf<ProjectStatsResponse?>(null)
        private set

    // Audit tab states
    var auditLogs by mutableStateOf<List<ProjectAuditLogItem>>(emptyList())
        private set
    var auditTotalLogs by mutableStateOf(0)
        private set
    var auditIsLoading by mutableStateOf(false)
        private set
    var auditStartDate by mutableStateOf<String?>(null)
    var auditEndDate by mutableStateOf<String?>(null)

    // Aggregated stats from audit logs
    var memberStats by mutableStateOf<List<MemberActivityStats>>(emptyList())
        private set

    // Activity chart data computed on a background thread
    var auditActivityChartData by mutableStateOf<List<Pair<String, Int>>>(emptyList())
        private set

    // Coroutine Jobs to prevent concurrent runs
    private var statsJob: kotlinx.coroutines.Job? = null
    private var auditJob: kotlinx.coroutines.Job? = null

    // Project members for mapping
    private var projectMembers: List<ProjectDetailedMember> = emptyList()

    fun initProject(projectUuid: String) {
        statsJob?.cancel()
        auditJob?.cancel()
        statsJob = viewModelScope.launch {
            isLoading = true
            errorMessage = null
            accessDenied = false

            try {
                val detailedRes = ProjectStatsRepository.getDetailedProject(projectUuid)
                if (detailedRes.isSuccessful) {
                    val detailedData = detailedRes.body()
                    if (detailedData != null) {
                        projectTitle = detailedData.project.title
                        highlightColor = detailedData.project.color ?: "#FF7EB6"
                        userRole = detailedData.project.role ?: "MEMBER"
                        isProjectAdmin = userRole == "ADMIN" || userRole == "MANAGER"
                        projectMembers = detailedData.members

                        if (!isProjectAdmin) {
                            accessDenied = true
                            isLoading = false
                            return@launch
                        }

                        // Load initial stats
                        loadStats(projectUuid, statsDays)
                    } else {
                        errorMessage = "Données du projet introuvables."
                    }
                } else {
                    if (detailedRes.code() == 403) {
                        accessDenied = true
                    } else {
                        errorMessage = "Projet non trouvé ou accès refusé."
                    }
                }
            } catch (e: Exception) {
                Log.e("PROJECT_STATS_VM", "Error initializing stats", e)
                errorMessage = "Une erreur réseau est survenue."
            } finally {
                isLoading = false
            }
        }
    }

    fun setStatsDaysAndReload(projectUuid: String, days: Int) {
        statsDays = days
        statsJob?.cancel()
        statsJob = viewModelScope.launch {
            isLoading = true
            try {
                loadStats(projectUuid, days)
            } catch (e: Exception) {
                Log.e("PROJECT_STATS_VM", "Error reloading stats", e)
            } finally {
                isLoading = false
            }
        }
    }

    private suspend fun loadStats(projectUuid: String, days: Int) {
        try {
            val statsRes = ProjectStatsRepository.getProjectStats(projectUuid, days)
            if (statsRes.isSuccessful) {
                statsData = statsRes.body()
            } else {
                Log.e("PROJECT_STATS_VM", "Stats request failed: ${statsRes.message()}")
            }
        } catch (e: Exception) {
            Log.e("PROJECT_STATS_VM", "Network error loading stats", e)
        }
    }

    fun setTab(projectUuid: String, tab: String) {
        activeTab = tab
        if (tab == "audit" && auditLogs.isEmpty() && !auditIsLoading) {
            loadAuditLogs(projectUuid)
        }
    }

    fun setDatesAndReloadAudit(projectUuid: String, start: String?, end: String?) {
        auditStartDate = start
        auditEndDate = end
        loadAuditLogs(projectUuid)
    }

    fun loadAuditLogs(projectUuid: String) {
        auditJob?.cancel()
        auditJob = viewModelScope.launch {
            auditIsLoading = true
            auditLogs = emptyList()
            memberStats = emptyList()
            auditActivityChartData = emptyList()
            try {
                // Fetch in background (IO dispatcher)
                val allLogs = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    ProjectStatsRepository.fetchAllAuditLogs(
                        projectUuid = projectUuid,
                        startDate = auditStartDate,
                        endDate = auditEndDate
                    )
                }

                // Process statistics on background thread (Default dispatcher)
                val processedData = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
                    val stats = AuditLogProcessor.calculateMemberStats(allLogs, projectMembers)
                    val chartData = AuditLogProcessor.calculateActivityChartData(allLogs)
                    Pair(stats, chartData)
                }

                // Update states once on the Main thread
                auditLogs = allLogs
                auditTotalLogs = allLogs.size
                memberStats = processedData.first
                auditActivityChartData = processedData.second
            } catch (e: Exception) {
                Log.e("PROJECT_STATS_VM", "Error loading audit logs", e)
            } finally {
                auditIsLoading = false
            }
        }
    }
}
