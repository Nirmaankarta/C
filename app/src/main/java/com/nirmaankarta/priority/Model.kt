package com.nirmaankarta.priority

import androidx.compose.ui.graphics.Color
import java.time.LocalDate
import java.time.temporal.ChronoUnit

enum class Quadrant(val label: String, val title: String, val tagline: String, val accent: Color) {
    Q1("Q1", "Do First", "Urgent & Important", Color(0xFFEF4444)),
    Q2("Q2", "Schedule", "Not Urgent, but Important", Color(0xFF3B82F6)),
    Q3("Q3", "Delegate", "Urgent, Not Important", Color(0xFFF59E0B)),
    Q4("Q4", "Eliminate", "Neither Urgent nor Important", Color(0xFF64748B)),
}

data class Task(
    val id: Long,
    val text: String,
    val quadrant: Quadrant,
    val done: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
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
