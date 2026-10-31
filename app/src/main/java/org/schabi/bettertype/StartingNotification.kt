package org.schabi.bettertype

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Handler
import android.os.Looper

/**
 * Shows how far the languages have loaded, in its own channel so the keep-alive notification can be switched off
 * without hiding it. Not part of [KeepAliveService], because after a force stop Android starts only the spell
 * checker and won't let it start the keep-alive service from the background.
 */
object StartingNotification {
    private const val CHANNEL = "loading"
    private const val ID = 2

    private val handler = Handler(Looper.getMainLooper())
    private var context: Context? = null

    /** Refreshes the notification every second while a language loads, and removes it when nothing does. */
    private val update = object : Runnable {
        override fun run() {
            val context = context ?: return
            // Cancel before reading, so a change posted meanwhile by the warm-up thread isn't lost.
            handler.removeCallbacks(this)
            val loading = LtLanguages.loading()
            val manager = context.getSystemService(NotificationManager::class.java)
            if (loading == null) {
                manager.cancel(ID)
            } else {
                manager.notify(ID, notification(context, loading))
                handler.postDelayed(this, 1000)
            }
        }
    }

    /** Called from the main thread by every service of the engine process; the first call sets it up. */
    fun attach(context: Context) {
        if (this.context != null) return
        val app = context.applicationContext
        this.context = app
        app.getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL, app.getString(R.string.loading_channel), NotificationManager.IMPORTANCE_LOW),
        )
        LtLanguages.onLoadingChanged = {
            handler.removeCallbacks(update)
            handler.post(update)
        }
        // Also removes a notification left behind when the previous engine process was killed mid-load.
        handler.post(update)
    }

    private fun notification(context: Context, loading: LtLanguages.Loading): Notification {
        val builder = Notification.Builder(context, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_menu_edit)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
        if (loading.percent == null) {
            builder.setContentTitle(context.getString(R.string.loading_language, LtLanguages.displayName(loading.language.locale)))
                .setProgress(0, 0, true)
        } else {
            builder.setContentTitle(context.getString(R.string.loading_language_percent, LtLanguages.displayName(loading.language.locale), loading.percent))
                .setProgress(100, loading.percent, false)
        }
        return builder.build()
    }
}
