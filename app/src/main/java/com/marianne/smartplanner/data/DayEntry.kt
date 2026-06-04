package com.marianne.smartplanner.data

data class Goal(
    val id: Int,
    val name: String,
    val checked: Boolean = false,
    val completedAt: String? = null
)

enum class Mood(val label: String, val emoji: String) {
    AWFUL("Awful", "😭"),
    BAD("Bad",   "😟"),
    OKAY("Okay", "😐"),
    GOOD("Good", "🙂"),
    GREAT("Great","😄")
}

data class DayEntry(
    val date: String = "",
    val awakeStart: String = "",
    val awakeEnd: String = "",
    val awakeEndNextDay: Boolean = false,
    val routineChecks: Map<Int, Boolean> = emptyMap(),
    val routineCheckTimes: Map<Int, String> = emptyMap(),
    val weeklyChecks: Map<Int, Boolean> = emptyMap(),
    val weeklyCheckTimes: Map<Int, String> = emptyMap(),
    val todayGoals: List<Goal> = emptyList(),
    val diary: String = "",
    val moodOrdinal: Int = -1
) {
    fun isRoutineTaskChecked(taskId: Int) = routineChecks[taskId] == true
    fun isWeeklyTaskChecked(taskId: Int) = weeklyChecks[taskId] == true

    val awakeMinutes: Int?
        get() {
            val s = parseTime(awakeStart) ?: return null
            val e = parseTime(awakeEnd) ?: return null
            return if (awakeEndNextDay || e < s) (24 * 60 - s) + e else e - s
        }
}

fun parseTime(raw: String): Int? {
    val parts = raw.trim().split(":")
    val h = parts.getOrNull(0)?.toIntOrNull() ?: return null
    val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
    if (h !in 0..23 || m !in 0..59) return null
    return h * 60 + m
}

fun formatMinutes(total: Int): String {
    val h = total / 60
    val m = total % 60
    return when {
        h > 0 && m > 0 -> "${h}h ${m}min"
        h > 0 -> "${h}h"
        else -> "${m}min"
    }
}
