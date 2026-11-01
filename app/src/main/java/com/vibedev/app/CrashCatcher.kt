package com.vibedev.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File

/**
 * Test builds only. When the app crashes, save what happened. On the next
 * launch, CrashReportDialog shows it with a "Copy for Claude" button, so a
 * non-technical author can paste the details into their Claude session.
 */
object CrashCatcher {
    private const val FILE_NAME = "last_crash.txt"

    fun install(context: Context) {
        val appContext = context.applicationContext
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            runCatching {
                File(appContext.filesDir, FILE_NAME).writeText(describe(error, thread))
            }
            previous?.uncaughtException(thread, error)
        }
    }

    /** Returns the saved crash report, or null if the last run didn't crash. */
    fun pendingReport(context: Context): String? =
        File(context.filesDir, FILE_NAME).takeIf { it.exists() }?.readText()

    fun clear(context: Context) {
        File(context.filesDir, FILE_NAME).delete()
    }

    private fun describe(error: Throwable, thread: Thread): String = buildString {
        appendLine("My app crashed. Please find and fix the cause. Details:")
        appendLine()
        appendLine("App version: ${BuildConfig.VERSION_NAME} (build ${BuildConfig.VERSION_CODE})")
        appendLine("Phone: ${Build.MANUFACTURER} ${Build.MODEL}, Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
        appendLine("Thread: ${thread.name}")
        appendLine()
        append(error.stackTraceToString().take(12_000))
    }
}

@Composable
fun CrashReportDialog() {
    if (!BuildConfig.DEBUG) return
    val context = LocalContext.current
    var report by remember { mutableStateOf(CrashCatcher.pendingReport(context)) }
    val text = report ?: return

    fun dismiss() {
        CrashCatcher.clear(context)
        report = null
    }

    AlertDialog(
        onDismissRequest = ::dismiss,
        title = { Text("The app crashed last time") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()).heightIn(max = 400.dp)) {
                Text("Tap \"Copy for Claude\", then paste it into your Claude chat so Claude can fix it.")
                Spacer(Modifier.height(12.dp))
                Text(text = text, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val clipboard = context.getSystemService(ClipboardManager::class.java)
                clipboard.setPrimaryClip(ClipData.newPlainText("Crash report", text))
                dismiss()
            }) { Text("Copy for Claude") }
        },
        dismissButton = {
            TextButton(onClick = ::dismiss) { Text("Dismiss") }
        },
    )
}
