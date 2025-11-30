package com.example.onlydate

import android.app.Activity
import android.appwidget.AppWidgetManager

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat

class WidgetConfigActivity : Activity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    private lateinit var textColorInput: EditText
    private lateinit var backgroundColorInput: EditText
    private lateinit var opacitySeekBar: SeekBar
    private lateinit var dateSizeSeekBar: SeekBar
    private lateinit var daySizeSeekBar: SeekBar
    private lateinit var tempSizeSeekBar: SeekBar
    private lateinit var dateSizeLabel: TextView
    private lateinit var daySizeLabel: TextView
    private lateinit var tempSizeLabel: TextView
    private lateinit var showYearSwitch: SwitchCompat
    private lateinit var showDayOfWeekSwitch: SwitchCompat
    private lateinit var showTemperatureSwitch: SwitchCompat
    private lateinit var languageRadioGroup: RadioGroup
    private lateinit var layoutRadioGroup: RadioGroup

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_widget_config)
        setResult(RESULT_CANCELED)

        textColorInput = findViewById(R.id.text_color_input)
        backgroundColorInput = findViewById(R.id.background_color_input)
        opacitySeekBar = findViewById(R.id.opacity_seekbar)
        dateSizeSeekBar = findViewById(R.id.date_size_seekbar)
        daySizeSeekBar = findViewById(R.id.day_size_seekbar)
        tempSizeSeekBar = findViewById(R.id.temp_size_seekbar)
        dateSizeLabel = findViewById(R.id.date_size_label)
        daySizeLabel = findViewById(R.id.day_size_label)
        tempSizeLabel = findViewById(R.id.temp_size_label)
        showYearSwitch = findViewById(R.id.show_year_switch)
        showDayOfWeekSwitch = findViewById(R.id.show_day_of_week_switch)
        showTemperatureSwitch = findViewById(R.id.show_temperature_switch)
        languageRadioGroup = findViewById(R.id.language_radio_group)
        layoutRadioGroup = findViewById(R.id.layout_radio_group)

        val intent = intent
        val extras = intent.extras
        if (extras != null) {
            appWidgetId = extras.getInt(
                AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID
            )
        }

        // Always load global settings
        loadSettings()

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

        dateSizeSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val size = progress + 8
                dateSizeLabel.text = "$size sp"
                if (fromUser) {
                    updateWidgets()
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        daySizeSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val size = progress + 8
                daySizeLabel.text = "$size sp"
                if (fromUser) {
                    updateWidgets()
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        tempSizeSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val size = progress + 8
                tempSizeLabel.text = "$size sp"
                if (fromUser) {
                    updateWidgets()
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        showYearSwitch.setOnCheckedChangeListener { _, _ -> updateWidgets() }
        showDayOfWeekSwitch.setOnCheckedChangeListener { _, _ -> updateWidgets() }
        showTemperatureSwitch.setOnCheckedChangeListener { _, _ -> updateWidgets() }
        languageRadioGroup.setOnCheckedChangeListener { _, _ -> updateWidgets() }
        layoutRadioGroup.setOnCheckedChangeListener { _, _ -> updateWidgets() }
    }

    private fun updateWidgets() {
        val context: Context = this@WidgetConfigActivity
        saveSettings(context)

        // Update ALL widgets with fresh temperature data
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val componentName = ComponentName(context, DateWidgetProvider::class.java)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
        
        // Fetch temperature in background if enabled
        val showTemp = WidgetConfigActivity.loadShowTemp(context)
        if (showTemp) {
            Thread {
                try {
                    val temp = DateWidgetProvider.fetchTemperaturePublic(context)
                    DateWidgetProvider.updateAppWidget(context, appWidgetManager, appWidgetIds, temp)
                    if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                        DateWidgetProvider.updateAppWidget(context, appWidgetManager, appWidgetId, temp)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    DateWidgetProvider.updateAppWidget(context, appWidgetManager, appWidgetIds)
                    if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                        DateWidgetProvider.updateAppWidget(context, appWidgetManager, appWidgetId)
                    }
                }
            }.start()
        } else {
            DateWidgetProvider.updateAppWidget(context, appWidgetManager, appWidgetIds)
            if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                DateWidgetProvider.updateAppWidget(context, appWidgetManager, appWidgetId)
            }
        }
    }

    private fun loadSettings() {
        val prefs = getSharedPreferences(PREFS_NAME, 0)
        textColorInput.setText(prefs.getString(PREF_TEXT_COLOR_KEY, "#FFFFFF") ?: "#FFFFFF")
        backgroundColorInput.setText(prefs.getString(PREF_BG_COLOR_KEY, "#000000") ?: "#000000")
        opacitySeekBar.progress = prefs.getInt(PREF_BG_OPACITY_KEY, 128)
        
        val dateSize = prefs.getInt(PREF_DATE_SIZE_KEY, 24)
        val daySize = prefs.getInt(PREF_DAY_SIZE_KEY, 16)
        val tempSize = prefs.getInt(PREF_TEMP_SIZE_KEY, 16)
        
        dateSizeSeekBar.progress = dateSize - 8
        daySizeSeekBar.progress = daySize - 8
        tempSizeSeekBar.progress = tempSize - 8
        
        dateSizeLabel.text = "$dateSize sp"
        daySizeLabel.text = "$daySize sp"
        tempSizeLabel.text = "$tempSize sp"
        
        showYearSwitch.isChecked = prefs.getBoolean(PREF_SHOW_YEAR_KEY, true)
        showDayOfWeekSwitch.isChecked = prefs.getBoolean(PREF_SHOW_DOW_KEY, true)
        showTemperatureSwitch.isChecked = prefs.getBoolean(PREF_SHOW_TEMP_KEY, false)

        val language = prefs.getString(PREF_LANGUAGE_KEY, LANG_SYSTEM) ?: LANG_SYSTEM
        when (language) {
            LANG_ENGLISH -> languageRadioGroup.check(R.id.lang_english)
            LANG_JAPANESE -> languageRadioGroup.check(R.id.lang_japanese)
            else -> languageRadioGroup.check(R.id.lang_system)
        }

        val layoutType = prefs.getInt(PREF_LAYOUT_TYPE_KEY, LAYOUT_HORIZONTAL)
        when (layoutType) {
            LAYOUT_HORIZONTAL -> layoutRadioGroup.check(R.id.layout_horizontal)
            LAYOUT_TWO_ROWS_TEMP -> layoutRadioGroup.check(R.id.layout_two_rows_temp)
            LAYOUT_TWO_ROWS_WEEKDAY_TEMP -> layoutRadioGroup.check(R.id.layout_two_rows_weekday_temp)
            LAYOUT_THREE_ROWS -> layoutRadioGroup.check(R.id.layout_three_rows)
            else -> layoutRadioGroup.check(R.id.layout_horizontal)
        }
    }

    private fun saveSettings(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, 0).edit()
        prefs.putString(PREF_TEXT_COLOR_KEY, textColorInput.text.toString())
        prefs.putString(PREF_BG_COLOR_KEY, backgroundColorInput.text.toString())
        prefs.putInt(PREF_BG_OPACITY_KEY, opacitySeekBar.progress)
        prefs.putInt(PREF_DATE_SIZE_KEY, dateSizeSeekBar.progress + 8)
        prefs.putInt(PREF_DAY_SIZE_KEY, daySizeSeekBar.progress + 8)
        prefs.putInt(PREF_TEMP_SIZE_KEY, tempSizeSeekBar.progress + 8)
        prefs.putBoolean(PREF_SHOW_YEAR_KEY, showYearSwitch.isChecked)
        prefs.putBoolean(PREF_SHOW_DOW_KEY, showDayOfWeekSwitch.isChecked)
        prefs.putBoolean(PREF_SHOW_TEMP_KEY, showTemperatureSwitch.isChecked)

        val language = when (languageRadioGroup.checkedRadioButtonId) {
            R.id.lang_english -> LANG_ENGLISH
            R.id.lang_japanese -> LANG_JAPANESE
            else -> LANG_SYSTEM
        }
        prefs.putString(PREF_LANGUAGE_KEY, language)

        val layoutType = when (layoutRadioGroup.checkedRadioButtonId) {
            R.id.layout_horizontal -> LAYOUT_HORIZONTAL
            R.id.layout_two_rows_temp -> LAYOUT_TWO_ROWS_TEMP
            R.id.layout_two_rows_weekday_temp -> LAYOUT_TWO_ROWS_WEEKDAY_TEMP
            R.id.layout_three_rows -> LAYOUT_THREE_ROWS
            else -> LAYOUT_HORIZONTAL
        }
        prefs.putInt(PREF_LAYOUT_TYPE_KEY, layoutType)

        prefs.apply()
    }

    companion object {
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

        internal const val LANG_SYSTEM = "system"
        internal const val LANG_ENGLISH = "english"
        internal const val LANG_JAPANESE = "japanese"

        internal const val LAYOUT_HORIZONTAL = 0
        internal const val LAYOUT_TWO_ROWS_TEMP = 1
        internal const val LAYOUT_TWO_ROWS_WEEKDAY_TEMP = 2
        internal const val LAYOUT_THREE_ROWS = 3

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

        internal fun loadShowYear(context: Context): Boolean {
            val prefs = context.getSharedPreferences(PREFS_NAME, 0)
            return prefs.getBoolean(PREF_SHOW_YEAR_KEY, true)
        }

        internal fun loadShowDayOfWeek(context: Context): Boolean {
            val prefs = context.getSharedPreferences(PREFS_NAME, 0)
            return prefs.getBoolean(PREF_SHOW_DOW_KEY, true)
        }

        internal fun loadShowTemp(context: Context): Boolean {
            val prefs = context.getSharedPreferences(PREFS_NAME, 0)
            return prefs.getBoolean(PREF_SHOW_TEMP_KEY, false)
        }

        internal fun loadLanguage(context: Context): String {
            val prefs = context.getSharedPreferences(PREFS_NAME, 0)
            return prefs.getString(PREF_LANGUAGE_KEY, LANG_SYSTEM) ?: LANG_SYSTEM
        }

        internal fun loadDateSize(context: Context): Int {
            val prefs = context.getSharedPreferences(PREFS_NAME, 0)
            return prefs.getInt(PREF_DATE_SIZE_KEY, 24)
        }

        internal fun loadDaySize(context: Context): Int {
            val prefs = context.getSharedPreferences(PREFS_NAME, 0)
            return prefs.getInt(PREF_DAY_SIZE_KEY, 16)
        }

        internal fun loadTempSize(context: Context): Int {
            val prefs = context.getSharedPreferences(PREFS_NAME, 0)
            return prefs.getInt(PREF_TEMP_SIZE_KEY, 16)
        }

        internal fun loadLayoutType(context: Context): Int {
            val prefs = context.getSharedPreferences(PREFS_NAME, 0)
            return prefs.getInt(PREF_LAYOUT_TYPE_KEY, LAYOUT_HORIZONTAL)
        }
    }
}