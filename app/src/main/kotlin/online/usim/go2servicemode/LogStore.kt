package online.usim.go2servicemode

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** One line of a run, as shown in the log page. */
data class LogEntry(val time: String, val text: String)

/**
 * Run logs are written to disk by default, one file per run.
 *
 * The in-memory list is only a view: if the process dies — which is exactly what
 * happens when a system dialog takes the foreground and the activity is
 * recreated — the record must still exist afterwards. That is why the log page
 * restores from the newest file instead of trusting composition state.
 *
 * A run file starts with an optional `## <summary>` line: the one-line outcome
 * shown above the folded raw output.
 */
object LogStore {

    private const val SUMMARY_PREFIX = "## "

    private val stamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US)
    private val human = SimpleDateFormat("MM-dd HH:mm:ss", Locale.US)

    private fun dir(context: Context): File =
        File(context.filesDir, "runs").apply { mkdirs() }

    /** Creates a new run file and returns it. */
    fun newRun(context: Context): File {
        val f = File(dir(context), "run-${stamp.format(Date())}.log")
        f.writeText("")
        return f
    }

    fun append(file: File, time: String, text: String) {
        runCatching { file.appendText("$time  $text\n") }
    }

    /**
     * Writes the one-line outcome as a header, keeping the lines already in the
     * file. Earlier this used writeText() alone, which truncates the file — the
     * live page looked complete while the stored run contained only the summary.
     */
    fun writeSummary(file: File, summary: String) {
        runCatching {
            val body = if (file.exists()) read(file) else ""
            val kept = body.lineSequence()
                .filterNot { it.startsWith(SUMMARY_PREFIX) }
                .joinToString("\n")
                .trimEnd('\n')
            file.writeText(
                if (kept.isEmpty()) "$SUMMARY_PREFIX$summary\n"
                else "$SUMMARY_PREFIX$summary\n$kept\n"
            )
        }
    }

    fun summaryOf(file: File): String? =
        runCatching {
            read(file).lineSequence()
                .firstOrNull { it.startsWith(SUMMARY_PREFIX) }
                ?.removePrefix(SUMMARY_PREFIX)
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
        }.getOrNull()

    /** Newest first. */
    fun list(context: Context): List<File> =
        (dir(context).listFiles { f -> f.isFile && f.name.endsWith(".log") } ?: emptyArray())
            .sortedByDescending { it.name }

    fun label(file: File): String {
        val m = Regex("run-(\\d{8})-(\\d{6})\\.log").find(file.name)
        if (m == null) return file.name
        return runCatching {
            val d = stamp.parse(m.groupValues[1] + "-" + m.groupValues[2])
            human.format(d ?: Date())
        }.getOrDefault(file.name)
    }

    fun read(file: File): String = runCatching { file.readText() }.getOrDefault("")

    fun delete(file: File) {
        runCatching { file.delete() }
    }

    /**
     * Splits a stored run back into (time, text) pairs, dropping the summary
     * line so it is not shown twice.
     */
    fun parse(file: File): List<LogEntry> =
        read(file).lineSequence()
            .filter { it.isNotBlank() && !it.startsWith(SUMMARY_PREFIX) }
            .map { line ->
                val i = line.indexOf("  ")
                if (i > 0) LogEntry(line.substring(0, i), line.substring(i + 2))
                else LogEntry("", line)
            }
            .toList()
}
