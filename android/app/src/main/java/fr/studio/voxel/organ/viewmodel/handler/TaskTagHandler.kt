package fr.studio.voxel.organ.viewmodel.handler

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import fr.studio.voxel.organ.data.TagRepository
import fr.studio.voxel.organ.data.TaskRepository
import fr.studio.voxel.organ.network.services.TagResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class TaskTagHandler(
    private val scope: CoroutineScope
) {
    var allProjectTags by mutableStateOf<List<TagResponse>>(emptyList())
        internal set

    fun loadTags(pUuid: String) {
        scope.launch {
            TagRepository.getTags(pUuid)
                .onSuccess { list ->
                    allProjectTags = list
                }.onFailure { e ->
                    Log.e("TASK_TAG_HANDLER", "loadTags error", e)
                }
        }
    }

    fun addTag(
        pUuid: String,
        oUuid: String,
        tUuid: String,
        tagUuid: String,
        onComplete: () -> Unit
    ) {
        scope.launch {
            TaskRepository.addTaskTag(pUuid, oUuid, tUuid, tagUuid)
                .onSuccess {
                    onComplete()
                }.onFailure { e ->
                    Log.e("TASK_TAG_HANDLER", "addTag error", e)
                }
        }
    }

    fun removeTag(
        pUuid: String,
        oUuid: String,
        tUuid: String,
        tagUuid: String,
        onComplete: () -> Unit
    ) {
        scope.launch {
            TaskRepository.removeTaskTag(pUuid, oUuid, tUuid, tagUuid)
                .onSuccess {
                    onComplete()
                }.onFailure { e ->
                    Log.e("TASK_TAG_HANDLER", "removeTag error", e)
                }
        }
    }
}
