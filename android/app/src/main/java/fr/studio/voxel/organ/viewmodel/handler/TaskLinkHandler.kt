package fr.studio.voxel.organ.viewmodel.handler

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import fr.studio.voxel.organ.network.services.AddLinkRequest
import fr.studio.voxel.organ.network.services.TaskApiService
import fr.studio.voxel.organ.network.services.TaskLinkSummary
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class TaskLinkHandler(
    private val taskService: TaskApiService,
    private val scope: CoroutineScope
) {
    var links by mutableStateOf<List<TaskLinkSummary>>(emptyList())
        internal set

    fun addLink(
        pUuid: String,
        oUuid: String,
        tUuid: String,
        url: String,
        description: String?,
        onComplete: () -> Unit
    ) {
        if (url.isBlank()) return
        scope.launch {
            try {
                val response = taskService.createLink(pUuid, oUuid, tUuid, AddLinkRequest(url, description))
                if (response.isSuccessful) {
                    onComplete()
                }
            } catch (e: Exception) {
                Log.e("TASK_LINK_HANDLER", "addLink error", e)
            }
        }
    }

    fun deleteLink(
        pUuid: String,
        oUuid: String,
        tUuid: String,
        linkUuid: String,
        onComplete: () -> Unit
    ) {
        scope.launch {
            try {
                val response = taskService.deleteLink(pUuid, oUuid, tUuid, linkUuid)
                if (response.isSuccessful) {
                    onComplete()
                }
            } catch (e: Exception) {
                Log.e("TASK_LINK_HANDLER", "deleteLink error", e)
            }
        }
    }
}
