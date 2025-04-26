package dev.cyberdeck.qs

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA
import android.graphics.drawable.Icon
import android.os.Environment
import android.os.IBinder
import android.provider.MediaStore
import android.util.Log
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCapture.CaptureMode
import androidx.camera.core.ImageCaptureException
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.time.Duration.Companion.seconds

class CameraService : LifecycleService() {

    companion object {
        const val CHANNEL_ID = "CameraServiceChannel"
        const val NOTIFICATION_ID = 2
        const val ACTION_FRONT = "ACTION_FRONT"
        const val ACTION_BACK = "ACTION_BACK"
        const val ACTION_EXTRA = "ACTION_EXTRA"
    }

    private lateinit var cameraController: CameraController

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        cameraController = CameraController(applicationContext, getStorageDir("pics"))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        when (intent?.getStringExtra(ACTION_EXTRA)) {
            ACTION_FRONT -> onFront()
            ACTION_BACK -> onBack()
            null -> onStart()
        }

        return START_STICKY
    }

    private fun onFront() {
        Log.d("CameraService", "onFront")
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            cameraController.capture(CameraSelector.DEFAULT_FRONT_CAMERA, 1.seconds, 1)
        } else {
            Log.e("CameraService", "Camera permission not granted")
        }
    }

    private fun onBack() {
        Log.d("CameraService", "onBack")
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            cameraController.capture(CameraSelector.DEFAULT_BACK_CAMERA, 1.seconds, 1)
        } else {
            Log.e("CameraService", "Camera permission not granted")
        }
    }

    private fun getStorageDir(albumName: String) = File(
        getExternalFilesDir(
            Environment.DIRECTORY_PICTURES
        ), albumName
    ).also {
        if (!it.mkdirs()) {
            Log.e("CameraService", "Directory not created")
        }
    }

    private fun onStart() {
        val notification = Notification.Builder(this, CHANNEL_ID)
            .setStyle(Notification.MediaStyle())
            .addAction(
                Notification.Action.Builder(
                    Icon.createWithResource(
                        "",
                        android.R.drawable.ic_media_previous
                    ), getString(R.string.back), serviceIntent(ACTION_BACK, 0)
                ).build()
            )
            .addAction(
                Notification.Action.Builder(
                    Icon.createWithResource("", android.R.drawable.ic_media_next),
                    getString(R.string.front),
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

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null
    }
}