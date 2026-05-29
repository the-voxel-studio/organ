package fr.studio.voxel.organ.ui.create

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
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import java.lang.Character.isEmoji

@Composable
fun IconSelector(
    selectedTab : IconType,
    onTabSelected: (IconType) -> Unit = {},
    imageUri: Uri?,
    onImageSelected: (Uri?) -> Unit = {},
    emojiText : String,
    onEmojiChanged: (String) -> Unit = {},
    svgCode: String,
    onSvgChanged: (String) -> Unit = {}
){
    //Ouvre la galerie photo
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) {uri: Uri? ->
        onImageSelected(uri)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ){
        TabRow(
            selectedTabIndex = selectedTab.ordinal,
            containerColor = MaterialTheme.colorScheme.outline.copy(0.2f),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp)),
            indicator = {Box(Modifier)},
            divider = {Box(Modifier)}
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
        ){
            when(selectedTab){
                IconType.IMAGE -> {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(120.dp)
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outlineVariant,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { galleryLauncher.launch("image/*") }
                    ){
                        if (imageUri != null){
                            AsyncImage(
                                model = imageUri,
                                contentDescription = "Image importée depuis la gallerie",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(12.dp))
                            )
                        } else{
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
                IconType.CAMERA ->{

                }
                IconType.EMOJI -> {
                    OutlinedTextField(
                        value = emojiText,
                        onValueChange = { input ->
                            if (input.isEmpty() || isEmoji(input )){
                                onEmojiChanged(input)
                            }
                        },
                        placeholder = { Text ("\uD83D\uDE80") },
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
}

fun isEmoji(text: String): Boolean {
    if (text.isEmpty()) return false
    val type = Character.getType(text.codePointAt(0)).toByte()
    return type == Character.SURROGATE || type == Character.OTHER_SYMBOL
}