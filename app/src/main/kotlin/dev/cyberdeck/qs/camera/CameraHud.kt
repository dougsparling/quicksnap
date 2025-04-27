package dev.cyberdeck.qs.camera

import android.Manifest
import android.R
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
import androidx.annotation.DrawableRes
import androidx.camera.core.CameraSelector.DEFAULT_BACK_CAMERA
import androidx.camera.core.CameraSelector.DEFAULT_FRONT_CAMERA
import androidx.core.app.ActivityCompat
import androidx.lifecycle.LifecycleService
import dev.cyberdeck.qs.camera.CameraController.CaptureSpec
import dev.cyberdeck.qs.camera.CameraController.PhotoSpec
import dev.cyberdeck.qs.camera.CameraController.VideoSpec
import dev.cyberdeck.qs.common.debug
import dev.cyberdeck.qs.common.prepStorageDir
import java.io.File
import kotlin.time.Duration.Companion.seconds

class CameraHud : LifecycleService() {

    companion object {
        const val CHANNEL_ID = "CameraHudChannel"
        const val NOTIFICATION_ID = 2
        const val HUD_ACTION_NAME = "ACTION_SPEC"

        val ACTIONS = listOf(
            HudAction(
                "BR",
                R.drawable.ic_media_play,
                VideoSpec(2.seconds, 7.seconds, DEFAULT_BACK_CAMERA)
            ),
            HudAction(
                "B3",
                R.drawable.ic_media_rew,
                PhotoSpec(1.seconds, 3, DEFAULT_BACK_CAMERA)
            ),
            HudAction(
                "B1",
                R.drawable.ic_media_previous,
                PhotoSpec(3.seconds, 1, DEFAULT_BACK_CAMERA)
            ),
            HudAction(
                "F1",
                R.drawable.ic_media_next,
                PhotoSpec(3.seconds, 1, DEFAULT_FRONT_CAMERA)
            ),
            HudAction(
                "F3",
                R.drawable.ic_media_ff,
                PhotoSpec(1.seconds, 3, DEFAULT_FRONT_CAMERA)
            ),
        )
    }

    private lateinit var cameraController: CameraController

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        cameraController = CameraController(applicationContext, prepStorageDir())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        when (val label = intent?.getStringExtra(HUD_ACTION_NAME)) {
            null -> onStart()
            else -> onStartCapture(ACTIONS.find { it.name == label }!!.spec)
        }

        return START_STICKY
    }

    private fun onStartCapture(spec: CaptureSpec) {
        debug("$spec")
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            cameraController.capture(spec)
        } else {
            debug("Camera permission not granted")
        }
    }

    private fun onStart() {
        val notification = Notification.Builder(this, CHANNEL_ID)
            .setStyle(Notification.MediaStyle())
            .setSmallIcon(dev.cyberdeck.qs.R.drawable.ic_launcher_foreground)
            .setOngoing(true)

        ACTIONS.forEachIndexed { index, spec ->
            notification.addAction(
                Notification.Action.Builder(
                    Icon.createWithResource("", spec.icon),
                    spec.name,
                    captureIntent(spec.name, index)
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
        Intent(this, CameraHud::class.java).apply {
            putExtra(HUD_ACTION_NAME, specName)
        },
        PendingIntent.FLAG_IMMUTABLE
    )

    private fun createNotificationChannel() {
        val hudChannel = NotificationChannel(
            CHANNEL_ID,
            "Snap UI",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(hudChannel)
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null
    }

    data class HudAction(
        val name: String,
        @DrawableRes val icon: Int,
        val spec: CaptureSpec
    )
}