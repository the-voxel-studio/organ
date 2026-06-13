package fr.studio.voxel.organ.ui.project.details.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import fr.studio.voxel.organ.network.services.TagResponse
import fr.studio.voxel.organ.ui.components.ColorSelector
import androidx.compose.ui.text.font.FontWeight
import fr.studio.voxel.organ.ui.components.ShimmerBox

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProjectTagsPanel(
    projectUuid: String,
    canManage: Boolean,
    projectColor: Color,
    showTagsPanel: Boolean,
    tags: List<TagResponse>,
    isTagsLoading: Boolean,
    isSavingTag: Boolean,
    tagErrorMessage: String?,
    onCreateTag: (String, String) -> Unit,
    onUpdateTag: (String, String, String) -> Unit,
    onDeleteTag: (String) -> Unit
) {
    if (!showTagsPanel) return

    var showForm by remember { mutableStateOf(false) }
    var editingTagUuid by remember { mutableStateOf<String?>(null) }
    var tagName by remember { mutableStateOf("") }
    var tagColor by remember { mutableStateOf(Color(0xFF808080)) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (canManage) "Gestion des Tags" else "Tags du projet",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (canManage) {
                    IconButton(
                        onClick = {
                            showForm = !showForm
                            if (showForm) {
                                editingTagUuid = null
                                tagName = ""
                                tagColor = Color(0xFF808080)
                            }
                        }
                    ) {
                        Crossfade(targetState = showForm, label = "tagFormButtonAnim") { isFormOpen ->
                            Icon(
                                imageVector = if (isFormOpen) Icons.Default.Close else Icons.Default.Add,
                                contentDescription = if (isFormOpen) "Fermer le formulaire" else "Ajouter un tag",
                                modifier = Modifier.size(28.dp),
                                tint = projectColor
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (isTagsLoading) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    repeat(4) {
                        ShimmerBox(modifier = Modifier.width(80.dp).height(32.dp), shape = RoundedCornerShape(10.dp))
                    }
                }
            } else {
                tagErrorMessage?.let { err ->
                    Text(
                        text = err,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }

                if (tags.isEmpty()) {
                    Text(
                        text = "Aucun tag disponible.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                } else {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        tags.forEach { tag ->
                            val tColor = remember(tag.color) {
                                try {
                                    val hex = tag.color.replace("#", "")
                                    Color(android.graphics.Color.parseColor("#$hex"))
                                } catch (e: Exception) {
                                    Color.Gray
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.surface)
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(tColor)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = tag.name,
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                if (canManage) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Modifier",
                                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                        modifier = Modifier
                                            .size(14.dp)
                                            .clickable {
                                                editingTagUuid = tag.uuid
                                                tagName = tag.name
                                                tagColor = tColor
                                                showForm = true
                                            }
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Supprimer",
                                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                        modifier = Modifier
                                            .size(14.dp)
                                            .clickable {
                                                onDeleteTag(tag.uuid)
                                            }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Creation / Edit Form
            AnimatedVisibility(
                visible = showForm && canManage,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(20.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = if (editingTagUuid != null) "Modifier le tag" else "Nouveau tag",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = tagName,
                        onValueChange = { tagName = it },
                        placeholder = { Text("Ex: Urgent, Bug...") },
                        singleLine = true,
                        label = { Text("Nom du tag") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Couleur du tag",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    ColorSelector(
                        selectedColor = tagColor,
                        onColorSelected = { tagColor = it }
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TextButton(onClick = {
                            showForm = false
                            editingTagUuid = null
                            tagName = ""
                        }) {
                            Text("Annuler")
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Button(
                            onClick = {
                                val colorHex = String.format("#%06X", 0xFFFFFF and tagColor.toArgb())
                                if (tagName.isNotBlank()) {
                                    val uuid = editingTagUuid
                                    if (uuid != null) {
                                        onUpdateTag(uuid, tagName, colorHex)
                                    } else {
                                        onCreateTag(tagName, colorHex)
                                    }
                                    showForm = false
                                    editingTagUuid = null
                                    tagName = ""
                                }
                            },
                            enabled = tagName.isNotBlank() && !isSavingTag,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (isSavingTag) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp))
                            } else {
                                Text("Enregistrer")
                            }
                        }
                    }
                }
            }
        }
    }
}
