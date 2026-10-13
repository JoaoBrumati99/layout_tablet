package com.amalfis.kiosk

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo

/**
 * Controla se o app pode girar a tela livremente (seguindo o sensor)
 * ou se fica travado em paisagem (comportamento padrão do kiosk).
 */
object ScreenPrefs {

    private const val PREFS_NAME = "amalfis_kiosk_prefs"
    private const val KEY_ROTATION_ENABLED = "screen_rotation_enabled"

    fun isRotationEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_ROTATION_ENABLED, false)
    }

    fun setRotationEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_ROTATION_ENABLED, enabled).apply()
    }

    /**
     * Aplica a orientação atual na activity. Chame antes ou logo após setContentView.
     * Quando a rotação está desativada, a tela fica travada em paisagem (padrão original).
     * Quando ativada, a tela acompanha o sensor do tablet (paisagem e retrato).
     */
    fun applyOrientation(activity: Activity) {
        activity.requestedOrientation = if (isRotationEnabled(activity)) {
            ActivityInfo.SCREEN_ORIENTATION_SENSOR
        } else {
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }
    }
}
