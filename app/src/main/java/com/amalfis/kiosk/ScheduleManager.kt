package com.amalfis.kiosk

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import java.util.Calendar

object ScheduleManager {
    private const val TAG = "ScheduleManager"
    const val ACTION_POWER_OFF = "com.amalfis.kiosk.ACTION_POWER_OFF"
    const val ACTION_POWER_ON = "com.amalfis.kiosk.ACTION_POWER_ON"

    fun updateAlarms(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        cancelAlarm(context, ACTION_POWER_OFF)
        cancelAlarm(context, ACTION_POWER_ON)

        if (!SchedulePrefs.isEnabled(context)) {
            Log.d(TAG, "Temporizador desativado.")
            return
        }

        val days = SchedulePrefs.getDays(context)
        val (onHour, onMin) = SchedulePrefs.getPowerOnTime(context)
        val (offHour, offMin) = SchedulePrefs.getPowerOffTime(context)

        scheduleNextAlarm(context, alarmManager, ACTION_POWER_ON, onHour, onMin, days)
        scheduleNextAlarm(context, alarmManager, ACTION_POWER_OFF, offHour, offMin, days)
    }

    private fun cancelAlarm(context: Context, action: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ScheduleReceiver::class.java).apply { this.action = action }
        val requestCode = if (action == ACTION_POWER_ON) 1001 else 1002
        val pendingIntent = PendingIntent.getBroadcast(
            context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    private fun scheduleNextAlarm(
        context: Context,
        alarmManager: AlarmManager,
        action: String,
        hour: Int,
        minute: Int,
        allowedDays: Set<Int>
    ) {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val now = Calendar.getInstance()
        if (calendar.timeInMillis <= now.timeInMillis || !allowedDays.contains(calendar.get(Calendar.DAY_OF_WEEK))) {
            for (i in 1..7) {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
                if (allowedDays.contains(calendar.get(Calendar.DAY_OF_WEEK))) {
                    calendar.set(Calendar.HOUR_OF_DAY, hour)
                    calendar.set(Calendar.MINUTE, minute)
                    calendar.set(Calendar.SECOND, 0)
                    calendar.set(Calendar.MILLISECOND, 0)
                    if (calendar.timeInMillis > now.timeInMillis) {
                        break
                    }
                }
            }
        }

        val intent = Intent(context, ScheduleReceiver::class.java).apply { this.action = action }
        val requestCode = if (action == ACTION_POWER_ON) 1001 else 1002
        val pendingIntent = PendingIntent.getBroadcast(
            context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }
            Log.d(TAG, "Alarme agendado para $action em ${calendar.time}")
        } catch (e: SecurityException) {
            Log.e(TAG, "Sem permissão para alarme exato, usando alarme normal", e)
            try {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            } catch (ex: Exception) {
                Log.e(TAG, "Falha ao agendar alarme fallback", ex)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Falha ao agendar alarme para $action", e)
        }
    }
}
