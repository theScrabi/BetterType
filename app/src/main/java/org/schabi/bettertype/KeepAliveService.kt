package org.schabi.bettertype

import android.app.ActivityManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Process
import android.util.Log

/**
 * Keeps the process, and with it the loaded LanguageTool languages, in memory. Without it Android drops the
 * spell checker process soon after the last text field closes, and the next check reloads the language
 * (German: over a minute on a phone). Only a foreground service is reliably kept alive.
 * Load progress is shown separately by [StartingNotification].
 */
class KeepAliveService : Service() {

    override fun onBind(intent: Intent?) = null

    override fun onCreate() {
        super.onCreate()
        StartingNotification.attach(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL, getString(R.string.keep_alive_channel), NotificationManager.IMPORTANCE_MIN),
        )
        val keepAlive = Notification.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_menu_edit)
            .setContentTitle(getString(R.string.keep_alive_text))
            .build()
        startForeground(ID, keepAlive, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        val languages = intent?.getStringArrayExtra(EXTRA_LANGUAGES)
        if (languages != null) LtLanguages.setEnabled(this, languages.toSet()) else LtLanguages.warmUpEnabled(this)
        return START_STICKY
    }

    companion object {
        private const val CHANNEL = "keep_alive"
        private const val ID = 1

        private const val EXTRA_LANGUAGES = "languages"

        /** Android only allows this while the app is in the foreground or right after boot or an update. */
        fun start(context: Context, intent: Intent = Intent(context, KeepAliveService::class.java)) {
            try {
                context.startForegroundService(intent)
            } catch (e: IllegalStateException) {
                Log.w("BetterType", "Could not start the keep-alive service: ${e.message}")
            }
        }

        /**
         * Saves the language choice and hands it to the engine process. LanguageTool keeps most of a language in
         * global caches that can't be dropped, so switching one off restarts the engine process, which also stops a
         * load that is still running; the restarted process loads only the languages still enabled.
         */
        fun applyLanguages(context: Context, codes: Set<String>) {
            val removed = LtLanguages.enabled(context) - codes
            LtLanguages.saveEnabled(context, codes)
            if (removed.isNotEmpty()) {
                val engine = "${context.packageName}:lt"
                context.getSystemService(ActivityManager::class.java).runningAppProcesses
                    ?.firstOrNull { it.processName == engine }
                    ?.let { Process.killProcess(it.pid) }
            }
            start(context, Intent(context, KeepAliveService::class.java).putExtra(EXTRA_LANGUAGES, codes.toTypedArray()))
        }
    }
}

class StartKeepAliveReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) = KeepAliveService.start(context)
}
