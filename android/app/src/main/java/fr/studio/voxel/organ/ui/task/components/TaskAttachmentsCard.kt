package fr.studio.voxel.organ.ui.task.components

import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import fr.studio.voxel.organ.R
import fr.studio.voxel.organ.ui.theme.MaterialColorScheme
import fr.studio.voxel.organ.viewmodel.TaskDetailsViewModel
import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.content.Intent
import android.widget.Toast
import android.util.Log
import androidx.compose.ui.platform.LocalContext
import fr.studio.voxel.organ.network.ApiClient
import androidx.core.content.FileProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

@Composable
fun TaskAttachmentsCard(
    viewModel: TaskDetailsViewModel,
    isTrashed: Boolean,
    onAddAttachmentClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showAttachmentsExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val isOwner = viewModel.isTaskOwner()
            val canAddAttachment = !isTrashed && viewModel.hasPerm("ATTACHMENT_ADD", isOwner)
            val canDeleteAttachment = !isTrashed && viewModel.hasPerm("ATTACHMENT_DELETE", isOwner)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showAttachmentsExpanded = !showAttachmentsExpanded }
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Fichiers joints",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Icon(
                    painter = painterResource(R.drawable.flechedroite_logo),
                    contentDescription = if (showAttachmentsExpanded) "Réduire" else "Développer",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .rotate(if (showAttachmentsExpanded) 270f else 90f)
                        .size(16.dp)
                )
            }

            AnimatedVisibility(
                visible = showAttachmentsExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (viewModel.attachments.isEmpty()) {
                        Text(
                            text = "Aucun fichier joint",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.outline
                        )
                    } else {
                        viewModel.attachments.forEach { att ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            downloadOrOpenAttachment(
                                                context = context,
                                                projectUuid = viewModel.projectUuid,
                                                organUuid = viewModel.organUuid,
                                                taskUuid = viewModel.taskUuid,
                                                attachment = att
                                            )
                                        }
                                ) {
                                    Text(
                                        text = att.fileName,
                                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${att.fileSize / 1024} Ko • Par ${att.uploadedBy.firstName}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                                if (canDeleteAttachment) {
                                    IconButton(onClick = { viewModel.deleteAttachment(att.uuid) }) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Supprimer",
                                            tint = Color(0xFFEF4444),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (canAddAttachment) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "+ Joindre",
                            color = MaterialColorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clickable { onAddAttachmentClick() }
                                .padding(8.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun downloadOrOpenAttachment(
    context: Context,
    projectUuid: String?,
    organUuid: String?,
    taskUuid: String?,
    attachment: fr.studio.voxel.organ.network.services.TaskAttachmentResponse
) {
    if (projectUuid == null || organUuid == null || taskUuid == null) {
        Toast.makeText(context, "Erreur de contexte de la tâche", Toast.LENGTH_SHORT).show()
        return
    }

    if (attachment.filePath.startsWith("drive://")) {
        val driveId = attachment.filePath.replace("drive://", "")
        val driveUrl = "https://drive.google.com/open?id=$driveId"
        Toast.makeText(context, "Ouverture dans le navigateur...", Toast.LENGTH_SHORT).show()
        try {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(driveUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                selector = Intent(Intent.ACTION_VIEW, Uri.parse("https://")).apply {
                    addCategory(Intent.CATEGORY_BROWSABLE)
                }
            }
            context.startActivity(browserIntent)
        } catch (e: Exception) {
            Toast.makeText(context, "Impossible d'ouvrir le navigateur", Toast.LENGTH_SHORT).show()
        }
    } else {
        Toast.makeText(context, "Téléchargement de ${attachment.fileName}...", Toast.LENGTH_SHORT).show()
        
        val baseUrl = ApiClient.getBaseUrl()
        val downloadUrl = "$baseUrl/api/projects/$projectUuid/organs/$organUuid/tasks/$taskUuid/attachments/${attachment.uuid}/download"
        
        val client = ApiClient.getOkHttpClient()
        val request = okhttp3.Request.Builder()
            .url(downloadUrl)
            .build()
            
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = client.newCall(request).execute()
                if (!response.isSuccessful) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Échec du téléchargement (code ${response.code})", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }
                
                val body = response.body
                if (body == null) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Fichier vide", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }
                
                // Write file to cache directory to be shared via FileProvider
                val file = File(context.cacheDir, attachment.fileName)
                body.byteStream().use { input ->
                    FileOutputStream(file).use { output ->
                        input.copyTo(output)
                    }
                }
                
                withContext(Dispatchers.Main) {
                    try {
                        val fileUri: Uri = FileProvider.getUriForFile(
                            context,
                            "fr.studio.voxel.organ.fileprovider",
                            file
                        )
                        
                        val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(fileUri, attachment.fileType.ifEmpty { "*/*" })
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(viewIntent)
                    } catch (e: Exception) {
                        Log.e("ATTACHMENT_OPEN", "Failed to open file", e)
                        Toast.makeText(context, "Fichier téléchargé dans le cache, mais aucune application ne peut l'ouvrir.", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("ATTACHMENT_DOWNLOAD", "Download failed", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Erreur lors du téléchargement", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}

