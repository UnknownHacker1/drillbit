package dev.teima.drillbit

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import dev.teima.drillbit.platform.BrowserShowStorage
import dev.teima.drillbit.platform.currentHash

/** Web entry point. A scanned QR code opens this page with the show in the `#show=` fragment. */
@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    val launchLink = currentHash().takeIf { it.contains("show=") }
    removeLoadingMessage()
    ComposeViewport {
        App(storage = BrowserShowStorage, launchLink = launchLink)
    }
}

private fun removeLoadingMessage(): Unit = js("{ const el = document.getElementById('loading'); if (el) el.remove(); }")
