package fr.studio.voxel.organ.viewmodel.handler

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import fr.studio.voxel.organ.network.services.TaskApiService
import fr.studio.voxel.organ.network.services.TaskAttachmentResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

class TaskAttachmentHandler(
    private val taskService: TaskApiService,
    private val scope: CoroutineScope
) {
    var attachments by mutableStateOf<List<TaskAttachmentResponse>>(emptyList())
        internal set
    var trashedAttachments by mutableStateOf<List<TaskAttachmentResponse>>(emptyList())
        internal set

    fun loadAttachments(pUuid: String, oUuid: String, tUuid: String) {
        scope.launch {
            try {
                val res = taskService.getAttachments(pUuid, oUuid, tUuid)
                if (res.isSuccessful) {
                    attachments = res.body() ?: emptyList()
                }
            } catch (e: Exception) {
                Log.e("TASK_ATTACHMENT_HANDLER", "loadAttachments error", e)
            }
        }
    }

    fun loadTrashData(pUuid: String, oUuid: String, tUuid: String) {
        scope.launch {
            try {
                val aRes = taskService.getTrashedAttachments(pUuid, oUuid, tUuid)
                if (aRes.isSuccessful) {
                    trashedAttachments = aRes.body() ?: emptyList()
                }
            } catch (e: Exception) {
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
                val response = taskService.uploadAttachment(pUuid, oUuid, tUuid, body)
                if (response.isSuccessful) {
                    loadAttachments(pUuid, oUuid, tUuid)
                    onTimelineUpdate()
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
            try {
                val response = taskService.deleteAttachment(pUuid, oUuid, tUuid, attachmentUuid, permanent)
                if (response.isSuccessful) {
                    loadAttachments(pUuid, oUuid, tUuid)
                    onTimelineUpdate()
                    if (isTrashOpen) {
                        loadTrashData(pUuid, oUuid, tUuid)
                    }
                }
            } catch (e: Exception) {
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
            try {
                val response = taskService.restoreAttachment(pUuid, oUuid, tUuid, attachmentUuid)
                if (response.isSuccessful) {
                    loadAttachments(pUuid, oUuid, tUuid)
                    onTimelineUpdate()
                    loadTrashData(pUuid, oUuid, tUuid)
                }
            } catch (e: Exception) {
                Log.e("TASK_ATTACHMENT_HANDLER", "restoreAttachment error", e)
            }
        }
    }
}
