package dev.teima.drillbit.platform

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.ClipEntry
import java.awt.datatransfer.StringSelection
import java.io.File
import java.util.concurrent.Executors
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import kotlin.math.PI
import kotlin.math.sin

actual fun platformName(): String = "Desktop (JVM ${System.getProperty("java.version")})"

actual fun webAppUrl(): String = PUBLIC_WEB_APP_URL

private val audio = Executors.newSingleThreadExecutor { Thread(it, "drillbit-metronome").apply { isDaemon = true } }

actual fun playTick(accent: Boolean) {
    audio.execute {
        runCatching {
            val rate = SAMPLE_RATE
            val format = AudioFormat(rate, 8, 1, true, false)
            val frequency = if (accent) ACCENT_HZ else TICK_HZ
            val samples = (rate * TICK_SECONDS).toInt()
            val buffer =
                ByteArray(samples) { i ->
                    val fade = 1.0 - i.toDouble() / samples
                    (sin(2 * PI * frequency * i / rate) * VOLUME * fade).toInt().toByte()
                }
            AudioSystem.getSourceDataLine(format).use { line ->
                line.open(format)
                line.start()
                line.write(buffer, 0, buffer.size)
                line.drain()
            }
        }
    }
}

/** Saves the show as a text file in the user's home folder. */
class FileShowStorage(
    private val file: File = File(System.getProperty("user.home"), ".drillbit/show.txt"),
) : ShowStorage {
    override fun load(): String? = runCatching { file.readText().trim() }.getOrNull()?.takeIf { it.isNotEmpty() }

    override fun save(code: String) {
        runCatching {
            file.parentFile?.mkdirs()
            file.writeText(code)
        }
    }
}

private const val SAMPLE_RATE = 22_050f
private const val TICK_HZ = 1_200.0
private const val ACCENT_HZ = 1_800.0
private const val TICK_SECONDS = 0.04
private const val VOLUME = 90.0

@OptIn(ExperimentalComposeUiApi::class)
actual fun textClipEntry(text: String): ClipEntry = ClipEntry(StringSelection(text))
