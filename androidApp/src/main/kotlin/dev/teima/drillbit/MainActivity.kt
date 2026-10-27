package dev.teima.drillbit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dev.teima.drillbit.platform.PreferencesShowStorage

/** Android entry point. Opening a Drillbit share link with the app loads that show. */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val storage = PreferencesShowStorage(applicationContext)
        val launchLink = intent?.dataString
        setContent {
            App(storage = storage, launchLink = launchLink)
        }
    }
}
