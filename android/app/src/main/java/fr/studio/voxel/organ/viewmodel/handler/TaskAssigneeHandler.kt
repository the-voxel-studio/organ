package fr.studio.voxel.organ.viewmodel.handler

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import fr.studio.voxel.organ.data.TaskRepository
import fr.studio.voxel.organ.network.services.User
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class TaskAssigneeHandler(
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
            TaskRepository.addAssignee(pUuid, oUuid, tUuid, userUuid)
                .onSuccess {
                    onComplete()
                }.onFailure { e ->
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
            TaskRepository.removeAssignee(pUuid, oUuid, tUuid, userUuid)
                .onSuccess {
                    onComplete()
                }.onFailure { e ->
                    Log.e("TASK_ASSIGNEE_HANDLER", "removeAssignee error", e)
                }
        }
    }
}
