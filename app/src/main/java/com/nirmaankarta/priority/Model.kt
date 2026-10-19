package com.nirmaankarta.priority

import androidx.compose.ui.graphics.Color
import java.time.LocalDate
import java.time.temporal.ChronoUnit

enum class Quadrant(val label: String, val title: String, val tagline: String, val accent: Color) {
    // label = Roman numeral badge, title = heading shown on the card, tagline = classic action name
    Q1("I", "Urgent & Important", "Do first", Color(0xFFF2575F)),
    Q2("II", "Not Urgent & Important", "Schedule", Color(0xFFF5A60B)),
    Q3("III", "Urgent & Unimportant", "Delegate", Color(0xFF4A6CF7)),
    Q4("IV", "Not Urgent & Unimportant", "Eliminate", Color(0xFF12B981)),
}

data class Task(
    val id: Long,
    val text: String,
    val quadrant: Quadrant,
    val done: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val dueDate: Long? = null, // LocalDate epoch-day, optional
)

enum class ThemeMode { System, Light, Dark }

data class Profile(
    val name: String = "You",
    val username: String = "you",
    val avatar: Int = 0,
    val customAvatar: String? = null, // Base64 JPEG
    val sfx: Boolean = true,
    val theme: ThemeMode = ThemeMode.System,
)

/** [last] is an ISO date (YYYY-MM-DD) of the most recent activity. */
data class Streak(val count: Int = 0, val last: String? = null) {
    /** Streak to display: it lapses to 0 once a full day has been missed. */
    fun shown(): Int {
        val d = runCatching { LocalDate.parse(last) }.getOrNull() ?: return 0
        return if (ChronoUnit.DAYS.between(d, LocalDate.now()) <= 1) count else 0
    }
}
