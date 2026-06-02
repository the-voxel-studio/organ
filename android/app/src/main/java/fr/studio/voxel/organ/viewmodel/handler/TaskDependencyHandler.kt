package fr.studio.voxel.organ.viewmodel.handler

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import fr.studio.voxel.organ.network.services.AddDependencyRequest
import fr.studio.voxel.organ.network.services.TaskApiService
import fr.studio.voxel.organ.network.services.TaskDependencyResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class TaskDependencyHandler(
    private val taskService: TaskApiService,
    private val scope: CoroutineScope
) {
    var dependencies by mutableStateOf<List<TaskDependencyResponse>>(emptyList())
        internal set

    fun loadDependencies(pUuid: String, oUuid: String, tUuid: String) {
        scope.launch {
            try {
                val res = taskService.getDependencies(pUuid, oUuid, tUuid)
                if (res.isSuccessful) {
                    dependencies = res.body() ?: emptyList()
                }
            } catch (e: Exception) {
                Log.e("TASK_DEPENDENCY_HANDLER", "loadDependencies error", e)
            }
        }
    }

    fun addDependency(pUuid: String, oUuid: String, tUuid: String, dependsOnUuid: String, onTimelineUpdate: () -> Unit) {
        scope.launch {
            try {
                val response = taskService.addDependency(pUuid, oUuid, tUuid, AddDependencyRequest(dependsOnUuid))
                if (response.isSuccessful) {
                    loadDependencies(pUuid, oUuid, tUuid)
                    onTimelineUpdate()
                }
            } catch (e: Exception) {
                Log.e("TASK_DEPENDENCY_HANDLER", "addDependency error", e)
            }
        }
    }

    fun removeDependency(pUuid: String, oUuid: String, tUuid: String, dependsOnUuid: String, permanent: Boolean, onTimelineUpdate: () -> Unit) {
        scope.launch {
            try {
                val response = taskService.removeDependency(pUuid, oUuid, tUuid, dependsOnUuid, permanent)
                if (response.isSuccessful) {
                    loadDependencies(pUuid, oUuid, tUuid)
                    onTimelineUpdate()
                }
            } catch (e: Exception) {
                Log.e("TASK_DEPENDENCY_HANDLER", "removeDependency error", e)
            }
        }
    }
}
