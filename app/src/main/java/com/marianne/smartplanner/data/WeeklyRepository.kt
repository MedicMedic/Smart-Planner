package com.marianne.smartplanner.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class WeeklyRepository(context: Context) {
    private val prefs = context.getSharedPreferences("weekly_routine", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val key = "tasks"

    fun save(tasks: List<WeeklyTaskDef>) {
        prefs.edit().putString(key, gson.toJson(tasks)).apply()
    }

    fun load(): List<WeeklyTaskDef> {
        val json = prefs.getString(key, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<WeeklyTaskDef>>() {}.type
            gson.fromJson(json, type)
        } catch (e: Exception) {
            emptyList()
        }
    }
}
