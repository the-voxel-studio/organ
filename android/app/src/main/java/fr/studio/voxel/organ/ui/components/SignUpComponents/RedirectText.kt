package fr.studio.voxel.organ.ui.components.SignUpComponents

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withLink

@Composable
fun LoginRedirectText(
    onLoginClick: () -> Unit,
    normalText : String,
    linkText : String
) {
    val annotatedString = buildAnnotatedString {
        append(normalText)

        //Configure le lien cliquable
        val link = LinkAnnotation.Url(
            url = "login", // Un identifiant ou une URL factice
            styles = TextLinkStyles(
                style = SpanStyle(
                    color = MaterialTheme.colorScheme.primary, // Ta couleur (ex: Bleu ou Vert)
                    fontWeight = FontWeight.Bold
                ),
                pressedStyle = SpanStyle(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                    textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
                )
            ),
            //Se déclenche quand on clique dessus
            linkInteractionListener = {
                onLoginClick()
            }
        )

        // Entoure le string avec ce lien
        withLink(link) {
            append(linkText)
        }
    }

    Text(text = annotatedString)
}