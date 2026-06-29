package fr.studio.voxel.organ.ui.project.stats.tabs

import android.app.DatePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.studio.voxel.organ.network.services.ProjectAuditLogItem
import fr.studio.voxel.organ.ui.components.EmptyTrashState
import fr.studio.voxel.organ.ui.components.ShimmerBox
import fr.studio.voxel.organ.ui.project.stats.components.AuditPicActivityChart
import fr.studio.voxel.organ.ui.project.stats.components.ChartCard
import fr.studio.voxel.organ.ui.project.stats.components.MemberActivityStatsCard
import fr.studio.voxel.organ.viewmodel.MemberActivityStats
import java.text.SimpleDateFormat
import java.util.Calendar

fun LazyListScope.auditActivityTab(
    auditLogs: List<ProjectAuditLogItem>,
    auditIsLoading: Boolean,
    auditStartDate: String?,
    auditEndDate: String?,
    auditActivityChartData: List<Pair<String, Int>>,
    memberStats: List<MemberActivityStats>,
    highlightColor: Color,
    displayDateFormatter: SimpleDateFormat,
    dateFormatter: SimpleDateFormat,
    context: android.content.Context,
    calendar: Calendar,
    onDateRangeSelected: (String?, String?) -> Unit,
    onResetFilters: () -> Unit
) {
    // Date Pickers Row
    item {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFFF8FAFC))
                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Text(
                text = "Filtrer la période d'activité",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.Black
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Start Date Button
                val startLabel = if (auditStartDate != null) {
                    try {
                        displayDateFormatter.format(dateFormatter.parse(auditStartDate)!!)
                    } catch (e: Exception) {
                        auditStartDate
                    }
                } else "Début"

                Button(
                    onClick = {
                        val currentVal = auditStartDate
                        if (currentVal != null) {
                            try {
                                calendar.time = dateFormatter.parse(currentVal)!!
                            } catch (e: Exception) {}
                        }
                        DatePickerDialog(
                            context,
                            { _, y, m, d ->
                                calendar.set(y, m, d)
                                val newDate = dateFormatter.format(calendar.time)
                                onDateRangeSelected(newDate, auditEndDate)
                            },
                            calendar.get(Calendar.YEAR),
                            calendar.get(Calendar.MONTH),
                            calendar.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                    border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f).height(44.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.DateRange, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                        Text(startLabel, fontSize = 12.sp, overflow = TextOverflow.Ellipsis, maxLines = 1)
                    }
                }

                Text("-", color = Color.Gray)

                // End Date Button
                val endLabel = if (auditEndDate != null) {
                    try {
                        displayDateFormatter.format(dateFormatter.parse(auditEndDate)!!)
                    } catch (e: Exception) {
                        auditEndDate
                    }
                } else "Fin"

                Button(
                    onClick = {
                        val currentVal = auditEndDate
                        if (currentVal != null) {
                            try {
                                calendar.time = dateFormatter.parse(currentVal)!!
                            } catch (e: Exception) {}
                        }
                        DatePickerDialog(
                            context,
                            { _, y, m, d ->
                                calendar.set(y, m, d)
                                val newDate = dateFormatter.format(calendar.time)
                                onDateRangeSelected(auditStartDate, newDate)
                            },
                            calendar.get(Calendar.YEAR),
                            calendar.get(Calendar.MONTH),
                            calendar.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                    border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f).height(44.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.DateRange, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                        Text(endLabel, fontSize = 12.sp, overflow = TextOverflow.Ellipsis, maxLines = 1)
                    }
                }
            }

            if (auditStartDate != null || auditEndDate != null) {
                Spacer(modifier = Modifier.height(12.dp))
                TextButton(
                    onClick = onResetFilters,
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Red),
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Réinitialiser les filtres", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (auditIsLoading) {
        item {
            ChartCard(title = "Pic d'Activité (Journal d'Audit)") {
                ShimmerBox(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }

        item {
            Text(
                text = "Activité des Membres (Chargement...)",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.Black,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            )
        }

        repeat(3) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        ShimmerBox(modifier = Modifier.size(40.dp), shape = androidx.compose.foundation.shape.CircleShape)
                        Column(modifier = Modifier.weight(1f)) {
                            ShimmerBox(modifier = Modifier.width(120.dp).height(20.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            ShimmerBox(modifier = Modifier.width(180.dp).height(14.dp))
                        }
                        ShimmerBox(modifier = Modifier.width(60.dp).height(24.dp), shape = RoundedCornerShape(8.dp))
                    }
                }
            }
        }
    } else {
        if (auditLogs.isNotEmpty()) {
            item {
                ChartCard(title = "Pic d'Activité (Journal d'Audit)") {
                    AuditPicActivityChart(
                        activityData = auditActivityChartData,
                        highlightColor = highlightColor,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                    )
                }
            }

            item {
                Text(
                    text = "Activité des Membres (${memberStats.size} collaborateurs)",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.Black,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                )
            }

            items(memberStats) { memberStat ->
                MemberActivityStatsCard(
                    stat = memberStat,
                    highlightColor = highlightColor
                )
            }
        } else {
            item {
                EmptyTrashState(
                    title = "Aucune activité",
                    description = "Aucune action n'a été enregistrée pour la plage de dates sélectionnée."
                )
            }
        }
    }
}
