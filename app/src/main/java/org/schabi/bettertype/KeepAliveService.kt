package org.schabi.bettertype

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Handler
import android.os.Looper
import android.util.Log

/**
 * Keeps the process, and with it the loaded LanguageTool languages, in memory. Without it Android drops the
 * spell checker process soon after the last text field closes, and the next check reloads the language
 * (German: over a minute on a phone). Only a foreground service is reliably kept alive.
 * Its notification shows how far the languages have loaded.
 */
class KeepAliveService : Service() {

    private val handler = Handler(Looper.getMainLooper())

    /** Refreshes the notification every second while a language loads. */
    private val update = object : Runnable {
        override fun run() {
            val loading = LtLanguages.loading()
            getSystemService(NotificationManager::class.java).notify(ID, notification(loading))
            handler.removeCallbacks(this)
            if (loading != null) handler.postDelayed(this, 1000)
        }
    }

    override fun onBind(intent: Intent?) = null

    override fun onCreate() {
        super.onCreate()
        LtLanguages.onLoadingChanged = {
            handler.removeCallbacks(update)
            handler.post(update)
        }
    }

    override fun onDestroy() {
        LtLanguages.onLoadingChanged = null
        handler.removeCallbacks(update)
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL, getString(R.string.keep_alive_channel), NotificationManager.IMPORTANCE_MIN),
        )
        startForeground(ID, notification(LtLanguages.loading()), ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        val locales = resources.configuration.locales
        for (i in 0 until locales.size()) LtLanguages.warmUp(this, locales[i])
        return START_STICKY
    }

    private fun notification(loading: LtLanguages.Loading?): Notification {
        val builder = Notification.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_menu_edit)
            .setOnlyAlertOnce(true)
        when {
            loading == null -> builder.setContentTitle(getString(R.string.keep_alive_text))
            loading.percent == null -> builder
                .setContentTitle(getString(R.string.loading_language, loading.language.name))
                .setProgress(0, 0, true)
            else -> builder
                .setContentTitle(getString(R.string.loading_language_percent, loading.language.name, loading.percent))
                .setProgress(100, loading.percent, false)
        }
        return builder.build()
    }

    companion object {
        private const val CHANNEL = "keep_alive"
        private const val ID = 1

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
