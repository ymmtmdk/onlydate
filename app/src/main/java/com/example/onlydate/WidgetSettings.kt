package com.example.onlydate

import android.content.Context
import android.graphics.Color

object WidgetSettings {
    internal const val PREFS_NAME = "com.example.onlydate.DateWidgetProvider"
    internal const val PREF_TEXT_COLOR_KEY = "text_color"
    internal const val PREF_BG_COLOR_KEY = "bg_color"
    internal const val PREF_BG_OPACITY_KEY = "bg_opacity"
    internal const val PREF_SHOW_YEAR_KEY = "show_year"
    internal const val PREF_SHOW_DOW_KEY = "show_dow"
    internal const val PREF_SHOW_TEMP_KEY = "show_temp"
    internal const val PREF_DATE_SIZE_KEY = "date_size"
    internal const val PREF_DAY_SIZE_KEY = "day_size"
    internal const val PREF_TEMP_SIZE_KEY = "temp_size"
    internal const val PREF_LANGUAGE_KEY = "language"
    internal const val PREF_LAYOUT_TYPE_KEY = "layout_type"
    internal const val PREF_LAST_TEMP_KEY = "last_temp"

    internal const val LANG_SYSTEM = "system"
    internal const val LANG_ENGLISH = "english"
    internal const val LANG_JAPANESE = "japanese"

    internal const val LAYOUT_HORIZONTAL = 0
    internal const val LAYOUT_TWO_ROWS_TEMP = 1
    internal const val LAYOUT_TWO_ROWS_WEEKDAY_TEMP = 2
    internal const val LAYOUT_THREE_ROWS = 3

    internal const val MIN_TEXT_SIZE = 8
    internal const val DEFAULT_TEXT_COLOR = "#FFFFFF"
    internal const val DEFAULT_BG_COLOR = "#000000"
    internal const val DEFAULT_BG_OPACITY = 128
    internal const val DEFAULT_DATE_SIZE = 24
    internal const val DEFAULT_DAY_SIZE = 16
    internal const val DEFAULT_TEMP_SIZE = 16
    internal const val DEFAULT_SHOW_YEAR = true
    internal const val DEFAULT_SHOW_DOW = true
    internal const val DEFAULT_SHOW_TEMP = false

    internal fun loadTextColor(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val colorString = prefs.getString(PREF_TEXT_COLOR_KEY, DEFAULT_TEXT_COLOR) ?: DEFAULT_TEXT_COLOR
        return try {
            Color.parseColor(colorString)
        } catch (e: IllegalArgumentException) {
            Color.WHITE
        }
    }

    internal fun loadBgColor(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val colorString = prefs.getString(PREF_BG_COLOR_KEY, DEFAULT_BG_COLOR) ?: DEFAULT_BG_COLOR
        return try {
            Color.parseColor(colorString)
        } catch (e: IllegalArgumentException) {
            Color.BLACK
        }
    }

    internal fun loadBgOpacity(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(PREF_BG_OPACITY_KEY, DEFAULT_BG_OPACITY)
    }

    internal fun loadShowYear(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(PREF_SHOW_YEAR_KEY, DEFAULT_SHOW_YEAR)
    }

    internal fun loadShowDayOfWeek(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(PREF_SHOW_DOW_KEY, DEFAULT_SHOW_DOW)
    }

    internal fun loadShowTemp(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(PREF_SHOW_TEMP_KEY, DEFAULT_SHOW_TEMP)
    }

    internal fun loadLanguage(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(PREF_LANGUAGE_KEY, LANG_SYSTEM) ?: LANG_SYSTEM
    }

    internal fun loadDateSize(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(PREF_DATE_SIZE_KEY, DEFAULT_DATE_SIZE)
    }

    internal fun loadDaySize(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(PREF_DAY_SIZE_KEY, DEFAULT_DAY_SIZE)
    }

    internal fun loadTempSize(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(PREF_TEMP_SIZE_KEY, DEFAULT_TEMP_SIZE)
    }

    internal fun loadLayoutType(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(PREF_LAYOUT_TYPE_KEY, LAYOUT_HORIZONTAL)
    }

    internal fun saveLastTemp(context: Context, temp: Double) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
        prefs.putFloat(PREF_LAST_TEMP_KEY, temp.toFloat())
        prefs.apply()
    }

    internal fun loadLastTemp(context: Context): Double? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (!prefs.contains(PREF_LAST_TEMP_KEY)) return null
        return prefs.getFloat(PREF_LAST_TEMP_KEY, 0f).toDouble()
    }
}
