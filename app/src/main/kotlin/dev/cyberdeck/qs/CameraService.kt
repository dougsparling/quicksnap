package dev.cyberdeck.qs

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA
import android.graphics.drawable.Icon
import android.os.Environment
import android.os.IBinder
import android.util.Log
import androidx.camera.core.CameraSelector.DEFAULT_BACK_CAMERA
import androidx.camera.core.CameraSelector.DEFAULT_FRONT_CAMERA
import androidx.core.app.ActivityCompat
import androidx.lifecycle.LifecycleService
import dev.cyberdeck.qs.CameraController.CaptureSpec
import dev.cyberdeck.qs.CameraController.PhotoSpec
import dev.cyberdeck.qs.CameraController.VideoSpec
import java.io.File
import kotlin.time.Duration.Companion.seconds

class CameraService : LifecycleService() {

    companion object {
        const val CHANNEL_ID = "CameraServiceChannel"
        const val NOTIFICATION_ID = 2
        const val ACTION_SPEC_NAME = "ACTION_SPEC"

        val SPECS = listOf(
            VideoSpec("BR", android.R.drawable.ic_media_play, 2.seconds, 5.seconds, DEFAULT_BACK_CAMERA),
            PhotoSpec("B3", android.R.drawable.ic_media_rew, 3.seconds, 3, DEFAULT_BACK_CAMERA),
            PhotoSpec("B1", android.R.drawable.ic_media_previous, 3.seconds, 1, DEFAULT_BACK_CAMERA),
            PhotoSpec("F1", android.R.drawable.ic_media_next, 3.seconds, 1, DEFAULT_FRONT_CAMERA),
            PhotoSpec("F3", android.R.drawable.ic_media_ff, 3.seconds, 3, DEFAULT_FRONT_CAMERA),
        )
    }

    private lateinit var cameraController: CameraController

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        cameraController = CameraController(applicationContext, getStorageDir("pics"))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        when (val spec = intent?.getStringExtra(ACTION_SPEC_NAME)) {
            null -> onStart()
            else -> onStartCapture(SPECS.find { it.label == spec }!!)
        }

        return START_STICKY
    }

    private fun onStartCapture(spec: CaptureSpec) {
        Log.d("CameraService", "onStartCapture: $spec")
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            cameraController.capture(spec)
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
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)

        SPECS.forEachIndexed { index, spec ->
            notification.addAction(
                Notification.Action.Builder(
                    Icon.createWithResource("", spec.icon),
                    spec.label,
                    captureIntent(spec.label, index)
                ).build()
            )
        }

        startForeground(
            NOTIFICATION_ID,
            notification.build(),
            FOREGROUND_SERVICE_TYPE_CAMERA
        )
    }

    private fun captureIntent(specName: String, requestCode: Int) = PendingIntent.getService(
        this,
        requestCode,
        Intent(this, CameraService::class.java).apply {
            putExtra(ACTION_SPEC_NAME, specName)
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