package fr.studio.voxel.organ.domain

import fr.studio.voxel.organ.network.services.ProjectAuditLogItem
import fr.studio.voxel.organ.network.services.ProjectDetailedMember
import fr.studio.voxel.organ.viewmodel.MemberActivityStats
import java.util.TreeMap

object AuditLogProcessor {

    fun calculateMemberStats(
        logs: List<ProjectAuditLogItem>,
        projectMembers: List<ProjectDetailedMember>
    ): List<MemberActivityStats> {
        val statsMap = mutableMapOf<String, MemberActivityStats>()

        // Initialize map with current project members
        for (member in projectMembers) {
            val user = member.user
            statsMap[user.uuid] = MemberActivityStats(
                uuid = user.uuid,
                firstName = user.firstName,
                lastName = user.lastName,
                email = user.email,
                role = member.globalRole
            )
        }

        // Aggregate from audit logs
        for (log in logs) {
            val user = log.user ?: continue
            val userUuid = user.uuid

            if (!statsMap.containsKey(userUuid)) {
                statsMap[userUuid] = MemberActivityStats(
                    uuid = userUuid,
                    firstName = user.firstName,
                    lastName = user.lastName,
                    email = user.email,
                    role = "MEMBRE"
                )
            }

            val stat = statsMap[userUuid]!!
            stat.totalActions++

            when (log.actionType) {
                "CREATE" -> {
                    stat.createdTasks++
                    stat.totalModifications++
                }
                "STATUS_CHANGE" -> {
                    stat.statusChanges++
                    stat.totalModifications++
                }
                "COMMENT_ADD" -> {
                    stat.comments++
                    stat.totalModifications++
                }
                "ATTACHMENT_ADD" -> {
                    stat.attachments++
                    stat.totalModifications++
                }
                "UPDATE", "ASSIGNEE_ADD", "ASSIGNEE_REMOVE" -> {
                    stat.updates++
                    stat.totalModifications++
                }
                "CONSULTATION" -> {
                    stat.consultations++
                }
            }
        }

        // Sort by total actions desc
        return statsMap.values.sortedByDescending { it.totalActions }
    }

    fun calculateActivityChartData(logs: List<ProjectAuditLogItem>): List<Pair<String, Int>> {
        val map = TreeMap<String, Int>()
        for (log in logs) {
            val dayStr = log.createdAt.take(10)
            map[dayStr] = (map[dayStr] ?: 0) + 1
        }
        return map.toList()
    }
}
