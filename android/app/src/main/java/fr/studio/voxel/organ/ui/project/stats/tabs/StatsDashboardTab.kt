package fr.studio.voxel.organ.ui.project.stats.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.studio.voxel.organ.R
import fr.studio.voxel.organ.network.services.ProjectStatsResponse
import fr.studio.voxel.organ.ui.components.EmptyTrashState
import fr.studio.voxel.organ.ui.components.ShimmerBox
import fr.studio.voxel.organ.ui.project.stats.components.*

@OptIn(ExperimentalLayoutApi::class)
fun LazyListScope.statsDashboardTab(
    statsData: ProjectStatsResponse?,
    statsDays: Int,
    highlightColor: Color,
    totalCreated: Int,
    totalCompleted: Int,
    completionRate: Int,
    totalComments: Int,
    totalAttachments: Int,
    totalConsultations: Int,
    isLoading: Boolean,
    onDaysSelected: (Int) -> Unit
) {
    // Days Filter Row
    item {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Période :",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                color = Color.Black
            )
            listOf(7 to "7 jours", 30 to "30 jours", 90 to "90 jours").forEach { (days, label) ->
                val isSelected = statsDays == days
                val btnBg = if (isSelected) highlightColor else Color(0xFFF1F5F9)
                val btnTextColor = if (isSelected) Color.White else Color.Gray

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(btnBg)
                        .clickable { onDaysSelected(days) }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = label,
                        fontSize = 12.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        color = btnTextColor
                    )
                }
            }
        }
    }

    if (isLoading) {
        // KPI Shimmer Cards Grid (2 columns)
        item {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                maxItemsInEachRow = 2,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val kpiModifier = Modifier
                    .weight(1f)
                    .height(72.dp)
                repeat(6) {
                    ShimmerBox(
                        modifier = kpiModifier,
                        shape = RoundedCornerShape(16.dp)
                    )
                }
            }
        }

        // Shimmer Chart Cards
        repeat(3) { index ->
            val title = when (index) {
                0 -> "Activité sur la Période"
                1 -> "Tâches et Estimations par Organ"
                else -> "Répartition par Statut"
            }
            item {
                ChartCard(title = title) {
                    ShimmerBox(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        }
    } else if (statsData == null || statsData.history.isEmpty()) {
        item {
            EmptyTrashState(
                title = "Aucune donnée disponible",
                description = "Essayez d'ajuster l'amplitude de date ou de collaborer sur le projet pour voir apparaître les statistiques."
            )
        }
    } else {
        // KPI Cards Grid (2 columns)
        item {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                maxItemsInEachRow = 2,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val kpiModifier = Modifier
                    .weight(1f)
                    .minimumInteractiveComponentSize()
                KpiCard(title = "Créées", value = totalCreated.toString(), iconRes = R.drawable.menu_tache, color = Color(0xFF3B82F6), modifier = kpiModifier)
                KpiCard(title = "Terminées", value = totalCompleted.toString(), iconRes = R.drawable.menu_tache, color = Color(0xFF10B981), modifier = kpiModifier)
                KpiCard(title = "Complétion", value = "$completionRate%", iconRes = R.drawable.outline_info, color = highlightColor, modifier = kpiModifier)
                KpiCard(title = "Commentaires", value = totalComments.toString(), iconRes = R.drawable.projet_folder, color = Color(0xFFF59E0B), modifier = kpiModifier)
                KpiCard(title = "Fichiers joints", value = totalAttachments.toString(), iconRes = R.drawable.projet_folder, color = Color(0xFFEC4899), modifier = kpiModifier)
                KpiCard(title = "Consultations", value = totalConsultations.toString(), iconRes = R.drawable.icon_account, color = Color(0xFF6366F1), modifier = kpiModifier)
            }
        }

        // Line Chart: Activity Temporal
        item {
            ChartCard(title = "Activité sur la Période") {
                ActivityLineChart(
                    history = statsData.history,
                    highlightColor = highlightColor,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                )
            }
        }

        // Bar Chart: Organ distribution
        item {
            ChartCard(title = "Tâches et Estimations par Organ") {
                OrgansBarChart(
                    current = statsData.current,
                    highlightColor = highlightColor,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                )
            }
        }

        // Doughnut Chart: Status distribution
        item {
            ChartCard(title = "Répartition par Statut") {
                TaskStatusDoughnutChart(
                    current = statsData.current,
                    highlightColor = highlightColor,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                )
            }
        }
    }
}
