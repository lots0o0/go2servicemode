package online.usim.go2servicemode

import android.content.Context
import java.io.File

/**
 * What the device looks like right now, from the app's side.
 *
 * The app cannot see /system/bin/su (the bind-mount lives in a namespace it
 * does not share), so "is su up?" is answered by two proxies:
 *   - the resident helper's heartbeat (status file mtime)
 *   - the payload log written by transaction 1 (root-owned, world-readable)
 */
data class DeviceSnapshot(
    val helperAlive: Boolean,
    val payloadRan: Boolean,
    val payloadSucceeded: Boolean,
    val suWaitTries: Int?
) {
    /** Only a live helper can actually run am, so this is what unlocks step 2. */
    val canOpenMenu: Boolean get() = helperAlive

    val description: String
        get() = when {
            helperAlive -> "通道在线，可直接执行第二步"
            payloadSucceeded -> "本次开机曾建立成功，但通道已断开，请重新执行"
            payloadRan -> "上次执行未完成，详见日志"
            else -> "未检测到通道，请先执行上一步"
        }
}

object DeviceProbe {

    private val TX1_LOG = File("/data/local/tmp/g2sm-tx1.log")

    fun probe(context: Context): DeviceSnapshot {
        val lines = runCatching { TX1_LOG.readLines() }.getOrNull().orEmpty()
        val ran = lines.any { it.startsWith("tx1-start") }
        val lateloadOk = lines.any { it.startsWith("tx1-lateload=0") }
        val suPresent = lines.any { it.startsWith("tx1-su-present") }
        val tries = lines.firstOrNull { it.startsWith("tx1-su-present-tries=") }
            ?.substringAfter('=')
            ?.toIntOrNull()
        return DeviceSnapshot(
            helperAlive = MenuTx.ready(context),
            payloadRan = ran,
            payloadSucceeded = lateloadOk && suPresent,
            suWaitTries = tries
        )
    }
}
