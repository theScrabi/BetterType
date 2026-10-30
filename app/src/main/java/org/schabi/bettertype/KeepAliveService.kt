package org.schabi.bettertype

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.util.Log

/**
 * Keeps the process, and with it the loaded LanguageTool languages, in memory. Without it Android drops the
 * spell checker process soon after the last text field closes, and the next check reloads the language
 * (German: over a minute on a phone). Only a foreground service is reliably kept alive.
 */
class KeepAliveService : Service() {

    override fun onBind(intent: Intent?) = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL, getString(R.string.keep_alive_channel), NotificationManager.IMPORTANCE_MIN),
        )
        val notification = Notification.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_menu_edit)
            .setContentTitle(getString(R.string.keep_alive_text))
            .build()
        startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        val locales = resources.configuration.locales
        for (i in 0 until locales.size()) LtLanguages.warmUp(locales[i])
        return START_STICKY
    }

    companion object {
        private const val CHANNEL = "keep_alive"

        /** Android only allows this while the app is in the foreground or right after boot or an update. */
        fun start(context: Context) {
            try {
                context.startForegroundService(Intent(context, KeepAliveService::class.java))
            } catch (e: IllegalStateException) {
                Log.w("BetterType", "Could not start the keep-alive service: ${e.message}")
            }
        }
    }
}

class StartKeepAliveReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) = KeepAliveService.start(context)
}
