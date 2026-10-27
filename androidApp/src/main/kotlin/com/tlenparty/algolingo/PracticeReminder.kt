package com.tlenparty.algolingo

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Daily "time to practice" reminder, fired 24h after the last app launch and then every day. */
class PracticeReminder : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        schedule(context)
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL, "Напоминания о тренировке", NotificationManager.IMPORTANCE_DEFAULT)
        )
        val open = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
        )
        nm.notify(
            1,
            Notification.Builder(context, CHANNEL)
                .setSmallIcon(R.drawable.ic_launcher_monochrome)
                .setColor(0xFF58CC02.toInt())
                .setContentTitle("Пора потренироваться 🧠")
                .setContentText("Пара задач — 5 минут, без клавиатуры. Стрик сам себя не продлит!")
                .setContentIntent(open)
                .setAutoCancel(true)
                .build()
        )
    }

    companion object {
        private const val CHANNEL = "practice"

        // ponytail: alarms are wiped on reboot; the next app launch re-arms them. Add a BOOT_COMPLETED receiver if that matters.
        fun schedule(context: Context) {
            val pi = PendingIntent.getBroadcast(
                context, 0, Intent(context, PracticeReminder::class.java),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            // One-shot with a 1h window re-armed from onReceive: setInexactRepeating may slip by up to 18h.
            context.getSystemService(AlarmManager::class.java).setWindow(
                AlarmManager.RTC, System.currentTimeMillis() + AlarmManager.INTERVAL_DAY, AlarmManager.INTERVAL_HOUR, pi
            )
        }
    }
}
