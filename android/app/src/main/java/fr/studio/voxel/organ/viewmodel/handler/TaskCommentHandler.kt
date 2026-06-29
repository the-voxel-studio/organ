package fr.studio.voxel.organ.viewmodel.handler

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import fr.studio.voxel.organ.data.TaskRepository
import fr.studio.voxel.organ.network.services.TaskCommentResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class TaskCommentHandler(
    private val scope: CoroutineScope
) {
    var comments by mutableStateOf<List<TaskCommentResponse>>(emptyList())
        internal set
    var trashedComments by mutableStateOf<List<TaskCommentResponse>>(emptyList())
        internal set

    fun loadComments(pUuid: String, oUuid: String, tUuid: String) {
        scope.launch {
            TaskRepository.getComments(pUuid, oUuid, tUuid)
                .onSuccess { list ->
                    comments = list
                }.onFailure { e ->
                    Log.e("TASK_COMMENT_HANDLER", "loadComments error", e)
                }
        }
    }

    fun loadTrashData(pUuid: String, oUuid: String, tUuid: String) {
        scope.launch {
            TaskRepository.getTrashedComments(pUuid, oUuid, tUuid)
                .onSuccess { list ->
                    trashedComments = list
                }.onFailure { e ->
                    Log.e("TASK_COMMENT_HANDLER", "loadTrashData error", e)
                }
        }
    }

    fun addComment(pUuid: String, oUuid: String, tUuid: String, content: String, onTimelineUpdate: () -> Unit) {
        if (content.isBlank()) return
        scope.launch {
            TaskRepository.createComment(pUuid, oUuid, tUuid, content)
                .onSuccess {
                    loadComments(pUuid, oUuid, tUuid)
                    onTimelineUpdate()
                }.onFailure { e ->
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
            TaskRepository.deleteComment(pUuid, oUuid, tUuid, commentUuid, permanent)
                .onSuccess {
                    loadComments(pUuid, oUuid, tUuid)
                    onTimelineUpdate()
                    if (isTrashOpen) {
                        loadTrashData(pUuid, oUuid, tUuid)
                    }
                }.onFailure { e ->
                    Log.e("TASK_COMMENT_HANDLER", "deleteComment error", e)
                }
        }
    }

    fun restoreComment(pUuid: String, oUuid: String, tUuid: String, commentUuid: String, onTimelineUpdate: () -> Unit) {
        scope.launch {
            TaskRepository.restoreComment(pUuid, oUuid, tUuid, commentUuid)
                .onSuccess {
                    loadComments(pUuid, oUuid, tUuid)
                    onTimelineUpdate()
                    loadTrashData(pUuid, oUuid, tUuid)
                }.onFailure { e ->
                    Log.e("TASK_COMMENT_HANDLER", "restoreComment error", e)
                }
        }
    }
}
