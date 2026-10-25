package online.usim.go2servicemode

import android.content.Context
import df.root.DFBridge
import java.io.File

/**
 * TRANSACTION 2 — open ServiceMode with one whitelisted keyString.
 *
 * The app never touches privilege. It cannot: ksud bind-mounts /system/bin/su
 * in a mount namespace the app does not share (verified on device — as app uid
 * the path does not exist, while `adb shell su -c id` returns u:r:ksu:s0), and
 * `am` is a Java program that is SIGKILLed inside the payload's
 * vendor_modprobe context.
 *
 * The channel is a mailbox the APP owns:
 *   files/tx2/req     <- this class writes "<seq>|<command>"
 *   files/tx2/out.log <- the root helper appends stdout/stderr
 *   files/tx2/status  <- the root helper writes its pid once listening
 *
 * Files must be created by the app, not by the helper: root-created files carry
 * no MLS category set, and the app then cannot read them at all.
 */
object MenuTx {

    private const val AM = "/system/bin/am"
    private const val TIMEOUT_MS = 6000L
    private const val POLL_MS = 80L
    private const val HEARTBEAT_MS = 3000L
    private var seq = 0

    fun dir(context: Context): File = File(context.filesDir, "tx2")

    /**
     * Creates the channel if needed. Called at app start, before any run, so
     * the helper always finds files it can write into.
     */
    fun ensure(context: Context): File {
        val d = dir(context)
        d.mkdirs()
        for (name in arrayOf("req", "out.log", "status")) {
            val f = File(d, name)
            if (!f.exists()) runCatching { f.writeText("") }
        }
        return d
    }

    /**
     * True once the helper has announced itself and is still heartbeating.
     * The pid alone is not evidence: a killed helper leaves its last pid on
     * disk, and the app cannot stat a root process to tell.
     */
    fun ready(context: Context): Boolean {
        val st = File(dir(context), "status")
        if (!st.exists()) return false
        val beat = System.currentTimeMillis() - st.lastModified()
        return beat in 0..HEARTBEAT_MS &&
            runCatching { st.readText().trim() }.getOrDefault("").isNotEmpty()
    }

    fun clearStatus(context: Context) {
        runCatching { File(dir(context), "status").writeText("") }
    }

    /** Blocking; call from Dispatchers.IO. */
    fun request(context: Context, key: String, onLog: (String) -> Unit): Int {
        if (key != Engine.KEY_GCF && key != Engine.KEY_NAS) {
            onLog("拒绝白名单之外的 keyString")
            return -2
        }
        val d = ensure(context)
        val req = File(d, "req")
        val out = File(d, "out.log")
        if (!ready(context)) {
            onLog("事务 2 未就绪：常驻助手没有报到（先完成事务 1）")
            return -3
        }
        val am = "$AM start --user 0 -n ${DFBridge.COMPONENT} --es keyString $key"
        onLog("→ $am")
        val before = runCatching { out.length() }.getOrDefault(0L)
        return try {
            req.writeText("${++seq}\n$am\n")
            val deadline = System.currentTimeMillis() + TIMEOUT_MS
            while (System.currentTimeMillis() < deadline && out.length() <= before) {
                Thread.sleep(POLL_MS)
            }
            val all = runCatching { out.readText() }.getOrDefault("")
            if (all.length.toLong() > before) {
                all.substring(before.toInt().coerceAtMost(all.length))
                    .lineSequence().filter { it.isNotBlank() }.forEach(onLog)
                0
            } else {
                onLog("事务 2 超时：助手没有回写结果")
                -4
            }
        } catch (t: Throwable) {
            onLog("事务 2 失败：${t.javaClass.simpleName}: ${t.message}")
            -99
        }
    }
}
