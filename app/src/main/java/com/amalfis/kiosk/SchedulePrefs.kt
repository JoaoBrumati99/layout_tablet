package com.amalfis.kiosk

import android.content.Context
import java.util.Calendar

object SchedulePrefs {
    private const val PREFS_NAME = "amalfis_schedule_prefs"
    private const val KEY_ENABLED = "schedule_enabled"
    private const val KEY_DAYS = "schedule_days"
    private const val KEY_ON_HOUR = "power_on_hour"
    private const val KEY_ON_MINUTE = "power_on_minute"
    private const val KEY_OFF_HOUR = "power_off_hour"
    private const val KEY_OFF_MINUTE = "power_off_minute"

    fun isEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_ENABLED, false)
    }

    fun setEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    fun getDays(context: Context): Set<Int> {
        val str = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_DAYS, "2,3,4,5,6")
        if (str.isNullOrEmpty()) return setOf(Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY)
        return str.split(",").mapNotNull { it.toIntOrNull() }.toSet()
    }

    fun setDays(context: Context, days: Set<Int>) {
        val str = days.joinToString(",")
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_DAYS, str).apply()
    }

    fun getPowerOnTime(context: Context): Pair<Int, Int> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val hour = prefs.getInt(KEY_ON_HOUR, 7)
        val minute = prefs.getInt(KEY_ON_MINUTE, 0)
        return Pair(hour, minute)
    }

    fun setPowerOnTime(context: Context, hour: Int, minute: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putInt(KEY_ON_HOUR, hour).putInt(KEY_ON_MINUTE, minute).apply()
    }

    fun getPowerOffTime(context: Context): Pair<Int, Int> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val hour = prefs.getInt(KEY_OFF_HOUR, 18)
        val minute = prefs.getInt(KEY_OFF_MINUTE, 0)
        return Pair(hour, minute)
    }

    fun setPowerOffTime(context: Context, hour: Int, minute: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putInt(KEY_OFF_HOUR, hour).putInt(KEY_OFF_MINUTE, minute).apply()
    }
}
