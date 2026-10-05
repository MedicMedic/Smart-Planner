package com.marianne.smartplanner

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Calendar

object NotificationScheduler {

    private const val PREFS       = "app_settings"
    private const val KEY_ENABLED = "notification_enabled"
    private const val KEY_TIMES   = "notification_times"
    private const val KEY_AUTO_OPEN = "auto_open_app"
    private const val MAX_SLOTS   = 20
    private val gson = Gson()

    fun schedule(context: Context, times: List<String>) {
        cancelAll(context)
        saveTimes(context, times)
        if (isEnabled(context)) times.forEachIndexed { i, t -> scheduleOne(context, i, t) }
    }

    fun setEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_ENABLED, enabled).apply()
        if (enabled) loadTimes(context).forEachIndexed { i, t -> scheduleOne(context, i, t) }
        else cancelAll(context)
    }

    fun scheduleOne(context: Context, index: Int, timeStr: String) {
        val parts = timeStr.split(":").mapNotNull { it.toIntOrNull() }
        if (parts.size < 2) return
        val now = Calendar.getInstance()
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, parts[0])
            set(Calendar.MINUTE, parts[1])
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (!after(now)) add(Calendar.DAY_OF_MONTH, 1)
        }
        val pi = pendingIntent(context, index, timeStr)
        val am = context.getSystemService(AlarmManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) {
            am.setWindow(AlarmManager.RTC_WAKEUP, cal.timeInMillis, 10 * 60 * 1000L, pi)
        } else {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pi)
        }
    }

    fun rescheduleAll(context: Context) {
        if (!isEnabled(context)) return
        loadTimes(context).forEachIndexed { i, t -> scheduleOne(context, i, t) }
    }

    fun cancelAll(context: Context) {
        val am = context.getSystemService(AlarmManager::class.java)
        for (i in 0 until MAX_SLOTS) am.cancel(pendingIntent(context, i, ""))
    }

    fun loadTimes(context: Context): List<String> {
        val json = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_TIMES, null) ?: return emptyList()
        return try {
            gson.fromJson(json, object : TypeToken<List<String>>() {}.type)
        } catch (e: Exception) { emptyList() }
    }

    fun setAutoOpen(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_AUTO_OPEN, enabled).apply()
    }

    fun isAutoOpen(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_AUTO_OPEN, false)

    fun isEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_ENABLED, false)

    private fun saveTimes(context: Context, times: List<String>) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_TIMES, gson.toJson(times)).apply()
    }

    private fun pendingIntent(context: Context, index: Int, timeStr: String): PendingIntent {
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            action = "com.marianne.smartplanner.NOTIFY"
            putExtra("index", index)
            putExtra("time", timeStr)
        }
        return PendingIntent.getBroadcast(
            context, index, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
