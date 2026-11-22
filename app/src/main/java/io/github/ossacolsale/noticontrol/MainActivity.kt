package io.github.ossacolsale.noticontrol

import android.content.ActivityNotFoundException
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect

class MainActivity : ComponentActivity() {
    private val model: InventoryViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val dark = isSystemInDarkTheme()
            val colors = if (Build.VERSION.SDK_INT >= 31) {
                if (dark) dynamicDarkColorScheme(this) else dynamicLightColorScheme(this)
            } else if (dark) {
                darkColorScheme(primary = Color(0xFF9BD4C4), onPrimary = Color(0xFF07372D))
            } else {
                lightColorScheme(primary = Color(0xFF285E53), onPrimary = Color.White)
            }
            MaterialTheme(colorScheme = colors) {
                LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { model.refresh() }
                InventoryScreen(model, ::openNativeSettings) {
                    try {
                        startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                    } catch (_: ActivityNotFoundException) {
                        Toast.makeText(this, R.string.no_access_screen, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }
}
