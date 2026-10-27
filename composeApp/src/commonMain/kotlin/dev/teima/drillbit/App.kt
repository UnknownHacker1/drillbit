package dev.teima.drillbit

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import dev.teima.drillbit.core.codec.ShowCodec
import dev.teima.drillbit.core.codec.extractShowCode
import dev.teima.drillbit.core.demo.demoShow
import dev.teima.drillbit.core.model.Show
import dev.teima.drillbit.platform.NoStorage
import dev.teima.drillbit.platform.ShowStorage
import dev.teima.drillbit.ui.DrillbitApp
import dev.teima.drillbit.ui.state.DrillbitState
import dev.teima.drillbit.ui.state.Mode
import dev.teima.drillbit.ui.theme.DrillbitTheme

/**
 * Entry point shared by every platform.
 *
 * @param launchLink a link or show code the app was opened with (a scanned QR code on the
 *   web, an intent on Android, a command-line argument on desktop). Opening a link starts in
 *   performer mode, because that is who scans QR codes.
 */
@Composable
fun App(
    storage: ShowStorage = NoStorage,
    launchLink: String? = null,
) {
    val state =
        remember {
            val linked = launchLink?.let(::decodeOrNull)
            val initial = linked ?: storage.load()?.let(::decodeOrNull) ?: demoShow()
            DrillbitState(initial, storage, if (linked != null) Mode.PERFORM else Mode.DESIGN)
        }
    DrillbitTheme {
        DrillbitApp(state)
    }
}

private fun decodeOrNull(text: String): Show? =
    extractShowCode(text)?.let {
        runCatching { ShowCodec.decode(it) }.getOrNull()
    }
