package com.papoter.app.tools

import java.text.SimpleDateFormat
import java.util.*

object TimeDateTool {
    fun getCurrentTime(): String {
        val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
        return sdf.format(Date())
    }

    fun getCurrentDate(): String {
        val sdf = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault())
        return sdf.format(Date())
    }

    fun getTimeOfDayGreeting(userName: String?): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val name = if (userName.isNullOrBlank()) "there" else userName
        return when (hour) {
            in 5..11 -> "Good morning, $name! Ready for a natter?"
            in 12..16 -> "Good afternoon, $name! Fancy a chat?"
            in 17..21 -> "Good evening, $name! Unwind with a chat?"
            else -> "Up late, $name? I'm here for a midnight natter."
        }
    }

    fun execute(): String {
        return "Current time is ${getCurrentTime()} on ${getCurrentDate()}."
    }
}
