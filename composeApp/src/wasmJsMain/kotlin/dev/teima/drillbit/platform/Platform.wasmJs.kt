package dev.teima.drillbit.platform

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.ClipEntry

actual fun platformName(): String = "Web (Kotlin/Wasm)"

/** The page's own address, so share links work wherever the web build is hosted. */
actual fun webAppUrl(): String = currentPageUrl().substringBefore('#')

actual fun playTick(accent: Boolean) = beep(if (accent) ACCENT_HZ else TICK_HZ)

/** The part of the address after `#`, where share links carry the show code. */
fun currentHash(): String = locationHash()

/** Saves the show in the browser's localStorage. */
object BrowserShowStorage : ShowStorage {
    override fun load(): String? = storageGet(KEY)

    override fun save(code: String) = storageSet(KEY, code)
}

private fun currentPageUrl(): String = js("window.location.href")

private fun locationHash(): String = js("window.location.hash")

private fun storageGet(key: String): String? =
    js("{ try { return window.localStorage.getItem(key); } catch (e) { return null; } }")

private fun storageSet(
    key: String,
    value: String,
): Unit = js("{ try { window.localStorage.setItem(key, value); } catch (e) {} }")

private fun beep(frequency: Double): Unit =
    js(
        """{
        try {
            const ctx = window.drillbitAudio || (window.drillbitAudio = new (window.AudioContext || window.webkitAudioContext)());
            const osc = ctx.createOscillator();
            const gain = ctx.createGain();
            osc.frequency.value = frequency;
            gain.gain.setValueAtTime(0.2, ctx.currentTime);
            gain.gain.exponentialRampToValueAtTime(0.0001, ctx.currentTime + 0.05);
            osc.connect(gain);
            gain.connect(ctx.destination);
            osc.start();
            osc.stop(ctx.currentTime + 0.06);
        } catch (e) {}
    }""",
    )

private const val KEY = "drillbit.show"
private const val TICK_HZ = 1_200.0
private const val ACCENT_HZ = 1_800.0

@OptIn(ExperimentalComposeUiApi::class)
actual fun textClipEntry(text: String): ClipEntry = ClipEntry.withPlainText(text)
