package dev.teima.drillbit.platform

import android.content.ClipData
import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import androidx.compose.ui.platform.ClipEntry

actual fun platformName(): String = "Android ${Build.VERSION.RELEASE}"

actual fun webAppUrl(): String = PUBLIC_WEB_APP_URL

private val tones by lazy { ToneGenerator(AudioManager.STREAM_MUSIC, TONE_VOLUME) }

actual fun playTick(accent: Boolean) {
    tones.startTone(if (accent) ToneGenerator.TONE_PROP_BEEP2 else ToneGenerator.TONE_PROP_BEEP, TICK_MS)
}

/** Saves the show in the app's private shared preferences. */
class PreferencesShowStorage(
    context: Context,
) : ShowStorage {
    private val preferences = context.getSharedPreferences("drillbit", Context.MODE_PRIVATE)

    override fun load(): String? = preferences.getString(KEY, null)

    override fun save(code: String) = preferences.edit().putString(KEY, code).apply()

    private companion object {
        const val KEY = "show"
    }
}

private const val TONE_VOLUME = 60
private const val TICK_MS = 40

actual fun textClipEntry(text: String): ClipEntry = ClipEntry(ClipData.newPlainText("Drillbit", text))
