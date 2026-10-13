package com.amalfis.kiosk

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent

/**
 * Necessário para o app virar "Device Owner" e poder usar o modo kiosk (Lock Task)
 * de forma completa, bloqueando Home e Recentes.
 * Não precisa fazer nada aqui além de existir e estar registrado no Manifest.
 */
class AdminReceiver : DeviceAdminReceiver() {

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
    }
}