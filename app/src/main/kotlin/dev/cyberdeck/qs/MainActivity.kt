package dev.cyberdeck.qs

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.ui.platform.LocalContext
import dev.cyberdeck.qs.camera.CameraHud
import dev.cyberdeck.qs.ui.MainView

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val serviceIntent = Intent(this, CameraHud::class.java)
        startForegroundService(serviceIntent)

        setContent {
            val colorScheme = when {
                isSystemInDarkTheme() -> dynamicDarkColorScheme(LocalContext.current)
                else -> dynamicLightColorScheme(LocalContext.current)
            }

            MaterialTheme(colorScheme = colorScheme) {
                MainView()
            }
        }
    }
}