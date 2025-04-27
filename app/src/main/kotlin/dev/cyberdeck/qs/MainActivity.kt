package dev.cyberdeck.qs

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val serviceIntent = Intent(this, CameraService::class.java)
        startForegroundService(serviceIntent)

        setContent {
            val colorScheme = when {
                isSystemInDarkTheme() -> dynamicDarkColorScheme(LocalContext.current)
                else -> dynamicLightColorScheme(LocalContext.current)
            }

            MaterialTheme(colorScheme = colorScheme) {
                Scaffold { insets ->
                    Box(modifier = Modifier.padding(insets)) {
                        Text("hello world")
                    }
                }
            }
        }
    }
}