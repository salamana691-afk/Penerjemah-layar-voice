package com.autodub.edge

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.autodub.edge.core.constants.AppConstants
import com.autodub.edge.core.di.AppContainer

class AutoDubApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java) ?: return

            val captureChannel = NotificationChannel(
                AppConstants.NOTIFICATION_CHANNEL_CAPTURE,
                getString(R.string.capture_notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.capture_notification_channel_desc)
                setShowBadge(false)
            }

            val overlayChannel = NotificationChannel(
                AppConstants.NOTIFICATION_CHANNEL_OVERLAY,
                getString(R.string.overlay_notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.overlay_notification_channel_desc)
                setShowBadge(false)
            }

            manager.createNotificationChannel(captureChannel)
            manager.createNotificationChannel(overlayChannel)
        }
    }
}
