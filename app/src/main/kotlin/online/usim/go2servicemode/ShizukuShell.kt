package online.usim.go2servicemode

import rikka.shizuku.Shizuku

/**
 * Starts the resident helper through Shizuku.
 *
 * Why Shizuku: the helper only works if it runs in a domain that keeps access to
 * the app's private directory after the engine restores SELinux. This process
 * cannot get there (writing /proc/self/attr/exec is refused even with full
 * capabilities), but Shizuku's shell context can: `su` is visible to it, and
 * `su -c` lands in the ksu domain. Transaction 1 writes the helper script to
 * /data/local/tmp, so all that is left is launching it.
 *
 * `Shizuku.newProcess` is not part of the public surface in 13.x, so it is
 * reached through reflection; Shizuku itself is a plain library class, so this
 * does not hit the framework hidden-API list.
 */
object ShizukuShell {

    private const val HELPER = "/data/local/tmp/g2sm-helper.sh"

    fun isBinderAlive(): Boolean = runCatching { Shizuku.pingBinder() }.getOrDefault(false)

    fun hasPermission(): Boolean = runCatching {
        Shizuku.checkSelfPermission() == android.content.pm.PackageManager.PERMISSION_GRANTED
    }.getOrDefault(false)

    fun requestPermission(requestCode: Int = 1001) {
        runCatching { Shizuku.requestPermission(requestCode) }
    }

    /** Blocking. Returns the helper's exit code, or a negative value on failure. */
    fun startHelper(): Int {
        if (!isBinderAlive()) return -10
        if (!hasPermission()) return -11
        // The trailing `&` matters: setsid execs the helper rather than forking,
        // so without it the remote process never exits and waitFor() blocks the
        // app forever ("stuck at starting channel").
        val cmd = "/system/bin/su -c 'setsid /system/bin/sh $HELPER >/dev/null 2>&1 &'"
        return try {
            val m = Shizuku::class.java.getDeclaredMethod(
                "newProcess",
                Array<String>::class.java,
                Array<String>::class.java,
                String::class.java
            )
            m.isAccessible = true
            val p = m.invoke(
                null,
                arrayOf("/system/bin/sh", "-c", cmd),
                null,
                null
            ) as Process
            p.waitFor()
        } catch (t: Throwable) {
            -12
        }
    }
}
