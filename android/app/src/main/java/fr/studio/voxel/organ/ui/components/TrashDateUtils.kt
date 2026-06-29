package fr.studio.voxel.organ.ui.components

import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

fun formatDeletedDate(dateString: String?): String {
    if (dateString.isNullOrBlank()) return "-"
    return try {
        val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.FRANCE)
        parser.timeZone = TimeZone.getTimeZone("UTC")
        val date = parser.parse(dateString) ?: return "-"
        val formatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRANCE)
        formatter.format(date)
    } catch (e: Exception) {
        dateString.take(10)
    }
}
