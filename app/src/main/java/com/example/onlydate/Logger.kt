package com.example.onlydate

import android.util.Log
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Logger {
    private val logBuffer = StringBuilder()
    private var listener: ((String) -> Unit)? = null

    fun d(tag: String, msg: String) {
        Log.d(tag, msg)
        appendLog("D/$tag: $msg")
    }

    fun e(tag: String, msg: String, tr: Throwable? = null) {
        Log.e(tag, msg, tr)
        appendLog("E/$tag: $msg\n${Log.getStackTraceString(tr)}")
    }

    fun i(tag: String, msg: String) {
        Log.i(tag, msg)
        appendLog("I/$tag: $msg")
    }

    fun w(tag: String, msg: String, tr: Throwable? = null) {
        Log.w(tag, msg, tr)
        val stackTrace = if (tr != null) "\n${Log.getStackTraceString(tr)}" else ""
        appendLog("W/$tag: $msg$stackTrace")
    }

    private fun appendLog(formattedMsg: String) {
        val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        val entry = "$timestamp $formattedMsg\n"
        synchronized(logBuffer) {
            logBuffer.append(entry)
        }
        // Notify on main thread if possible, but since this is a simple object, 
        // the caller (Activity) should handle threading if needed. 
        // However, logs can come from background threads.
        // We will just invoke the listener. The Activity should use runOnUiThread.
        listener?.invoke(getLogs())
    }

    fun setListener(l: (String) -> Unit) {
        listener = l
        // Send current buffer immediately
        l(getLogs())
    }

    fun removeListener() {
        listener = null
    }

    fun getLogs(): String {
        synchronized(logBuffer) {
            return logBuffer.toString()
        }
    }
    
    fun clear() {
        synchronized(logBuffer) {
            logBuffer.setLength(0)
        }
        listener?.invoke("")
    }
}
