package com.marianne.smartplanner.data

import android.content.Context
import com.google.gson.Gson
import com.marianne.smartplanner.widget.WidgetRefresher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class DayRepository(private val context: Context) {

    private val prefs = context.getSharedPreferences("day_entries", Context.MODE_PRIVATE)
    private val gson  = Gson()
    // The app is in the foreground when it saves, so launching the (now suspend)
    // refresh here is safe — the process won't be frozen mid-update.
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun save(entry: DayEntry, updateChecklist: Boolean = false, updateGoals: Boolean = false) {
        prefs.edit().putString(entry.date, gson.toJson(entry)).apply()
        if (updateChecklist || updateGoals) {
            scope.launch {
                if (updateChecklist) WidgetRefresher.refreshChecklist(context)
                if (updateGoals)     WidgetRefresher.refreshGoals(context)
            }
        }
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
