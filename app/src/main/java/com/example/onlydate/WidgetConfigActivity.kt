package com.example.onlydate

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.ComponentName
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

        // Always load global settings
        loadSettings()

        // Set result OK immediately if it's a widget configuration
        if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            val resultValue = Intent()
            resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            setResult(RESULT_OK, resultValue)
        }

        // Setup listeners for instant updates
        setupListeners()
    }

    private fun setupListeners() {
        val textWatcher = object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                updateWidgets()
            }
        }

        textColorInput.addTextChangedListener(textWatcher)
        backgroundColorInput.addTextChangedListener(textWatcher)

        opacitySeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    updateWidgets()
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        showDayOfWeekSwitch.setOnCheckedChangeListener { _, _ ->
            updateWidgets()
        }
    }

    private fun updateWidgets() {
        val context: Context = this@WidgetConfigActivity
        saveSettings(context)

        // Update ALL widgets
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val componentName = ComponentName(context, DateWidgetProvider::class.java)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
        DateWidgetProvider.updateAppWidget(context, appWidgetManager, appWidgetIds)
    }

    private fun loadSettings() {
        val prefs = getSharedPreferences(PREFS_NAME, 0)
        textColorInput.setText(prefs.getString(PREF_TEXT_COLOR_KEY, "#FFFFFF") ?: "#FFFFFF")
        backgroundColorInput.setText(prefs.getString(PREF_BG_COLOR_KEY, "#000000") ?: "#000000")
        opacitySeekBar.progress = prefs.getInt(PREF_BG_OPACITY_KEY, 128)
        showDayOfWeekSwitch.isChecked = prefs.getBoolean(PREF_SHOW_DOW_KEY, true)
    }

    private fun saveSettings(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, 0).edit()
        prefs.putString(PREF_TEXT_COLOR_KEY, textColorInput.text.toString())
        prefs.putString(PREF_BG_COLOR_KEY, backgroundColorInput.text.toString())
        prefs.putInt(PREF_BG_OPACITY_KEY, opacitySeekBar.progress)
        prefs.putBoolean(PREF_SHOW_DOW_KEY, showDayOfWeekSwitch.isChecked)
        prefs.apply()
    }

    companion object {
        internal const val PREFS_NAME = "com.example.onlydate.DateWidgetProvider"
        internal const val PREF_TEXT_COLOR_KEY = "text_color"
        internal const val PREF_BG_COLOR_KEY = "bg_color"
        internal const val PREF_BG_OPACITY_KEY = "bg_opacity"
        internal const val PREF_SHOW_DOW_KEY = "show_dow"

        internal fun loadTextColor(context: Context): Int {
            val prefs = context.getSharedPreferences(PREFS_NAME, 0)
            val colorString = prefs.getString(PREF_TEXT_COLOR_KEY, "#FFFFFF") ?: "#FFFFFF"
            return try {
                Color.parseColor(colorString)
            } catch (e: IllegalArgumentException) {
                Color.WHITE
            }
        }

        internal fun loadBgColor(context: Context): Int {
            val prefs = context.getSharedPreferences(PREFS_NAME, 0)
            val colorString = prefs.getString(PREF_BG_COLOR_KEY, "#000000") ?: "#000000"
            return try {
                Color.parseColor(colorString)
            } catch (e: IllegalArgumentException) {
                Color.BLACK
            }
        }

        internal fun loadBgOpacity(context: Context): Int {
            val prefs = context.getSharedPreferences(PREFS_NAME, 0)
            return prefs.getInt(PREF_BG_OPACITY_KEY, 128)
        }

        internal fun loadShowDayOfWeek(context: Context): Boolean {
            val prefs = context.getSharedPreferences(PREFS_NAME, 0)
            return prefs.getBoolean(PREF_SHOW_DOW_KEY, true)
        }
    }
}