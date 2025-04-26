package dev.cyberdeck.qs

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA
import android.graphics.drawable.Icon
import android.os.IBinder
import android.util.Log

class CameraService : Service() {

    companion object {
        const val CHANNEL_ID = "CameraServiceChannel"
        const val NOTIFICATION_ID = 2
        const val ACTION_FRONT = "ACTION_FRONT"
        const val ACTION_BACK = "ACTION_BACK"
        const val ACTION_EXTRA = "ACTION_EXTRA"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.getStringExtra(ACTION_EXTRA)) {
            ACTION_FRONT -> onFront()
            ACTION_BACK -> onBack()
            null -> onStart()
        }

        return START_STICKY
    }

    private fun onFront() {
        Log.d("CameraService", "onFront")
    }

    private fun onBack() {
        Log.d("CameraService", "onBack")
    }

    private fun onStart() {
        val notification = Notification.Builder(this, CHANNEL_ID)
            .setStyle(Notification.MediaStyle())
            .addAction(
                Notification.Action.Builder(
                    Icon.createWithResource(
                        "",
                        android.R.drawable.ic_media_previous
                    ), "Back", serviceIntent(ACTION_BACK, 0)
                ).build()
            )
            .addAction(
                Notification.Action.Builder(
                    Icon.createWithResource("", android.R.drawable.ic_media_next),
                    "Front",
                    serviceIntent(ACTION_FRONT, 1)
                ).build()
            )
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .build()

        startForeground(
            NOTIFICATION_ID,
            notification,
            FOREGROUND_SERVICE_TYPE_CAMERA
        )
    }

    private fun serviceIntent(action: String, requestCode: Int) = PendingIntent.getService(
        this,
        requestCode,
        Intent(this, CameraService::class.java).apply {
            putExtra(ACTION_EXTRA, action)
        },
        PendingIntent.FLAG_IMMUTABLE
    )

    private fun createNotificationChannel() {
        val serviceChannel = NotificationChannel(
            CHANNEL_ID,
            "Snap UI",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(serviceChannel)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}