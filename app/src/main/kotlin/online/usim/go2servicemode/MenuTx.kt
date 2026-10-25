package online.usim.go2servicemode

import android.content.Context
import df.root.DFBridge
import java.io.File

/**
 * TRANSACTION 2 — send one whitelisted keyString to ServiceMode.
 *
 * The helper started through Shizuku lives in the ksu domain, so the mailbox is
 * usable in both directions for as long as the helper runs:
 *
 *   files/tx2/req                        <- app writes "<seq>\n<command>"
 *   /data/local/tmp/g2sm-tx2-out.log     <- helper appends output
 *   /data/local/tmp/g2sm-tx2-status      <- helper heartbeat (mtime)
 *
 * Several commands per boot work because the helper keeps polling req.
 */
object MenuTx {

    private const val AM = "/system/bin/am"
    private const val TIMEOUT_MS = 8000L
    private const val POLL_MS = 80L
    private const val HEARTBEAT_MS = 3000L

    private val STATUS = File("/data/local/tmp/g2sm-tx2-status")
    private val OUT = File("/data/local/tmp/g2sm-tx2-out.log")

    private var seq = 0

    fun dir(context: Context): File = File(context.filesDir, "tx2")

    fun ensure(context: Context): File {
        val d = dir(context)
        d.mkdirs()
        val req = File(d, "req")
        if (!req.exists()) runCatching { req.writeText("") }
        return d
    }

    /** True while the helper keeps refreshing its status file. */
    fun ready(context: Context): Boolean {
        if (!STATUS.exists()) return false
        val beat = System.currentTimeMillis() - STATUS.lastModified()
        return beat in 0..HEARTBEAT_MS &&
            runCatching { STATUS.readText().trim() }.getOrDefault("").isNotEmpty()
    }

    fun clearStatus(context: Context) = Unit

    /** Blocking; call from Dispatchers.IO. */
    fun request(context: Context, key: String, onLog: (String) -> Unit): Int {
        if (key != Engine.KEY_GCF && key != Engine.KEY_NAS) {
            onLog("拒绝白名单之外的 keyString")
            return -2
        }
        val d = ensure(context)
        val req = File(d, "req")
        if (!ready(context)) {
            onLog("通道未就绪：先用 Shizuku 启动通道")
            return -3
        }
        val am = "$AM start --user 0 -n ${DFBridge.COMPONENT} --es keyString $key"
        onLog("→ $am")
        val before = runCatching { OUT.length() }.getOrDefault(0L)
        return try {
            req.writeText("${++seq}\n$am\n")
            val deadline = System.currentTimeMillis() + TIMEOUT_MS
            while (System.currentTimeMillis() < deadline && OUT.length() <= before) {
                Thread.sleep(POLL_MS)
            }
            val all = runCatching { OUT.readText() }.getOrDefault("")
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
