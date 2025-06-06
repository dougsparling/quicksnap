package dev.cyberdeck.qs.camera

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA
import android.graphics.drawable.Icon
import android.os.IBinder
import android.view.View
import android.widget.RemoteViews
import androidx.annotation.DrawableRes
import androidx.camera.core.CameraSelector.DEFAULT_BACK_CAMERA
import androidx.camera.core.CameraSelector.DEFAULT_FRONT_CAMERA
import androidx.core.app.ActivityCompat
import androidx.lifecycle.LifecycleService
import dev.cyberdeck.qs.R
import dev.cyberdeck.qs.camera.CameraController.CaptureSpec
import dev.cyberdeck.qs.camera.CameraController.PhotoSpec
import dev.cyberdeck.qs.camera.CameraController.VideoSpec
import dev.cyberdeck.qs.common.debug
import dev.cyberdeck.qs.common.prepStorageDir
import kotlin.time.Duration.Companion.seconds

class CameraHud : LifecycleService() {

    companion object {
        const val CHANNEL_ID = "CameraHudChannel"
        const val NOTIFICATION_ID = 2
        const val HUD_ACTION_NAME = "ACTION_SPEC"

        val ACTIONS = listOf(
            HudAction(
                "F3",
                R.drawable.keyboard_arrow_down,
                PhotoSpec(1.seconds, 3, DEFAULT_FRONT_CAMERA)
            ),
            HudAction(
                "FR",
                R.drawable.vertical_align_bottom,
                VideoSpec(2.seconds, 10.seconds, DEFAULT_FRONT_CAMERA)
            ),
            HudAction(
                "BR",
                R.drawable.vertical_align_top,
                VideoSpec(2.seconds, 10.seconds, DEFAULT_BACK_CAMERA)
            ),
            HudAction(
                "B3",
                R.drawable.keyboard_arrow_up,
                PhotoSpec(1.seconds, 3, DEFAULT_BACK_CAMERA)
            ),
        )

        val BUTTON_BINDINGS = listOf(
            R.id.action_button_1,
            R.id.action_button_2,
            R.id.action_button_3,
            R.id.action_button_4,
            R.id.action_button_5
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
        val remoteViews = RemoteViews(packageName, R.layout.custom_notification_layout)

        val notification = Notification.Builder(this, CHANNEL_ID)
            .setStyle(Notification.DecoratedCustomViewStyle())
            .setCustomContentView(remoteViews)
            .setSmallIcon(android.R.drawable.ic_menu_gallery) // Still need a small icon for the status bar
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setOngoing(true)

        BUTTON_BINDINGS.forEachIndexed { index, binding ->
            when (val action = ACTIONS.getOrNull(index)) {
                null -> remoteViews.setViewVisibility(binding, View.GONE)
                else -> {
                    remoteViews.setImageViewResource(binding, action.icon)
                    remoteViews.setOnClickPendingIntent(binding, captureIntent(action.name, index))
                }
            }
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
            "QuickSnap UI",
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
