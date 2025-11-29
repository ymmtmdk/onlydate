package com.example.onlydate

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.SeekBar
import androidx.appcompat.widget.SwitchCompat

class WidgetConfigActivity : Activity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    private lateinit var textColorInput: EditText
    private lateinit var backgroundColorInput: EditText
    private lateinit var opacitySeekBar: SeekBar
    private lateinit var showDayOfWeekSwitch: SwitchCompat

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_widget_config)
        setResult(RESULT_CANCELED)

        textColorInput = findViewById(R.id.text_color_input)
        backgroundColorInput = findViewById(R.id.background_color_input)
        opacitySeekBar = findViewById(R.id.opacity_seekbar)
        showDayOfWeekSwitch = findViewById(R.id.show_day_of_week_switch)

        val intent = intent
        val extras = intent.extras
        if (extras != null) {
            appWidgetId = extras.getInt(
                AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID
            )
        }

        val isWidgetConfigure = appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID

        if (isWidgetConfigure) {
            // Widget configuration flow
            loadSettings()
            findViewById<Button>(R.id.save_button).setOnClickListener {
                val context: Context = this@WidgetConfigActivity
                saveSettings(context, appWidgetId)
                val appWidgetManager = AppWidgetManager.getInstance(context)
                DateWidgetProvider.updateAppWidget(context, appWidgetManager, appWidgetId)
                val resultValue = Intent()
                resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                setResult(RESULT_OK, resultValue)
                finish()
            }
        } else {
            // App launcher flow
            // The UI will show default values. The "Save" button will just close the activity.
            findViewById<Button>(R.id.save_button).setOnClickListener {
                finish()
            }
        }
    }

    private fun loadSettings() {
        val prefs = getSharedPreferences(PREFS_NAME, 0)
        textColorInput.setText(prefs.getString(PREF_TEXT_COLOR_KEY + appWidgetId, "#FFFFFF") ?: "#FFFFFF")
        backgroundColorInput.setText(prefs.getString(PREF_BG_COLOR_KEY + appWidgetId, "#000000") ?: "#000000")
        opacitySeekBar.progress = prefs.getInt(PREF_BG_OPACITY_KEY + appWidgetId, 128)
        showDayOfWeekSwitch.isChecked = prefs.getBoolean(PREF_SHOW_DOW_KEY + appWidgetId, true)
    }

    private fun saveSettings(context: Context, appWidgetId: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, 0).edit()
        prefs.putString(PREF_TEXT_COLOR_KEY + appWidgetId, textColorInput.text.toString())
        prefs.putString(PREF_BG_COLOR_KEY + appWidgetId, backgroundColorInput.text.toString())
        prefs.putInt(PREF_BG_OPACITY_KEY + appWidgetId, opacitySeekBar.progress)
        prefs.putBoolean(PREF_SHOW_DOW_KEY + appWidgetId, showDayOfWeekSwitch.isChecked)
        prefs.apply()
    }

    companion object {
        internal const val PREFS_NAME = "com.example.onlydate.DateWidgetProvider"
        internal const val PREF_TEXT_COLOR_KEY = "text_color_"
        internal const val PREF_BG_COLOR_KEY = "bg_color_"
        internal const val PREF_BG_OPACITY_KEY = "bg_opacity_"
        internal const val PREF_SHOW_DOW_KEY = "show_dow_"

        internal fun loadTextColor(context: Context, appWidgetId: Int): Int {
            val prefs = context.getSharedPreferences(PREFS_NAME, 0)
            val colorString = prefs.getString(PREF_TEXT_COLOR_KEY + appWidgetId, "#FFFFFF") ?: "#FFFFFF"
            return try {
                Color.parseColor(colorString)
            } catch (e: IllegalArgumentException) {
                Color.WHITE
            }
        }

        internal fun loadBgColor(context: Context, appWidgetId: Int): Int {
            val prefs = context.getSharedPreferences(PREFS_NAME, 0)
            val colorString = prefs.getString(PREF_BG_COLOR_KEY + appWidgetId, "#000000") ?: "#000000"
            return try {
                Color.parseColor(colorString)
            } catch (e: IllegalArgumentException) {
                Color.BLACK
            }
        }

        internal fun loadBgOpacity(context: Context, appWidgetId: Int): Int {
            val prefs = context.getSharedPreferences(PREFS_NAME, 0)
            return prefs.getInt(PREF_BG_OPACITY_KEY + appWidgetId, 128)
        }

        internal fun loadShowDayOfWeek(context: Context, appWidgetId: Int): Boolean {
            val prefs = context.getSharedPreferences(PREFS_NAME, 0)
            return prefs.getBoolean(PREF_SHOW_DOW_KEY + appWidgetId, true)
        }
    }
}