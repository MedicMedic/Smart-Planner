package com.marianne.smartplanner

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.core.app.NotificationCompat

class NotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED -> NotificationScheduler.rescheduleAll(context)
            else -> {
                val index = intent.getIntExtra("index", 0)
                val time  = intent.getStringExtra("time") ?: return
                showNotification(context, index)
                // "Display over other apps" exempts us from background-launch limits.
                if (NotificationScheduler.isAutoOpen(context) && Settings.canDrawOverlays(context)) {
                    context.startActivity(
                        Intent(context, MainActivity::class.java)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    )
                }
                NotificationScheduler.scheduleOne(context, index, time)
            }
        }
    }

    private fun showNotification(context: Context, id: Int) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        // Channel importance can't be raised once created, so the high-priority
        // channel needed for full-screen launch gets a new id.
        nm.createNotificationChannel(
            NotificationChannel("planner_alarm", "Scheduled Reminders", NotificationManager.IMPORTANCE_HIGH)
        )
        val openApp = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_IMMUTABLE
        )
        nm.notify(
            id,
            NotificationCompat.Builder(context, "planner_alarm")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(context.getString(R.string.app_name))
                .setContentText("Log your day!")
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(openApp)
                .setFullScreenIntent(openApp, true)
                .setAutoCancel(true)
                .build()
        )
    }
}
