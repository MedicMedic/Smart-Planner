package com.marianne.smartplanner.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken

object BackupManager {

    private val gson = Gson()

    fun exportJson(context: Context): String {
        val dayPrefs     = context.getSharedPreferences("day_entries",     Context.MODE_PRIVATE)
        val routinePrefs = context.getSharedPreferences("routine_template", Context.MODE_PRIVATE)
        val weeklyPrefs  = context.getSharedPreferences("weekly_routine",   Context.MODE_PRIVATE)
        val appPrefs     = context.getSharedPreferences("app_settings",     Context.MODE_PRIVATE)

        val entries = dayPrefs.all.values.mapNotNull { v ->
            if (v is String) try { gson.fromJson(v, DayEntry::class.java) } catch (e: Exception) { null }
            else null
        }

        val times: List<String> = try {
            gson.fromJson(
                appPrefs.getString("notification_times", "[]") ?: "[]",
                object : TypeToken<List<String>>() {}.type
            ) ?: emptyList()
        } catch (e: Exception) { emptyList() }

        val routine: List<RoutineTaskDef> = try {
            gson.fromJson(
                routinePrefs.getString("tasks", "[]") ?: "[]",
                object : TypeToken<List<RoutineTaskDef>>() {}.type
            ) ?: emptyList()
        } catch (e: Exception) { emptyList() }

        val weekly: List<WeeklyTaskDef> = try {
            gson.fromJson(
                weeklyPrefs.getString("tasks", "[]") ?: "[]",
                object : TypeToken<List<WeeklyTaskDef>>() {}.type
            ) ?: emptyList()
        } catch (e: Exception) { emptyList() }

        return gson.toJson(JsonObject().apply {
            addProperty("version",              1)
            addProperty("timetableName",        appPrefs.getString("timetable_name", "My Timetable") ?: "My Timetable")
            addProperty("notificationsEnabled", appPrefs.getBoolean("notification_enabled", false))
            add("notificationTimes", gson.toJsonTree(times))
            add("routine",           gson.toJsonTree(routine))
            add("weeklyTasks",       gson.toJsonTree(weekly))
            add("dayEntries",        gson.toJsonTree(entries))
        })
    }

    fun importJson(context: Context, json: String) {
        val obj = gson.fromJson(json, JsonObject::class.java)

        val dayPrefs     = context.getSharedPreferences("day_entries",     Context.MODE_PRIVATE)
        val routinePrefs = context.getSharedPreferences("routine_template", Context.MODE_PRIVATE)
        val weeklyPrefs  = context.getSharedPreferences("weekly_routine",   Context.MODE_PRIVATE)
        val appPrefs     = context.getSharedPreferences("app_settings",     Context.MODE_PRIVATE)

        appPrefs.edit()
            .putString ("timetable_name",      obj.get("timetableName")?.asString ?: "My Timetable")
            .putBoolean("notification_enabled", obj.get("notificationsEnabled")?.asBoolean ?: false)
            .putString ("notification_times",   gson.toJson(obj.get("notificationTimes") ?: gson.toJsonTree(emptyList<String>())))
            .apply()

        routinePrefs.edit()
            .putString("tasks", gson.toJson(obj.get("routine") ?: gson.toJsonTree(emptyList<RoutineTaskDef>())))
            .apply()

        weeklyPrefs.edit()
            .putString("tasks", gson.toJson(obj.get("weeklyTasks") ?: gson.toJsonTree(emptyList<WeeklyTaskDef>())))
            .apply()

        val entries: List<DayEntry> = try {
            gson.fromJson(obj.get("dayEntries"), object : TypeToken<List<DayEntry>>() {}.type) ?: emptyList()
        } catch (e: Exception) { emptyList() }

        dayPrefs.edit().clear().also { ed ->
            entries.forEach { entry -> if (entry.date.isNotBlank()) ed.putString(entry.date, gson.toJson(entry)) }
        }.apply()
    }
}
