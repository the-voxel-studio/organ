package fr.studio.voxel.organ.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import java.text.BreakIterator

enum class IconType(val title: String) {
    IMAGE("Image"),
    EMOJI("Emoji"),
    SVG("SVG"),
    CAMERA("Camera")
}

@Composable
fun IconSelector(
    selectedTab : IconType,
    onTabSelected: (IconType) -> Unit = {},
    imageUri: Any?,
    onImageSelected: (Uri?) -> Unit = {},
    emojiText : String,
    onEmojiChanged: (String) -> Unit = {},
    svgCode: String,
    onSvgChanged: (String) -> Unit = {}
) {
    val context = LocalContext.current
    var tempUri by remember { mutableStateOf<Uri?>(null) }
    var showPermissionRationale by remember { mutableStateOf(false) }

    // Ouvre la galerie photo
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        onImageSelected(uri)
    }

    // Ouvre l'appareil photo
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempUri != null) {
            onImageSelected(tempUri)
        }
    }

    // Demande la permission
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val uri = createTempImageUri(context)
            tempUri = uri
            cameraLauncher.launch(uri)
        }
    }

    val resolvedModel = remember(imageUri) {
        val uriStr = when (imageUri) {
            is String -> imageUri
            is Uri -> imageUri.toString()
            else -> null
        }
        if (uriStr != null) {
            if (uriStr.startsWith("data:")) {
                try {
                    val base64Content = uriStr.substringAfter("base64,")
                    android.util.Base64.decode(base64Content, android.util.Base64.DEFAULT)
                } catch (e: Exception) {
                    null
                }
            } else if (uriStr.startsWith("/") || uriStr.startsWith("uploads/")) {
                val cleanPath = if (uriStr.startsWith("/")) uriStr else "/$uriStr"
                "http://10.0.2.2:8001$cleanPath"
            } else if (uriStr.startsWith("http")) {
                uriStr.replace("localhost:8000", "10.0.2.2:8001")
                    .replace("127.0.0.1:8000", "10.0.2.2:8001")
            } else {
                try {
                    android.util.Base64.decode(uriStr, android.util.Base64.DEFAULT)
                } catch (e: Exception) {
                    imageUri
                }
            }
        } else {
            imageUri
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        TabRow(
            selectedTabIndex = selectedTab.ordinal,
            containerColor = MaterialTheme.colorScheme.outline.copy(0.2f),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp)),
            indicator = { Box(Modifier) },
            divider = { Box(Modifier) }
        ) {
            IconType.entries.forEach { type ->
                val isSelected = selectedTab == type
                Tab(
                    selected = isSelected,
                    onClick = { onTabSelected(type) },
                    modifier = Modifier
                        .padding(4.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent),
                    text = {
                        Text(
                            text = type.title,
                            color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 120.dp)
        ) {
            when (selectedTab) {
                IconType.IMAGE -> {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(120.dp)
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { galleryLauncher.launch("image/*") }
                    ) {
                        if (imageUri != null) {
                            AsyncImage(
                                model = resolvedModel,
                                contentDescription = "Image importée depuis la galerie",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(12.dp))
                            )
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.Add, contentDescription = "Upload an image"
                                )
                                Text(
                                    text = "Upload",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
                IconType.CAMERA -> {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(120.dp)
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable {
                                val currentPermission = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.CAMERA
                                ) == PackageManager.PERMISSION_GRANTED
                                if (currentPermission) {
                                    val uri = createTempImageUri(context)
                                    tempUri = uri
                                    cameraLauncher.launch(uri)
                                } else {
                                    showPermissionRationale = true
                                }
                            }
                    ) {
                        if (imageUri != null) {
                            AsyncImage(
                                model = resolvedModel,
                                contentDescription = "Image prise depuis la caméra",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(12.dp))
                            )
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.PhotoCamera, contentDescription = "Prendre une photo"
                                )
                                Text(
                                    text = "Appareil photo",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
                IconType.EMOJI -> {
                    OutlinedTextField(
                        value = emojiText,
                        onValueChange = { input ->
                            val emoji = getFirstEmojiGrapheme(input)
                            onEmojiChanged(emoji)
                        },
                        placeholder = { Text("\uD83D\uDE80") },
                        textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center),
                        modifier = Modifier
                            .width(100.dp)
                    )
                }

                IconType.SVG -> {
                    OutlinedTextField(
                        value = svgCode,
                        onValueChange = onSvgChanged,
                        placeholder = { Text("<svg> ... </svg>") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }

    if (showPermissionRationale) {
        AlertDialog(
            onDismissRequest = { showPermissionRationale = false },
            title = { Text("Permission Requise", fontWeight = FontWeight.Bold) },
            text = { 
                Text("L'accès à votre appareil photo est nécessaire pour vous permettre de prendre directement une photo et l'utiliser comme icône.") 
            },
            confirmButton = {
                TextButton(onClick = {
                    showPermissionRationale = false
                    permissionLauncher.launch(Manifest.permission.CAMERA)
                }) {
                    Text("Autoriser", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionRationale = false }) {
                    Text("Annuler")
                }
            }
        )
    }
}

fun createTempImageUri(context: android.content.Context): Uri {
    val tempFile = java.io.File.createTempFile("camera_photo_", ".jpg", context.cacheDir).apply {
        createNewFile()
        deleteOnExit()
    }
    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        tempFile
    )
}

fun getFirstEmojiGrapheme(text: String): String {
    val clean = text.trim()
    if (clean.isEmpty()) return ""

    val boundary = BreakIterator.getCharacterInstance()
    boundary.setText(clean)
    var start = boundary.first()
    var end = boundary.next()

    while (end != BreakIterator.DONE) {
        val grapheme = clean.substring(start, end)
        if (isEmojiGrapheme(grapheme)) {
            return grapheme
        }
        start = end
        end = boundary.next()
    }
    return ""
}

fun isEmojiGrapheme(grapheme: String): Boolean {
    if (grapheme.isEmpty()) return false
    var i = 0
    while (i < grapheme.length) {
        val codePoint = grapheme.codePointAt(i)
        val type = Character.getType(codePoint).toByte()
        if (type == Character.SURROGATE || 
            type == Character.OTHER_SYMBOL || 
            codePoint in 0x1F300..0x1F9FF || 
            codePoint in 0x2600..0x27BF
        ) {
            return true
        }
        i += Character.charCount(codePoint)
    }
    return false
}
