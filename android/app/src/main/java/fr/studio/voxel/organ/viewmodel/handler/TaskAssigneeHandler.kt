package fr.studio.voxel.organ.viewmodel.handler

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import fr.studio.voxel.organ.network.services.AssigneeRequest
import fr.studio.voxel.organ.network.services.TaskApiService
import fr.studio.voxel.organ.network.services.User
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class TaskAssigneeHandler(
    private val taskService: TaskApiService,
    private val scope: CoroutineScope
) {
    var assignees by mutableStateOf<List<User>>(emptyList())
        internal set

    fun addAssignee(
        pUuid: String,
        oUuid: String,
        tUuid: String,
        userUuid: String,
        onComplete: () -> Unit
    ) {
        scope.launch {
            try {
                val response = taskService.addAssignee(pUuid, oUuid, tUuid, AssigneeRequest(userUuid))
                if (response.isSuccessful) {
                    onComplete()
                }
            } catch (e: Exception) {
                Log.e("TASK_ASSIGNEE_HANDLER", "addAssignee error", e)
            }
        }
    }

    fun removeAssignee(
        pUuid: String,
        oUuid: String,
        tUuid: String,
        userUuid: String,
        onComplete: () -> Unit
    ) {
        scope.launch {
            try {
                val response = taskService.removeAssignee(pUuid, oUuid, tUuid, userUuid)
                if (response.isSuccessful) {
                    onComplete()
                }
            } catch (e: Exception) {
                Log.e("TASK_ASSIGNEE_HANDLER", "removeAssignee error", e)
            }
        }
    }
}
