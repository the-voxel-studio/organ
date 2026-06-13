package fr.studio.voxel.organ.ui.project.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import fr.studio.voxel.organ.R
import fr.studio.voxel.organ.ui.components.BackToLink
import fr.studio.voxel.organ.ui.header.Header
import fr.studio.voxel.organ.ui.project.AccessDeniedScreen
import fr.studio.voxel.organ.ui.project.stats.tabs.auditActivityTab
import fr.studio.voxel.organ.ui.project.stats.tabs.statsDashboardTab
import fr.studio.voxel.organ.viewmodel.ProjectStatsViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ProjectStatsScreen(
    projectUuid: String,
    onBack: () -> Unit,
    viewModel: ProjectStatsViewModel = viewModel()
) {
    val context = LocalContext.current
    LaunchedEffect(projectUuid) {
        viewModel.initProject(projectUuid)
    }

    val highlightColor = remember(viewModel.highlightColor) {
        try {
            Color(android.graphics.Color.parseColor(viewModel.highlightColor))
        } catch (e: Exception) {
            Color(0xFFFF7EB6)
        }
    }

    val calendar = remember { Calendar.getInstance() }
    val dateFormatter = remember { SimpleDateFormat("yyyy-MM-dd", Locale.FRANCE) }
    val displayDateFormatter = remember { SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE) }

    // KPI Calculations
    val stats = viewModel.statsData
    val statsHistory = remember(stats) { stats?.history ?: emptyList() }
    val totalCreated = remember(statsHistory) { statsHistory.sumOf { it.tasksCreated } }
    val totalCompleted = remember(statsHistory) { statsHistory.sumOf { it.tasksCompleted } }
    val completionRate = remember(totalCreated, totalCompleted) {
        if (totalCreated == 0) 0 else ((totalCompleted.toFloat() / totalCreated.toFloat()) * 100).toInt()
    }
    val totalComments = remember(statsHistory) { statsHistory.sumOf { it.commentsAdded } }
    val totalAttachments = remember(statsHistory) { statsHistory.sumOf { it.attachmentsAdded } }
    val totalConsultations = remember(statsHistory) { statsHistory.sumOf { it.consultations } }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        if (viewModel.projectTitle.isEmpty() && viewModel.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = highlightColor)
            }
        } else if (viewModel.accessDenied) {
            AccessDeniedScreen(onBack = onBack)
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Header(navigateUp = onBack, canOpenSidebar = false)

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Back link & Title Info
                    item {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            BackToLink(
                                label = "Retour au Projet",
                                onClick = onBack,
                                modifier = Modifier.fillMaxWidth()
                              )

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Start,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.outline_info),
                                    contentDescription = null,
                                    tint = highlightColor,
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Statistiques - ${viewModel.projectTitle}",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                                    color = Color.Black
                                )
                            }

                            Text(
                                text = "Consultez l'activité générale et le récapitulatif des membres du projet.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                            )
                        }
                    }

                    // Tab selector (Pill style like front_angular)
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFFF1F5F9))
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val active = viewModel.activeTab
                            val isStats = active == "stats"

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isStats) Color.White else Color.Transparent)
                                    .clickable { viewModel.setTab(projectUuid, "stats") }
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Tableau de Bord",
                                    fontWeight = FontWeight.Bold,
                                    color = if (isStats) Color.Black else Color.Gray,
                                    fontSize = 14.sp
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (!isStats) Color.White else Color.Transparent)
                                    .clickable { viewModel.setTab(projectUuid, "audit") }
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Activité des Membres",
                                    fontWeight = FontWeight.Bold,
                                    color = if (!isStats) Color.Black else Color.Gray,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }

                    if (viewModel.activeTab == "stats") {
                        // --- STATS TAB ---
                        statsDashboardTab(
                            statsData = viewModel.statsData,
                            statsDays = viewModel.statsDays,
                            highlightColor = highlightColor,
                            totalCreated = totalCreated,
                            totalCompleted = totalCompleted,
                            completionRate = completionRate,
                            totalComments = totalComments,
                            totalAttachments = totalAttachments,
                            totalConsultations = totalConsultations,
                            isLoading = viewModel.isLoading,
                            onDaysSelected = { days ->
                                viewModel.setStatsDaysAndReload(projectUuid, days)
                            }
                        )
                    } else {
                        // --- AUDIT TAB ---
                        auditActivityTab(
                            auditLogs = viewModel.auditLogs,
                            auditIsLoading = viewModel.auditIsLoading,
                            auditStartDate = viewModel.auditStartDate,
                            auditEndDate = viewModel.auditEndDate,
                            auditActivityChartData = viewModel.auditActivityChartData,
                            memberStats = viewModel.memberStats,
                            highlightColor = highlightColor,
                            displayDateFormatter = displayDateFormatter,
                            dateFormatter = dateFormatter,
                            context = context,
                            calendar = calendar,
                            onDateRangeSelected = { start, end ->
                                viewModel.setDatesAndReloadAudit(projectUuid, start, end)
                            },
                            onResetFilters = {
                                viewModel.setDatesAndReloadAudit(projectUuid, null, null)
                            }
                        )
                    }
                }
            }
        }
    }
}
