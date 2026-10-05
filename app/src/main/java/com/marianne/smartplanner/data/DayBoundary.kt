package com.marianne.smartplanner.data

import android.content.Context
import java.time.LocalDate
import java.time.LocalDateTime

/** When the planner's day rolls over (e.g. "04:00" means 03:59 still belongs to the previous date). */
object DayBoundary {

    private const val PREFS   = "app_settings"
    private const val KEY     = "day_end_time"
    private const val DEFAULT = "00:00"

    fun get(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, DEFAULT) ?: DEFAULT

    fun set(context: Context, time: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, time).apply()
    }

    private fun rolloverMinutes(context: Context): Int {
        val p = get(context).split(":").mapNotNull { it.toIntOrNull() }
        return if (p.size < 2) 0 else p[0] * 60 + p[1]
    }

    /** The planner date that "right now" belongs to. */
    fun today(context: Context): LocalDate =
        LocalDateTime.now().minusMinutes(rolloverMinutes(context).toLong()).toLocalDate()

    /** Final minute of a planner day, e.g. "03:59" for a 04:00 rollover, "23:59" for midnight. */
    fun lastMinute(context: Context): String {
        val m = (rolloverMinutes(context) - 1 + 24 * 60) % (24 * 60)
        return "%02d:%02d".format(m / 60, m % 60)
    }
}
