package com.wlqboard.nativeapp

import android.content.Context
import android.os.Build
import android.os.Process
import android.util.Log
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** Captures uncaught failures and keeps a small, local diagnostic history. */
class CrashHandler private constructor(
    private val appContext: Context,
    private val previousHandler: Thread.UncaughtExceptionHandler?
) : Thread.UncaughtExceptionHandler {

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        try {
            appendReport(appContext, createReport(throwable, "Excepción no controlada", thread.name))
        } catch (storageFailure: Exception) {
            Log.e(TAG, "No se pudo guardar el informe de cierre", storageFailure)
        }
        val previous = previousHandler
        if (previous != null && previous !== this) {
            previous.uncaughtException(thread, throwable)
        } else {
            Process.killProcess(Process.myPid())
            kotlin.system.exitProcess(10)
        }
    }

    companion object {
        private const val TAG = "VozFlowCrashHandler"
        private const val PREFS_NAME = "vozflow_diagnostics"
        const val LAST_CRASH_REPORT_KEY = "last_crash_report"
        private const val MAX_REPORTS = 3
        private val lock = Any()

        @Volatile private var installedHandler: CrashHandler? = null

        @Synchronized
        fun install(context: Context) {
            val existing = installedHandler
            if (existing != null) {
                if (Thread.getDefaultUncaughtExceptionHandler() !== existing) {
                    Thread.setDefaultUncaughtExceptionHandler(existing)
                }
                return
            }
            val previous = Thread.getDefaultUncaughtExceptionHandler()
            val handler = CrashHandler(context.applicationContext, previous)
            installedHandler = handler
            Thread.setDefaultUncaughtExceptionHandler(handler)
        }

        fun recordException(context: Context, throwable: Throwable, stage: String) {
            try {
                install(context)
                appendReport(context.applicationContext, createReport(throwable, stage, Thread.currentThread().name))
            } catch (storageFailure: Exception) {
                Log.e(TAG, "No se pudo registrar un error de diagnóstico", storageFailure)
            }
        }

        fun getLastReport(context: Context): String {
            val stored = readStoredValue(context) ?: return ""
            val reports = parseReports(stored)
            return if (reports.isNotEmpty()) reports.last() else stored
        }

        fun getRecentReports(context: Context): List<String> {
            val stored = readStoredValue(context) ?: return emptyList()
            return parseReports(stored).takeLast(MAX_REPORTS)
        }

        fun clear(context: Context) {
            try {
                context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    .edit().remove(LAST_CRASH_REPORT_KEY).commit()
            } catch (failure: Exception) {
                Log.e(TAG, "No se pudo limpiar el historial de errores", failure)
            }
        }

        private fun readStoredValue(context: Context): String? = try {
            context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(LAST_CRASH_REPORT_KEY, null)
        } catch (failure: Exception) {
            Log.e(TAG, "No se pudo leer el historial de errores", failure)
            null
        }

        private fun appendReport(context: Context, report: String) {
            synchronized(lock) {
                val preferences = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val stored = preferences.getString(LAST_CRASH_REPORT_KEY, null)
                val reports = parseReports(stored.orEmpty()).toMutableList()
                reports.add(report)
                val bounded = reports.takeLast(MAX_REPORTS)
                val json = JSONArray()
                bounded.forEach(json::put)
                val saved = preferences.edit().putString(LAST_CRASH_REPORT_KEY, json.toString()).commit()
                if (!saved) Log.e(TAG, "SharedPreferences no confirmó la escritura del informe")
            }
        }

        private fun parseReports(value: String): List<String> {
            if (value.isBlank()) return emptyList()
            return try {
                val array = JSONArray(value)
                (0 until array.length()).mapNotNull { array.optString(it).takeIf(String::isNotBlank) }
            } catch (_: Exception) {
                listOf(value)
            }
        }

        private fun createReport(throwable: Throwable, stage: String, threadName: String): String {
            val timestamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.US).apply {
                timeZone = TimeZone.getDefault()
            }.format(Date())
            val rootCause = generateSequence(throwable) { it.cause }.last()
            val origin = throwable.stackTrace.firstOrNull()?.className ?: rootCause.javaClass.name
            return buildString {
                appendLine("VozFlow Pro · Informe de diagnóstico")
                appendLine("Fecha/hora: $timestamp")
                appendLine("Dispositivo: ${Build.MANUFACTURER} ${Build.MODEL}".trim())
                appendLine("Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
                appendLine("Etapa: $stage")
                appendLine("Hilo: $threadName")
                appendLine("Clase de excepción: ${throwable.javaClass.name}")
                appendLine("Causa raíz: ${rootCause.javaClass.name}: ${rootCause.message.orEmpty()}")
                appendLine("Clase del primer frame: $origin")
                appendLine()
                appendLine("Stack trace completo:")
                append(Log.getStackTraceString(throwable))
            }
        }
    }
}
