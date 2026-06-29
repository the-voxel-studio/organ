package fr.studio.voxel.organ.ui.organ.details.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp

@Composable
fun ClickableDescriptionText(
    text: String,
    highlightColor: Color,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val urlPattern = remember {
        java.util.regex.Pattern.compile(
            "(https?://[\\w\\d:#@%/;\\$()~_?\\+-=\\\\.&]+)",
            java.util.regex.Pattern.CASE_INSENSITIVE
        )
    }

    val annotatedString = remember(text) {
        buildAnnotatedString {
            val matcher = urlPattern.matcher(text)
            var lastIdx = 0
            while (matcher.find()) {
                val start = matcher.start()
                val end = matcher.end()

                // Normal text
                append(text.substring(lastIdx, start))

                // Link text
                val linkUrl = text.substring(start, end)
                val linkStart = length
                append(linkUrl)
                addStyle(
                    style = SpanStyle(
                        color = highlightColor,
                        textDecoration = TextDecoration.Underline,
                        fontWeight = FontWeight.Bold
                    ),
                    start = linkStart,
                    end = length
                )
                addStringAnnotation(
                    tag = "URL",
                    annotation = linkUrl,
                    start = linkStart,
                    end = length
                )

                lastIdx = end
            }
            if (lastIdx < text.length) {
                append(text.substring(lastIdx))
            }
        }
    }

    ClickableText(
        text = annotatedString,
        style = MaterialTheme.typography.bodySmall.copy(
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
            lineHeight = 22.sp,
            fontWeight = FontWeight.Normal
        ),
        modifier = modifier,
        onClick = { offset ->
            annotatedString.getStringAnnotations(tag = "URL", start = offset, end = offset)
                .firstOrNull()?.let { annotation ->
                    openBrowser(context, annotation.item)
                }
        }
    )
}

fun openBrowser(context: android.content.Context, url: String) {
    try {
        val cleanUrl = if (!url.startsWith("http://") && !url.startsWith("https://")) {
            "https://$url"
        } else {
            url
        }
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(cleanUrl))
        context.startActivity(intent)
    } catch (e: Exception) {
        // ignore
    }
}

fun formatDate(isoDate: String?): String {
    if (isoDate == null) return "-"
    return try {
        val clean = isoDate.substringBefore("T")
        val parts = clean.split("-")
        if (parts.size == 3) {
            "${parts[2]}/${parts[1]}/${parts[0]}"
        } else {
            clean
        }
    } catch (e: Exception) {
        "-"
    }
}
