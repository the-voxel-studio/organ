package fr.studio.voxel.organ.viewmodel.handler

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import fr.studio.voxel.organ.network.services.AddTagRequest
import fr.studio.voxel.organ.network.services.TagApiService
import fr.studio.voxel.organ.network.services.TagResponse
import fr.studio.voxel.organ.network.services.TaskApiService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class TaskTagHandler(
    private val taskService: TaskApiService,
    private val tagService: TagApiService,
    private val scope: CoroutineScope
) {
    var allProjectTags by mutableStateOf<List<TagResponse>>(emptyList())
        internal set

    fun loadTags(pUuid: String) {
        scope.launch {
            try {
                val tagsRes = tagService.getTags(pUuid)
                if (tagsRes.isSuccessful) {
                    allProjectTags = tagsRes.body() ?: emptyList()
                }
            } catch (e: Exception) {
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
            try {
                val response = taskService.addTaskTag(pUuid, oUuid, tUuid, AddTagRequest(tagUuid))
                if (response.isSuccessful) {
                    onComplete()
                }
            } catch (e: Exception) {
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
            try {
                val response = taskService.removeTaskTag(pUuid, oUuid, tUuid, tagUuid)
                if (response.isSuccessful) {
                    onComplete()
                }
            } catch (e: Exception) {
                Log.e("TASK_TAG_HANDLER", "removeTag error", e)
            }
        }
    }
}
