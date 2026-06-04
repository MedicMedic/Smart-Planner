package com.marianne.smartplanner

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat

class NotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED -> NotificationScheduler.rescheduleAll(context)
            else -> {
                val index = intent.getIntExtra("index", 0)
                val time  = intent.getStringExtra("time") ?: return
                showNotification(context, index)
                NotificationScheduler.scheduleOne(context, index, time)
            }
        }
    }

    private fun showNotification(context: Context, id: Int) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel("planner_reminders", "Daily Reminders", NotificationManager.IMPORTANCE_DEFAULT)
        )
        val openApp = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_SINGLE_TOP },
            PendingIntent.FLAG_IMMUTABLE
        )
        nm.notify(
            id,
            NotificationCompat.Builder(context, "planner_reminders")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(context.getString(R.string.app_name))
                .setContentText("Log your day!")
                .setContentIntent(openApp)
                .setAutoCancel(true)
                .build()
        )
    }
}
