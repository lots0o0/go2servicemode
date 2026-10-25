package online.usim.go2servicemode

import android.content.Context
import df.root.DFBridge
import df.root.IReporter

/**
 * Thin Kotlin wrapper over the Java engine bridge.
 *
 * Fast method only (DirtyFrag / libexp, classic ksud flavour) - that is the
 * combination proven on-device earlier. No profile, no Next flavour, no
 * KernelSU Manager interaction.
 */
object Engine {

    val KEY_GCF: String = DFBridge.KEY_GCF
    val KEY_NAS: String = DFBridge.KEY_NAS

    @Volatile
    var running: Boolean = false
        private set

    /** Blocking; call from a worker thread or coroutine on Dispatchers.IO. */
    fun establishSu(context: Context, onLog: (String) -> Unit): Int =
        guarded(onLog) { DFBridge.establishSu(context, reporter(onLog)) }

    /** Transaction 2. Rejected unless the key is whitelisted. */
    fun openMenu(context: Context, key: String, onLog: (String) -> Unit): Int {
        if (key != KEY_GCF && key != KEY_NAS) {
            onLog("refusing key outside the whitelist")
            return -2
        }
        return guarded(onLog) { DFBridge.openMenu(context, key, reporter(onLog)) }
    }

    private fun reporter(onLog: (String) -> Unit) =
        IReporter { msg -> if (!msg.isNullOrBlank()) onLog(msg) }

    private inline fun guarded(onLog: (String) -> Unit, block: () -> Int): Int {
        running = true
        return try {
            block()
        } catch (t: Throwable) {
            onLog("fatal: ${t.javaClass.simpleName}: ${t.message}")
            -99
        } finally {
            running = false
        }
    }
}
