package fr.studio.voxel.organ.ui.task.components

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import java.util.Calendar

fun formatDateTimeForDisplay(dateTimeStr: String): String {
    if (dateTimeStr.isBlank()) return ""
    // Input format: YYYY-MM-DD HH:mm
    // Output format: DD/MM/YYYY HH:mm
    return try {
        val parts = dateTimeStr.split(" ")
        if (parts.size == 2) {
            val dateParts = parts[0].split("-")
            val timeParts = parts[1].split(":")
            if (dateParts.size == 3 && timeParts.size == 2) {
                "${dateParts[2]}/${dateParts[1]}/${dateParts[0]} ${timeParts[0]}:${timeParts[1]}"
            } else {
                dateTimeStr
            }
        } else {
            dateTimeStr
        }
    } catch (e: Exception) {
        dateTimeStr
    }
}

fun showDateTimePicker(
    context: Context,
    currentValue: String,
    onDateTimeSelected: (String) -> Unit
) {
    val calendar = Calendar.getInstance()
    if (currentValue.isNotBlank()) {
        try {
            val parts = currentValue.split(" ")
            if (parts.size == 2) {
                val dateParts = parts[0].split("-")
                val timeParts = parts[1].split(":")
                calendar.set(Calendar.YEAR, dateParts[0].toInt())
                calendar.set(Calendar.MONTH, dateParts[1].toInt() - 1)
                calendar.set(Calendar.DAY_OF_MONTH, dateParts[2].toInt())
                calendar.set(Calendar.HOUR_OF_DAY, timeParts[0].toInt())
                calendar.set(Calendar.MINUTE, timeParts[1].toInt())
            }
        } catch (e: Exception) {
            // fallback
        }
    }

    DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            TimePickerDialog(
                context,
                { _, hourOfDay, minute ->
                    val formattedMonth = String.format("%02d", month + 1)
                    val formattedDay = String.format("%02d", dayOfMonth)
                    val formattedHour = String.format("%02d", hourOfDay)
                    val formattedMinute = String.format("%02d", minute)
                    onDateTimeSelected("$year-$formattedMonth-$formattedDay $formattedHour:$formattedMinute")
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                true
            ).show()
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    ).show()
}
