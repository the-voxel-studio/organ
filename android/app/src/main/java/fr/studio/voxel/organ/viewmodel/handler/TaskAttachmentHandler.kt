package fr.studio.voxel.organ.viewmodel.handler

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import fr.studio.voxel.organ.data.TaskRepository
import fr.studio.voxel.organ.network.services.TaskAttachmentResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

class TaskAttachmentHandler(
    private val scope: CoroutineScope
) {
    var attachments by mutableStateOf<List<TaskAttachmentResponse>>(emptyList())
        internal set
    var trashedAttachments by mutableStateOf<List<TaskAttachmentResponse>>(emptyList())
        internal set

    fun loadAttachments(pUuid: String, oUuid: String, tUuid: String) {
        scope.launch {
            TaskRepository.getAttachments(pUuid, oUuid, tUuid)
                .onSuccess { list ->
                    attachments = list
                }.onFailure { e ->
                    Log.e("TASK_ATTACHMENT_HANDLER", "loadAttachments error", e)
                }
        }
    }

    fun loadTrashData(pUuid: String, oUuid: String, tUuid: String) {
        scope.launch {
            TaskRepository.getTrashedAttachments(pUuid, oUuid, tUuid)
                .onSuccess { list ->
                    trashedAttachments = list
                }.onFailure { e ->
                    Log.e("TASK_ATTACHMENT_HANDLER", "loadTrashData error", e)
                }
        }
    }

    fun uploadAttachment(
        pUuid: String,
        oUuid: String,
        tUuid: String,
        fileName: String,
        mimeType: String,
        fileBytes: ByteArray,
        onTimelineUpdate: () -> Unit
    ) {
        scope.launch {
            try {
                val requestBody = fileBytes.toRequestBody(mimeType.toMediaTypeOrNull())
                val body = MultipartBody.Part.createFormData("file", fileName, requestBody)
                TaskRepository.uploadAttachment(pUuid, oUuid, tUuid, body)
                    .onSuccess {
                        loadAttachments(pUuid, oUuid, tUuid)
                        onTimelineUpdate()
                    }.onFailure { e ->
                        Log.e("TASK_ATTACHMENT_HANDLER", "uploadAttachment error", e)
                    }
            } catch (e: Exception) {
                Log.e("TASK_ATTACHMENT_HANDLER", "uploadAttachment error", e)
            }
        }
    }

    fun deleteAttachment(
        pUuid: String,
        oUuid: String,
        tUuid: String,
        attachmentUuid: String,
        permanent: Boolean,
        isTrashOpen: Boolean,
        onTimelineUpdate: () -> Unit
    ) {
        scope.launch {
            TaskRepository.deleteAttachment(pUuid, oUuid, tUuid, attachmentUuid, permanent)
                .onSuccess {
                    loadAttachments(pUuid, oUuid, tUuid)
                    onTimelineUpdate()
                    if (isTrashOpen) {
                        loadTrashData(pUuid, oUuid, tUuid)
                    }
                }.onFailure { e ->
                    Log.e("TASK_ATTACHMENT_HANDLER", "deleteAttachment error", e)
                }
        }
    }

    fun restoreAttachment(
        pUuid: String,
        oUuid: String,
        tUuid: String,
        attachmentUuid: String,
        onTimelineUpdate: () -> Unit
    ) {
        scope.launch {
            TaskRepository.restoreAttachment(pUuid, oUuid, tUuid, attachmentUuid)
                .onSuccess {
                    loadAttachments(pUuid, oUuid, tUuid)
                    onTimelineUpdate()
                    loadTrashData(pUuid, oUuid, tUuid)
                }.onFailure { e ->
                    Log.e("TASK_ATTACHMENT_HANDLER", "restoreAttachment error", e)
                }
        }
    }
}
