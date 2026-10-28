package com.arhomedesign.core.crash

import android.content.Context
import java.io.File

object CrashReporting {
    private const val FILE_NAME = "last_crash.log"

    fun init(context: Context) {
        val prev = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching {
                val file = File(context.filesDir, FILE_NAME)
                file.writeText(buildString {
                    appendLine("Thread: ${thread.name}")
                    appendLine(throwable.stackTraceToString())
                })
            }
            prev?.uncaughtException(thread, throwable)
        }
    }

    fun readLastCrash(context: Context): String? {
        val file = File(context.filesDir, FILE_NAME)
        if (!file.exists()) return null
        return runCatching { file.readText() }.getOrNull()
    }

    fun clearLastCrash(context: Context) {
        runCatching { File(context.filesDir, FILE_NAME).delete() }
    }
}
