package dev.cyberdeck.qs

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA
import android.os.IBinder
import androidx.core.app.NotificationCompat

class CameraService : Service() {

    companion object {
        const val CHANNEL_ID = "CameraServiceChannel"
        const val NOTIFICATION_ID = 1
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Foreground Service")
            .setContentText("Service is running in the foreground")

            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .build()

        startForeground(
            NOTIFICATION_ID,
            notification,
            FOREGROUND_SERVICE_TYPE_CAMERA
        )

        return START_STICKY
    }

    on

    private fun createNotificationChannel() {
        val serviceChannel = NotificationChannel(
            CHANNEL_ID,
            "Camera Service Channel",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(serviceChannel)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}