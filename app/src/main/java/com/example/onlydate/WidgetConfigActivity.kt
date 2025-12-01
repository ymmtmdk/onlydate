package com.example.onlydate

import android.app.Activity
import android.appwidget.AppWidgetManager

import android.app.AlarmManager
import android.app.AlertDialog
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import android.net.Uri

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
    private lateinit var logView: TextView

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
        logView = findViewById(R.id.log_view)

        Logger.setListener { logs ->
            runOnUiThread {
                logView.text = logs
            }
        }

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

        // Check for exact alarm permission on Android 12+
        checkExactAlarmPermission()
    }

    override fun onDestroy() {
        super.onDestroy()
        Logger.removeListener()
    }

    override fun onResume() {
        super.onResume()
        Logger.d(TAG, "App icon tapped / Activity started")
        updateWidgets()
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
        
        // Add focus change listeners for validation
        textColorInput.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                validateAndCorrectColorInput(textColorInput, WidgetSettings.DEFAULT_TEXT_COLOR, "text color")
            }
        }
        
        backgroundColorInput.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                validateAndCorrectColorInput(backgroundColorInput, WidgetSettings.DEFAULT_BG_COLOR, "background color")
            }
        }

        opacitySeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    updateWidgets()
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        setupSeekBarListener(dateSizeSeekBar, dateSizeLabel)
        setupSeekBarListener(daySizeSeekBar, daySizeLabel)
        setupSeekBarListener(tempSizeSeekBar, tempSizeLabel)

        showYearSwitch.setOnCheckedChangeListener { _, _ -> updateWidgets() }
        showDayOfWeekSwitch.setOnCheckedChangeListener { _, _ -> updateWidgets() }
        showTemperatureSwitch.setOnCheckedChangeListener { _, _ -> updateWidgets() }
        languageRadioGroup.setOnCheckedChangeListener { _, _ -> updateWidgets() }
        layoutRadioGroup.setOnCheckedChangeListener { _, _ -> updateWidgets() }
    }
    
    private fun validateAndCorrectColorInput(input: EditText, defaultValue: String, fieldName: String) {
        val colorStr = input.text.toString()
        if (!WidgetSettings.isValidColor(colorStr)) {
            Logger.w(TAG, "Invalid $fieldName: $colorStr, correcting to default")
            input.setText(defaultValue)
            android.widget.Toast.makeText(
                this,
                "Invalid $fieldName. Reset to default.",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun setupSeekBarListener(seekBar: SeekBar, label: TextView) {
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val size = progress + WidgetSettings.MIN_TEXT_SIZE
                label.text = "$size sp"
                if (fromUser) {
                    updateWidgets()
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun updateWidgets() {
        Logger.d(TAG, "updateWidgets")
        val context: Context = this@WidgetConfigActivity
        saveSettings(context)

        // Update ALL widgets with fresh temperature data
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val componentName = ComponentName(context, DateWidgetProvider::class.java)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)

        // Fetch temperature in background if enabled
        val showTemp = WidgetSettings.loadShowTemp(context)
        if (showTemp) {
            Thread {
                try {
                    val temp = DateWidgetProvider.fetchTemperaturePublic(context)
                    DateWidgetProvider.updateAppWidget(context, appWidgetManager, appWidgetIds, temp)
                } catch (e: Exception) {
                    e.printStackTrace()
                    DateWidgetProvider.updateAppWidget(context, appWidgetManager, appWidgetIds)
                }
            }.start()
        } else {
            DateWidgetProvider.updateAppWidget(context, appWidgetManager, appWidgetIds)
        }
    }

    private fun loadSettings() {
        Logger.d(TAG, "Loading settings")
        val prefs = getSharedPreferences(WidgetSettings.PREFS_NAME, Context.MODE_PRIVATE)
        
        // Load and validate text color
        val textColorStr = prefs.getString(WidgetSettings.PREF_TEXT_COLOR_KEY, WidgetSettings.DEFAULT_TEXT_COLOR) ?: WidgetSettings.DEFAULT_TEXT_COLOR
        if (WidgetSettings.isValidColor(textColorStr)) {
            textColorInput.setText(textColorStr)
        } else {
            Logger.w(TAG, "Invalid saved text color: $textColorStr, using default")
            textColorInput.setText(WidgetSettings.DEFAULT_TEXT_COLOR)
            // Fix the saved preference
            prefs.edit().putString(WidgetSettings.PREF_TEXT_COLOR_KEY, WidgetSettings.DEFAULT_TEXT_COLOR).apply()
        }
        
        // Load and validate background color
        val bgColorStr = prefs.getString(WidgetSettings.PREF_BG_COLOR_KEY, WidgetSettings.DEFAULT_BG_COLOR) ?: WidgetSettings.DEFAULT_BG_COLOR
        if (WidgetSettings.isValidColor(bgColorStr)) {
            backgroundColorInput.setText(bgColorStr)
        } else {
            Logger.w(TAG, "Invalid saved background color: $bgColorStr, using default")
            backgroundColorInput.setText(WidgetSettings.DEFAULT_BG_COLOR)
            // Fix the saved preference
            prefs.edit().putString(WidgetSettings.PREF_BG_COLOR_KEY, WidgetSettings.DEFAULT_BG_COLOR).apply()
        }
        
        opacitySeekBar.progress = prefs.getInt(WidgetSettings.PREF_BG_OPACITY_KEY, WidgetSettings.DEFAULT_BG_OPACITY)

        val dateSize = prefs.getInt(WidgetSettings.PREF_DATE_SIZE_KEY, WidgetSettings.DEFAULT_DATE_SIZE)
        val daySize = prefs.getInt(WidgetSettings.PREF_DAY_SIZE_KEY, WidgetSettings.DEFAULT_DAY_SIZE)
        val tempSize = prefs.getInt(WidgetSettings.PREF_TEMP_SIZE_KEY, WidgetSettings.DEFAULT_TEMP_SIZE)

        dateSizeSeekBar.progress = dateSize - WidgetSettings.MIN_TEXT_SIZE
        daySizeSeekBar.progress = daySize - WidgetSettings.MIN_TEXT_SIZE
        tempSizeSeekBar.progress = tempSize - WidgetSettings.MIN_TEXT_SIZE

        dateSizeLabel.text = "$dateSize sp"
        daySizeLabel.text = "$daySize sp"
        tempSizeLabel.text = "$tempSize sp"

        showYearSwitch.isChecked = prefs.getBoolean(WidgetSettings.PREF_SHOW_YEAR_KEY, WidgetSettings.DEFAULT_SHOW_YEAR)
        showDayOfWeekSwitch.isChecked = prefs.getBoolean(WidgetSettings.PREF_SHOW_DOW_KEY, WidgetSettings.DEFAULT_SHOW_DOW)
        showTemperatureSwitch.isChecked = prefs.getBoolean(WidgetSettings.PREF_SHOW_TEMP_KEY, WidgetSettings.DEFAULT_SHOW_TEMP)

        val language = prefs.getString(WidgetSettings.PREF_LANGUAGE_KEY, WidgetSettings.LANG_SYSTEM) ?: WidgetSettings.LANG_SYSTEM
        when (language) {
            WidgetSettings.LANG_ENGLISH -> languageRadioGroup.check(R.id.lang_english)
            WidgetSettings.LANG_JAPANESE -> languageRadioGroup.check(R.id.lang_japanese)
            else -> languageRadioGroup.check(R.id.lang_system)
        }

        val layoutType = prefs.getInt(WidgetSettings.PREF_LAYOUT_TYPE_KEY, WidgetSettings.LAYOUT_HORIZONTAL)
        when (layoutType) {
            WidgetSettings.LAYOUT_HORIZONTAL -> layoutRadioGroup.check(R.id.layout_horizontal)
            WidgetSettings.LAYOUT_TWO_ROWS_TEMP -> layoutRadioGroup.check(R.id.layout_two_rows_temp)
            WidgetSettings.LAYOUT_TWO_ROWS_WEEKDAY_TEMP -> layoutRadioGroup.check(R.id.layout_two_rows_weekday_temp)
            WidgetSettings.LAYOUT_THREE_ROWS -> layoutRadioGroup.check(R.id.layout_three_rows)
            else -> layoutRadioGroup.check(R.id.layout_horizontal)
        }
        Logger.d(TAG, "Settings loaded")
    }

    private fun saveSettings(context: Context) {
        Logger.d(TAG, "Saving settings")
        val prefs = context.getSharedPreferences(WidgetSettings.PREFS_NAME, Context.MODE_PRIVATE).edit()
        
        // Validate and save text color
        val textColorStr = textColorInput.text.toString()
        if (WidgetSettings.isValidColor(textColorStr)) {
            prefs.putString(WidgetSettings.PREF_TEXT_COLOR_KEY, textColorStr)
        } else {
            Logger.w(TAG, "Invalid text color input: $textColorStr, using default")
            prefs.putString(WidgetSettings.PREF_TEXT_COLOR_KEY, WidgetSettings.DEFAULT_TEXT_COLOR)
        }
        
        // Validate and save background color
        val bgColorStr = backgroundColorInput.text.toString()
        if (WidgetSettings.isValidColor(bgColorStr)) {
            prefs.putString(WidgetSettings.PREF_BG_COLOR_KEY, bgColorStr)
        } else {
            Logger.w(TAG, "Invalid background color input: $bgColorStr, using default")
            prefs.putString(WidgetSettings.PREF_BG_COLOR_KEY, WidgetSettings.DEFAULT_BG_COLOR)
        }
        
        prefs.putInt(WidgetSettings.PREF_BG_OPACITY_KEY, opacitySeekBar.progress)
        prefs.putInt(WidgetSettings.PREF_DATE_SIZE_KEY, dateSizeSeekBar.progress + WidgetSettings.MIN_TEXT_SIZE)
        prefs.putInt(WidgetSettings.PREF_DAY_SIZE_KEY, daySizeSeekBar.progress + WidgetSettings.MIN_TEXT_SIZE)
        prefs.putInt(WidgetSettings.PREF_TEMP_SIZE_KEY, tempSizeSeekBar.progress + WidgetSettings.MIN_TEXT_SIZE)
        prefs.putBoolean(WidgetSettings.PREF_SHOW_YEAR_KEY, showYearSwitch.isChecked)
        prefs.putBoolean(WidgetSettings.PREF_SHOW_DOW_KEY, showDayOfWeekSwitch.isChecked)
        prefs.putBoolean(WidgetSettings.PREF_SHOW_TEMP_KEY, showTemperatureSwitch.isChecked)

        val language = when (languageRadioGroup.checkedRadioButtonId) {
            R.id.lang_english -> WidgetSettings.LANG_ENGLISH
            R.id.lang_japanese -> WidgetSettings.LANG_JAPANESE
            else -> WidgetSettings.LANG_SYSTEM
        }
        prefs.putString(WidgetSettings.PREF_LANGUAGE_KEY, language)

        val layoutType = when (layoutRadioGroup.checkedRadioButtonId) {
            R.id.layout_horizontal -> WidgetSettings.LAYOUT_HORIZONTAL
            R.id.layout_two_rows_temp -> WidgetSettings.LAYOUT_TWO_ROWS_TEMP
            R.id.layout_two_rows_weekday_temp -> WidgetSettings.LAYOUT_TWO_ROWS_WEEKDAY_TEMP
            R.id.layout_three_rows -> WidgetSettings.LAYOUT_THREE_ROWS
            else -> WidgetSettings.LAYOUT_HORIZONTAL
        }
        prefs.putInt(WidgetSettings.PREF_LAYOUT_TYPE_KEY, layoutType)

        prefs.apply()
        Logger.d(TAG, "Settings saved")
    }

    private fun checkExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val canSchedule = alarmManager.canScheduleExactAlarms()
            Logger.d(TAG, "Checking exact alarm permission: canSchedule=$canSchedule")
            
            if (!canSchedule) {
                AlertDialog.Builder(this)
                    .setTitle("Permission Required")
                    .setMessage("To update the widget precisely every minute, please grant the 'Alarms & reminders' permission.")
                    .setPositiveButton("Grant") { _, _ ->
                        try {
                            // Try app-specific intent first
                            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                data = Uri.parse("package:${packageName}")
                            }
                            Logger.d(TAG, "Opening app-specific alarm permission settings")
                            startActivity(intent)
                        } catch (e: Exception) {
                            // Fallback to general alarm settings if app-specific fails
                            Logger.w(TAG, "Failed to open app-specific settings, trying general settings", e)
                            try {
                                val fallbackIntent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                                startActivity(fallbackIntent)
                            } catch (e2: Exception) {
                                Logger.e(TAG, "Failed to open alarm settings", e2)
                                android.widget.Toast.makeText(
                                    this,
                                    "Please enable 'Alarms & reminders' permission in Settings",
                                    android.widget.Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            } else {
                Logger.d(TAG, "Exact alarm permission already granted")
            }
        }
    }

    companion object {
        private const val TAG = "OnlyDate"
    }
}
