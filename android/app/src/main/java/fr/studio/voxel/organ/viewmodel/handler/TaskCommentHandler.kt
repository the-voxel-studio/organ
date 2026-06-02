package fr.studio.voxel.organ.viewmodel.handler

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import fr.studio.voxel.organ.network.services.CreateCommentRequest
import fr.studio.voxel.organ.network.services.TaskApiService
import fr.studio.voxel.organ.network.services.TaskCommentResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class TaskCommentHandler(
    private val taskService: TaskApiService,
    private val scope: CoroutineScope
) {
    var comments by mutableStateOf<List<TaskCommentResponse>>(emptyList())
        internal set
    var trashedComments by mutableStateOf<List<TaskCommentResponse>>(emptyList())
        internal set

    fun loadComments(pUuid: String, oUuid: String, tUuid: String) {
        scope.launch {
            try {
                val res = taskService.getComments(pUuid, oUuid, tUuid)
                if (res.isSuccessful) {
                    comments = res.body() ?: emptyList()
                }
            } catch (e: Exception) {
                Log.e("TASK_COMMENT_HANDLER", "loadComments error", e)
            }
        }
    }

    fun loadTrashData(pUuid: String, oUuid: String, tUuid: String) {
        scope.launch {
            try {
                val cRes = taskService.getTrashedComments(pUuid, oUuid, tUuid)
                if (cRes.isSuccessful) {
                    trashedComments = cRes.body() ?: emptyList()
                }
            } catch (e: Exception) {
                Log.e("TASK_COMMENT_HANDLER", "loadTrashData error", e)
            }
        }
    }

    fun addComment(pUuid: String, oUuid: String, tUuid: String, content: String, onTimelineUpdate: () -> Unit) {
        if (content.isBlank()) return
        scope.launch {
            try {
                val response = taskService.createComment(pUuid, oUuid, tUuid, CreateCommentRequest(content))
                if (response.isSuccessful) {
                    loadComments(pUuid, oUuid, tUuid)
                    onTimelineUpdate()
                }
            } catch (e: Exception) {
                Log.e("TASK_COMMENT_HANDLER", "addComment error", e)
            }
        }
    }

    fun deleteComment(
        pUuid: String,
        oUuid: String,
        tUuid: String,
        commentUuid: String,
        permanent: Boolean,
        isTrashOpen: Boolean,
        onTimelineUpdate: () -> Unit
    ) {
        scope.launch {
            try {
                val response = taskService.deleteComment(pUuid, oUuid, tUuid, commentUuid, permanent)
                if (response.isSuccessful) {
                    loadComments(pUuid, oUuid, tUuid)
                    onTimelineUpdate()
                    if (isTrashOpen) {
                        loadTrashData(pUuid, oUuid, tUuid)
                    }
                }
            } catch (e: Exception) {
                Log.e("TASK_COMMENT_HANDLER", "deleteComment error", e)
            }
        }
    }

    fun restoreComment(pUuid: String, oUuid: String, tUuid: String, commentUuid: String, onTimelineUpdate: () -> Unit) {
        scope.launch {
            try {
                val response = taskService.restoreComment(pUuid, oUuid, tUuid, commentUuid)
                if (response.isSuccessful) {
                    loadComments(pUuid, oUuid, tUuid)
                    onTimelineUpdate()
                    loadTrashData(pUuid, oUuid, tUuid)
                }
            } catch (e: Exception) {
                Log.e("TASK_COMMENT_HANDLER", "restoreComment error", e)
            }
        }
    }
}
