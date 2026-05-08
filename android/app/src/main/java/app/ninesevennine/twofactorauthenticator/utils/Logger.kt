package app.ninesevennine.twofactorauthenticator.utils

import android.content.Context
import android.util.Log
import java.io.File
import kotlin.time.Clock

object Logger {
    private const val TAG = "Logger"
    private const val FILE = "app.log"

    @Volatile
    private var file: File? = null
    private val lock = Any()

    fun init(context: Context) {
        if (file == null) {
            synchronized(lock) {
                if (file == null) {
                    try {
                        val logFile = File(context.cacheDir, FILE)
                        if (!logFile.exists()) {
                            val created = logFile.createNewFile()
                            if (!created) {
                                Log.e(TAG, "could not create log file")
                            }
                        }
                        file = logFile
                    } catch (e: Exception) {
                        Log.e(TAG, "failed to initialise log file", e)
                    }
                }
            }
        }
    }

    private fun ts(): String {
        val iso = Clock.System.now().toString()
        val noMs = iso.substringBefore(".") + "Z"
        return noMs.substring(2)
    }

    private fun log(type: Char, tag: String, msg: String, t: Throwable? = null) {
        val f = file ?: return

        val line = buildString {
            append("[${ts()}][$type][$tag] $msg\n")
            if (t != null) {
                append(Log.getStackTraceString(t))
                append("\n")
            }
        }

        try {
            synchronized(lock) {
                if (!f.exists()) {
                    f.createNewFile()
                }
                f.appendText(line)
            }
        } catch (e: Exception) {
            Log.e(TAG, "failed to write to log file", e)
        }

        when (type) {
            'V' -> Log.v(tag, msg, t)
            'D' -> Log.d(tag, msg, t)
            'I' -> Log.i(tag, msg, t)
            'W' -> Log.w(tag, msg, t)
            'E' -> Log.e(tag, msg, t)
        }
    }

    fun v(tag: String, msg: String, t: Throwable? = null) = log('V', tag, msg, t)
    fun d(tag: String, msg: String, t: Throwable? = null) = log('D', tag, msg, t)
    fun i(tag: String, msg: String, t: Throwable? = null) = log('I', tag, msg, t)
    fun w(tag: String, msg: String, t: Throwable? = null) = log('W', tag, msg, t)
    fun e(tag: String, msg: String, t: Throwable? = null) = log('E', tag, msg, t)

    fun getFullLog(): String {
        return try {
            val currentFile = file ?: return ""
            synchronized(lock) {
                if (currentFile.exists()) {
                    currentFile.readText()
                } else {
                    ""
                }
            }
        } catch (e: Exception) {
            "failed to read log\n${e.message}"
        }
    }
}