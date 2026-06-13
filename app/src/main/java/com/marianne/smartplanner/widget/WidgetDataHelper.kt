package com.marianne.smartplanner.widget

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.marianne.smartplanner.data.DEFAULT_ROUTINE_TASKS
import com.marianne.smartplanner.data.DayEntry
import com.marianne.smartplanner.data.RoutineTaskDef
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

object WidgetDataHelper {

    private val dateFmt = DateTimeFormatter.ISO_LOCAL_DATE
    private val timeFmt = DateTimeFormatter.ofPattern("HH:mm")
    private val gson = Gson()

    // Widget taps fire independent ActionCallbacks that can run concurrently on
    // different threads. Without this lock, two quick taps both read the entry
    // before either writes, and the second save clobbers the first — so a check
    // is silently lost. Serialize every read-modify-write of the day entry.
    private val entryLock = Any()

    fun todayKey(): String = LocalDate.now().format(dateFmt)
    private fun nowTime(): String = LocalTime.now().format(timeFmt)

    fun loadEntry(context: Context): DayEntry {
        val key = todayKey()
        val json = context.getSharedPreferences("day_entries", Context.MODE_PRIVATE)
            .getString(key, null) ?: return DayEntry(date = key)
        return try {
            gson.fromJson(json, DayEntry::class.java) ?: DayEntry(date = key)
        } catch (e: Exception) {
            DayEntry(date = key)
        }
    }

    fun saveEntry(context: Context, entry: DayEntry) {
        // commit() (synchronous), NOT apply(): widget toggles run in a background
        // process the OS may freeze/kill the instant the click handler returns.
        // apply()'s async disk flush can be lost in that window, silently dropping
        // a check ("it was checked, then it wasn't"). commit() forces the write to
        // disk before we return, while still inside the action's keep-alive window.
        context.getSharedPreferences("day_entries", Context.MODE_PRIVATE)
            .edit().putString(entry.date, gson.toJson(entry)).commit()
    }

    fun loadRoutine(context: Context): List<RoutineTaskDef> {
        val json = context.getSharedPreferences("routine_template", Context.MODE_PRIVATE)
            .getString("tasks", null) ?: return DEFAULT_ROUTINE_TASKS
        return try {
            val type = object : TypeToken<List<RoutineTaskDef>>() {}.type
            gson.fromJson<List<RoutineTaskDef>>(json, type).ifEmpty { DEFAULT_ROUTINE_TASKS }
        } catch (e: Exception) {
            DEFAULT_ROUTINE_TASKS
        }
    }

    fun toggleRoutineTask(context: Context, taskId: Int) {
        synchronized(entryLock) {
            val entry = loadEntry(context)
            val checks = entry.routineChecks.toMutableMap()
            val times = entry.routineCheckTimes.toMutableMap()
            val nowChecked = !(checks[taskId] ?: false)
            checks[taskId] = nowChecked
            if (nowChecked) times[taskId] = nowTime() else times.remove(taskId)
            saveEntry(context, entry.copy(routineChecks = checks, routineCheckTimes = times))
        }
    }

    fun toggleGoal(context: Context, goalId: Int) {
        synchronized(entryLock) {
            val entry = loadEntry(context)
            val time = nowTime()
            saveEntry(context, entry.copy(
                todayGoals = entry.todayGoals.map { g ->
                    if (g.id == goalId) {
                        val willCheck = !g.checked
                        g.copy(checked = willCheck, completedAt = if (willCheck) time else null)
                    } else g
                }
            ))
        }
    }
}
