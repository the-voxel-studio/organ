package fr.studio.voxel.organ.viewmodel.handler

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import fr.studio.voxel.organ.data.TaskRepository
import fr.studio.voxel.organ.network.services.TaskDependencyResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class TaskDependencyHandler(
    private val scope: CoroutineScope
) {
    var dependencies by mutableStateOf<List<TaskDependencyResponse>>(emptyList())
        internal set

    fun loadDependencies(pUuid: String, oUuid: String, tUuid: String) {
        scope.launch {
            TaskRepository.getDependencies(pUuid, oUuid, tUuid)
                .onSuccess { list ->
                    dependencies = list
                }.onFailure { e ->
                    Log.e("TASK_DEPENDENCY_HANDLER", "loadDependencies error", e)
                }
        }
    }

    fun addDependency(pUuid: String, oUuid: String, tUuid: String, dependsOnUuid: String, onTimelineUpdate: () -> Unit) {
        scope.launch {
            TaskRepository.addDependency(pUuid, oUuid, tUuid, dependsOnUuid)
                .onSuccess {
                    loadDependencies(pUuid, oUuid, tUuid)
                    onTimelineUpdate()
                }.onFailure { e ->
                    Log.e("TASK_DEPENDENCY_HANDLER", "addDependency error", e)
                }
        }
    }

    fun removeDependency(pUuid: String, oUuid: String, tUuid: String, dependsOnUuid: String, permanent: Boolean, onTimelineUpdate: () -> Unit) {
        scope.launch {
            TaskRepository.removeDependency(pUuid, oUuid, tUuid, dependsOnUuid, permanent)
                .onSuccess {
                    loadDependencies(pUuid, oUuid, tUuid)
                    onTimelineUpdate()
                }.onFailure { e ->
                    Log.e("TASK_DEPENDENCY_HANDLER", "removeDependency error", e)
                }
        }
    }
}
