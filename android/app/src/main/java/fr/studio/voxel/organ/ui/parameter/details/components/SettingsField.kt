package fr.studio.voxel.organ.ui.parameter.details.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

@Composable
fun SettingsField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    isPassword: Boolean = false,
    showPasswordToggle: Boolean = false,
    onPasswordToggleClick: (() -> Unit)? = null,
    enabled: Boolean = true
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.Black,
                fontSize = 10.sp,
                letterSpacing = 0.15.em
            ),
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)) },
            singleLine = true,
            enabled = enabled,
            visualTransformation = if (isPassword && !showPasswordToggle) androidx.compose.ui.text.input.PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
            trailingIcon = if (isPassword && onPasswordToggleClick != null) {
                {
                    TextButton(onClick = onPasswordToggleClick) {
                        Text(
                            text = if (showPasswordToggle) "MASQUER" else "VOIR",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            } else null,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                disabledBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f),
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color(0xFFF9F9F9),
                disabledContainerColor = Color(0xFFF0F0F0),
                disabledTextColor = Color.Gray
            )
        )
    }
}
