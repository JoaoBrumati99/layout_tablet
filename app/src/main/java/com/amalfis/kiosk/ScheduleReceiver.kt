package com.amalfis.kiosk

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.admin.DevicePolicyManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

class ScheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action
        Log.d("ScheduleReceiver", "onReceive: action = $action")

        when (action) {
            ScheduleManager.ACTION_POWER_OFF -> {
                try {
                    val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
                    if (dpm.isDeviceOwnerApp(context.packageName)) {
                        dpm.lockNow()
                    }
                } catch (e: Exception) {
                    Log.e("ScheduleReceiver", "Erro ao executar bloqueio agendado", e)
                }
            }
            ScheduleManager.ACTION_POWER_ON, Intent.ACTION_BOOT_COMPLETED, "android.intent.action.QUICKBOOT_POWERON" -> {
                try {
                    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    val channelId = "amalfis_power_on_channel"

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        val channel = NotificationChannel(
                            channelId,
                            "Temporizador de Energia",
                            NotificationManager.IMPORTANCE_HIGH
                        ).apply {
                            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                            setSound(null, null)
                        }
                        notificationManager.createNotificationChannel(channel)
                    }

                    val launchIntent = Intent(context, MainActivity::class.java).apply {
                        addFlags(
                            Intent.FLAG_ACTIVITY_NEW_TASK or
                                    Intent.FLAG_ACTIVITY_CLEAR_TASK or
                                    Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT
                        )
                    }
                    val pendingIntent = PendingIntent.getActivity(
                        context, 0, launchIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )

                    val notificationBuilder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        Notification.Builder(context, channelId)
                    } else {
                        @Suppress("DEPRECATION")
                        Notification.Builder(context)
                    }.apply {
                        setSmallIcon(R.drawable.ic_restart)
                        setContentTitle("Amalfis Acesso")
                        setContentText("Ligando dispositivo...")
                        setCategory(Notification.CATEGORY_ALARM)
                        setFullScreenIntent(pendingIntent, true)
                        setContentIntent(pendingIntent)
                        setAutoCancel(true)
                    }

                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                        @Suppress("DEPRECATION")
                        notificationBuilder.setPriority(Notification.PRIORITY_HIGH)
                    }

                    notificationManager.notify(2001, notificationBuilder.build())
                } catch (e: Exception) {
                    Log.e("ScheduleReceiver", "Erro ao acender tela com FullScreenIntent", e)
                }
            }
        }

        ScheduleManager.updateAlarms(context)
    }
}
