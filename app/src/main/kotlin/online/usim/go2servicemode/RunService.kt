package online.usim.go2servicemode

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder

/**
 * Keeps the process out of Samsung's app-freezing path while the exploit runs.
 *
 * The exploit needs 20-30 s of uninterrupted execution in this process; a plain
 * Activity gets frozen by Freecess in the background, which is exactly what made
 * earlier runs die immediately after "1. libc++: mutex acquired, forking".
 * UniRoot solved this the same way - a foreground service with a notification.
 *
 * The service does nothing else: no engine logic, no scheduling, no wake locks.
 */
class RunService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        ensureChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val text = intent?.getStringExtra(EXTRA_TEXT) ?: "正在运行，请勿操作手机"
        startForeground(NOTIFICATION_ID, buildNotification(text))
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java)
            if (nm.getNotificationChannel(CHANNEL_ID) == null) {
                nm.createNotificationChannel(
                    NotificationChannel(
                        CHANNEL_ID,
                        "运行状态",
                        NotificationManager.IMPORTANCE_LOW
                    ).apply {
                        description = "漏洞执行期间保持进程活跃"
                        setShowBadge(false)
                    }
                )
            }
        }
    }

    private fun buildNotification(text: String): Notification =
        Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("go2servicemode")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()

    companion object {
        private const val CHANNEL_ID = "run"
        private const val NOTIFICATION_ID = 1001
        const val EXTRA_TEXT = "text"
    }
}
