package com.lukasosstudios.localnotes.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateUtils {
    /** "3:42 PM" if today, otherwise "Sep 4". */
    fun formatRelative(millis: Long): String {
        val target = Calendar.getInstance().apply { timeInMillis = millis }
        val today = Calendar.getInstance()
        val isToday = target.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
            target.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)
        val pattern = if (isToday) "h:mm a" else "MMM d"
        return SimpleDateFormat(pattern, Locale.getDefault()).format(Date(millis))
    }
}
