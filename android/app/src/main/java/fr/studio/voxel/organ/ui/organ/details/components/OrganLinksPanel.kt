package fr.studio.voxel.organ.ui.organ.details.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import fr.studio.voxel.organ.network.services.OrganLinkSummary
import fr.studio.voxel.organ.ui.components.ShimmerBox

@Composable
fun OrganLinksPanel(
    projectUuid: String,
    organUuid: String,
    canManage: Boolean,
    highlightColor: Color,
    showLinksPanel: Boolean,
    links: List<OrganLinkSummary>,
    isLinksLoading: Boolean,
    isSavingLink: Boolean,
    linkErrorMessage: String?,
    onCreateLink: (String, String?) -> Unit,
    onUpdateLink: (String, String, String?) -> Unit,
    onDeleteLink: (String) -> Unit
) {
    if (!showLinksPanel) return

    var showForm by remember { mutableStateOf(false) }
    var editingLinkUuid by remember { mutableStateOf<String?>(null) }
    var linkUrl by remember { mutableStateOf("") }
    var linkDesc by remember { mutableStateOf("") }

    var linkToDelete by remember { mutableStateOf<OrganLinkSummary?>(null) }
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
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
                    text = if (canManage) "Gestion des Liens" else "Liens de l'Organ",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (canManage) {
                    TextButton(onClick = {
                        showForm = !showForm
                        editingLinkUuid = null
                        linkUrl = ""
                        linkDesc = ""
                    }) {
                        Text(
                            text = if (showForm) "Fermer" else "+ Ajouter",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = highlightColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (isLinksLoading) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeat(2) {
                        ShimmerBox(modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(12.dp))
                    }
                }
            } else {
                linkErrorMessage?.let { err ->
                    Text(
                        text = err,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }

                if (links.isEmpty()) {
                    Text(
                        text = "Aucun lien disponible.",
                        style = MaterialTheme.typography.bodyMedium,
                        color =MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        links.forEach { link ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surface)
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                    .clickable {
                                        openBrowser(context, link.url)
                                    }
                                    .padding(horizontal = 12.dp, vertical = 10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Link,
                                    contentDescription = null,
                                    tint = highlightColor,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = link.description ?: link.url,
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (link.description != null) {
                                        Text(
                                            text = link.url,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                if (canManage) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Modifier",
                                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clickable {
                                                editingLinkUuid = link.uuid
                                                linkUrl = link.url
                                                linkDesc = link.description ?: ""
                                                showForm = true
                                            }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Supprimer",
                                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clickable {
                                                linkToDelete = link
                                            }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Form
            if (showForm && canManage) {
                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = if (editingLinkUuid != null) "Modifier le lien" else "Nouveau lien",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = linkUrl,
                    onValueChange = { linkUrl = it },
                    placeholder = { Text("https://...") },
                    singleLine = true,
                    label = { Text("URL du lien") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = linkDesc,
                    onValueChange = { linkDesc = it },
                    placeholder = { Text("Ex: Documentation API") },
                    singleLine = true,
                    label = { Text("Description (optionnel)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextButton(onClick = {
                        showForm = false
                        editingLinkUuid = null
                        linkUrl = ""
                        linkDesc = ""
                    }) {
                        Text("Annuler")
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Button(
                        onClick = {
                            if (linkUrl.isNotBlank()) {
                                val uuid = editingLinkUuid
                                val desc = if (linkDesc.isBlank()) null else linkDesc
                                if (uuid != null) {
                                    onUpdateLink(uuid, linkUrl, desc)
                                } else {
                                    onCreateLink(linkUrl, desc)
                                }
                                showForm = false
                                editingLinkUuid = null
                                linkUrl = ""
                                linkDesc = ""
                            }
                        },
                        enabled = linkUrl.isNotBlank() && !isSavingLink,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = highlightColor)
                    ) {
                        if (isSavingLink) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp))
                        } else {
                            Text("Enregistrer")
                        }
                    }
                }
            }
        }
    }

    linkToDelete?.let { link ->
        AlertDialog(
            onDismissRequest = { linkToDelete = null },
            title = { Text("Supprimer le lien", fontWeight = FontWeight.Bold) },
            text = { Text("Voulez-vous vraiment supprimer le lien \"${link.description ?: link.url}\" ?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteLink(link.uuid)
                        linkToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Confirmer")
                }
            },
            dismissButton = {
                TextButton(onClick = { linkToDelete = null }) {
                    Text("Annuler")
                }
            }
        )
    }
}


