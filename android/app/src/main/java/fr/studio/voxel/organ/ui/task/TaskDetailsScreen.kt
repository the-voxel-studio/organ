package fr.studio.voxel.organ.ui.task

import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import fr.studio.voxel.organ.R
import fr.studio.voxel.organ.ui.task.components.*
import fr.studio.voxel.organ.network.services.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Edit
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import java.util.Calendar
import fr.studio.voxel.organ.ui.components.LoadingOverlay
import fr.studio.voxel.organ.ui.components.PrimaryButton
import fr.studio.voxel.organ.ui.theme.MaterialColorScheme
import fr.studio.voxel.organ.viewmodel.TaskDetailsViewModel
import androidx.compose.foundation.layout.ExperimentalLayoutApi

import fr.studio.voxel.organ.ui.components.ShimmerBox

@Composable
fun TaskDetailsShimmer() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Top Bar Mock
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            ShimmerBox(modifier = Modifier.size(40.dp), shape = CircleShape)
            ShimmerBox(modifier = Modifier.width(120.dp).height(24.dp))
            ShimmerBox(modifier = Modifier.size(40.dp), shape = CircleShape)
        }

        // Basic Info Card Mock
        ShimmerBox(modifier = Modifier.fillMaxWidth().height(150.dp))

        // Other Cards Mocks
        repeat(3) {
            ShimmerBox(modifier = Modifier.fillMaxWidth().height(80.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TaskDetailsScreen(
    projectUuid: String,
    organUuid: String,
    taskUuid: String,
    onBack: () -> Unit,
    onSuccess: () -> Unit,
    viewModel: TaskDetailsViewModel = viewModel()
) {
    val context = LocalContext.current

    LaunchedEffect(projectUuid, organUuid, taskUuid) {
        viewModel.initTask(projectUuid, organUuid, taskUuid)
    }

    LaunchedEffect(viewModel.isSuccess) {
        if (viewModel.isSuccess) {
            onSuccess()
        }
    }

    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var isDeletePermanent by remember { mutableStateOf(false) }



    var showAttachmentSourceDialog by remember { mutableStateOf(false) }
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempCameraUri != null) {
            val contentResolver = context.contentResolver
            try {
                val bytes = contentResolver.openInputStream(tempCameraUri!!)?.readBytes()
                if (bytes != null) {
                    val mimeType = "image/jpeg"
                    viewModel.uploadAttachment("camera_photo.jpg", mimeType, bytes)
                }
            } catch (e: Exception) {
                Log.e("TASK_DETAILS_SCREEN", "Camera photo read error", e)
            }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val uri = fr.studio.voxel.organ.ui.components.createTempImageUri(context)
            tempCameraUri = uri
            cameraLauncher.launch(uri)
        }
    }



    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val contentResolver = context.contentResolver
            var fileName = "file"
            contentResolver.query(it, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index >= 0) {
                        fileName = cursor.getString(index)
                    }
                }
            }
            try {
                val bytes = contentResolver.openInputStream(it)?.readBytes()
                if (bytes != null) {
                    val mimeType = contentResolver.getType(it) ?: "application/octet-stream"
                    viewModel.uploadAttachment(fileName, mimeType, bytes)
                }
            } catch (e: Exception) {
                Log.e("TASK_DETAILS_SCREEN", "File read error", e)
            }
        }
    }

    val isTrashed = viewModel.taskData?.deletedAt != null

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        if (viewModel.isLoading) {
            TaskDetailsShimmer()
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                // PC Modal style Top Bar
                TaskDetailsTopBar(
                    isTrashed = isTrashed,
                    taskUuid = viewModel.taskUuid ?: "",
                    taskTitle = viewModel.taskData?.title,
                    onBack = onBack,
                    hasRestorePermission = viewModel.hasPerm("TASK_RESTORE", viewModel.isTaskOwner()),
                    onRestoreClick = { viewModel.restoreTask() },
                    hasDeletePermission = (isTrashed && viewModel.hasPerm("TASK_HARD_DELETE", viewModel.isTaskOwner())) ||
                            (!isTrashed && viewModel.hasPerm("TASK_DELETE", viewModel.isTaskOwner())),
                    onDeleteClick = {
                        isDeletePermanent = isTrashed
                        showDeleteConfirmDialog = true
                    }
                )

                // Scrollable Content Pane
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {

                    // 1. Basic Details Form Card
                    TaskBasicInfoCard(viewModel = viewModel)

                    if (viewModel.taskUuid != "new") {
                        TaskAssigneesCard(viewModel = viewModel, isTrashed = isTrashed)
                        TaskTagsCard(viewModel = viewModel, isTrashed = isTrashed)
                        TaskLinksCard(viewModel = viewModel, isTrashed = isTrashed)
                        TaskDependenciesCard(viewModel = viewModel, isTrashed = isTrashed)
                        TaskAttachmentsCard(
                            viewModel = viewModel,
                            isTrashed = isTrashed,
                            onAddAttachmentClick = { showAttachmentSourceDialog = true }
                        )
                        TaskCommentsCard(viewModel = viewModel, isTrashed = isTrashed)
                        TaskTimelineCard(viewModel = viewModel)
                        TaskTrashCard(viewModel = viewModel)
                    }
                }

                // Sticky Footer
                if (!isTrashed) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                            .padding(24.dp)
                    ) {
                        PrimaryButton(
                            text = if (viewModel.taskUuid == "new") "Créer la tâche" else "Enregistrer les modifications",
                            onClick = { viewModel.saveTask() },
                            modifier = Modifier.fillMaxWidth(),
                            isLoading = viewModel.isSaving
                        )
                    }
                }
            }
        }
    }

    // Delete confirmation dialog
    if (showDeleteConfirmDialog) {
        TaskDeleteConfirmDialog(
            isDeletePermanent = isDeletePermanent,
            onDismiss = { showDeleteConfirmDialog = false },
            onConfirm = {
                viewModel.deleteTask(isDeletePermanent)
                showDeleteConfirmDialog = false
            }
        )
    }

    // Attachment Source Dialog
    if (showAttachmentSourceDialog) {
        TaskAttachmentSourceDialog(
            onDismiss = { showAttachmentSourceDialog = false },
            onSelectCamera = {
                showAttachmentSourceDialog = false
                cameraPermissionLauncher.launch("android.permission.CAMERA")
            },
            onSelectFile = {
                showAttachmentSourceDialog = false
                filePickerLauncher.launch("*/*")
            }
        )
    }
}
