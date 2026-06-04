package com.marianne.smartplanner.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class RoutineRepository(context: Context) {

    private val prefs = context.getSharedPreferences("routine_template", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val key = "tasks"

    fun save(tasks: List<RoutineTaskDef>) {
        prefs.edit().putString(key, gson.toJson(tasks)).apply()
    }

    fun load(): List<RoutineTaskDef> {
        val json = prefs.getString(key, null) ?: return DEFAULT_ROUTINE_TASKS
        return try {
            val type = object : TypeToken<List<RoutineTaskDef>>() {}.type
            gson.fromJson<List<RoutineTaskDef>>(json, type).ifEmpty { DEFAULT_ROUTINE_TASKS }
        } catch (e: Exception) {
            DEFAULT_ROUTINE_TASKS
        }
    }
}
