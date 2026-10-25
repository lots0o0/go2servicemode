package online.usim.go2servicemode

import android.content.Context
import java.io.File
import java.security.MessageDigest

/**
 * Identity of the installed APK.
 *
 * Two builds can differ in behaviour while looking identical, so the About page
 * shows what is actually installed: version, build date, and the SHA-256 of the
 * APK file this process was loaded from.
 */
object ApkInfo {

    fun apkFile(context: Context): File = File(context.applicationInfo.sourceDir)

    /** Full hex digest. Call off the main thread: it reads the whole APK. */
    fun sha256(context: Context): String? = runCatching {
        val md = MessageDigest.getInstance("SHA-256")
        apkFile(context).inputStream().use { input ->
            val buf = ByteArray(64 * 1024)
            while (true) {
                val n = input.read(buf)
                if (n <= 0) break
                md.update(buf, 0, n)
            }
        }
        md.digest().joinToString("") { "%02x".format(it) }
    }.getOrNull()

    /** Short form for display: 16 hex chars. */
    fun shortHash(context: Context): String? = sha256(context)?.take(16)
}
