package dev.teima.drillbit

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import dev.teima.drillbit.platform.FileShowStorage

/** Desktop entry point. Pass a share link or show code as the first argument to open it. */
fun main(args: Array<String>) =
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "Drillbit",
            state = rememberWindowState(width = 1360.dp, height = 860.dp),
        ) {
            App(storage = FileShowStorage(), launchLink = args.firstOrNull())
        }
    }
