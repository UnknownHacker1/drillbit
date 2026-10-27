package dev.teima.drillbit.platform

import androidx.compose.ui.platform.ClipEntry

/*
 * The whole platform layer of Drillbit. Everything else (model, planner, codec, QR, UI) is
 * common code; each target only supplies these few functions.
 */

/** Human-readable name of the running target, shown in the About line. */
expect fun platformName(): String

/** Base URL of the hosted web app. Share links and QR codes point here. */
expect fun webAppUrl(): String

/** Plays a short metronome tick. Accented ticks mark the first count of a set. */
expect fun playTick(accent: Boolean)

/** Keeps the current show between launches. */
interface ShowStorage {
    fun load(): String?

    fun save(code: String)
}

/** Storage that forgets everything; used in previews and tests. */
object NoStorage : ShowStorage {
    override fun load(): String? = null

    override fun save(code: String) = Unit
}

/** Where the GitHub Pages build of the web app is published. */
const val PUBLIC_WEB_APP_URL: String = "https://unknownhacker1.github.io/drillbit/"

/** Wraps plain text for the Compose clipboard; the entry type differs per platform. */
expect fun textClipEntry(text: String): ClipEntry
