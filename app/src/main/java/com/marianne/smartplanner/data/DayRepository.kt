package com.marianne.smartplanner.data

import android.content.Context
import com.google.gson.Gson

class DayRepository(context: Context) {

    private val prefs = context.getSharedPreferences("day_entries", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun save(entry: DayEntry) {
        prefs.edit().putString(entry.date, gson.toJson(entry)).apply()
    }

    fun load(date: String): DayEntry? {
        val json = prefs.getString(date, null) ?: return null
        return try {
            gson.fromJson(json, DayEntry::class.java)
        } catch (e: Exception) {
            null
        }
    }

}
