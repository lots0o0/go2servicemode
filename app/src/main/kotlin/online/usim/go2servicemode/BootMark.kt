package online.usim.go2servicemode

import android.content.Context
import android.os.SystemClock
import android.provider.Settings

/**
 * Remembers that transaction 1 succeeded, together with the boot it happened
 * in. The engine can only work once per boot; a second attempt returns "ok"
 * within about a second without executing the payload, which is indistinguishable
 * from a real failure unless the app knows a run already happened.
 */
object BootMark {

    private const val PREFS = "boot_state"
    private const val KEY_BOOT_COUNT = "success_boot_count"
    private const val KEY_ELAPSED = "success_elapsed"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun bootCount(context: Context): Int = runCatching {
        Settings.Global.getInt(context.contentResolver, Settings.Global.BOOT_COUNT, -1)
    }.getOrDefault(-1)

    fun markSuccess(context: Context) {
        prefs(context).edit()
            .putInt(KEY_BOOT_COUNT, bootCount(context))
            .putLong(KEY_ELAPSED, SystemClock.elapsedRealtime())
            .apply()
    }

    /** True when the last success happened in the boot the device is in now. */
    fun successThisBoot(context: Context): Boolean {
        val p = prefs(context)
        val savedElapsed = p.getLong(KEY_ELAPSED, 0L)
        if (savedElapsed <= 0L) return false
        val savedBoot = p.getInt(KEY_BOOT_COUNT, -1)
        val currentBoot = bootCount(context)
        if (savedBoot >= 0 && currentBoot >= 0) return savedBoot == currentBoot
        // Fallback when BOOT_COUNT is unreadable: elapsed time only grows within
        // one boot, so a smaller current value means the device restarted.
        return SystemClock.elapsedRealtime() >= savedElapsed
    }
}
