package com.amalfis.kiosk

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {

    private val actionsAceitas = setOf(
        Intent.ACTION_BOOT_COMPLETED,
        "android.intent.action.QUICKBOOT_POWERON",
        "com.htc.intent.action.QUICKBOOT_POWERON"
    )

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action in actionsAceitas) {
            // Reagenda os alarmes do temporizador assim que o dispositivo liga
            ScheduleManager.updateAlarms(context)

            val launchIntent = Intent(context, MainActivity::class.java).apply {
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TASK
                )
            }
            context.startActivity(launchIntent)
        }
    }
}
