package dev.teima.drillbit.core

import dev.teima.drillbit.core.codec.ShowCodec
import dev.teima.drillbit.core.codec.shareLink
import dev.teima.drillbit.core.demo.demoShow
import dev.teima.drillbit.core.qr.ErrorCorrection
import dev.teima.drillbit.core.qr.QrCode
import java.io.File
import kotlin.test.Test

/**
 * Writes sample QR codes as PGM images when `DRILLBIT_QR_EXPORT` points at a folder, so they
 * can be checked with an independent decoder (for example OpenCV's `QRCodeDetector`).
 */
class QrExportTest {
    @Test
    fun exportSamplesForExternalDecoding() {
        val folder = System.getenv("DRILLBIT_QR_EXPORT")?.let(::File) ?: return
        folder.mkdirs()
        val samples =
            mapOf(
                "v1" to "DB",
                "kotlinconf" to "https://kotlinconf.com/contest",
                "medium" to "x".repeat(300),
                "demo" to shareLink("https://example.github.io/drillbit/", ShowCodec.encode(demoShow())),
            )
        for ((name, text) in samples) {
            val qr = QrCode.encodeText(text, ErrorCorrection.LOW)
            File(folder, "$name.txt").writeText(text)
            File(folder, "$name.pgm").writeBytes(toPgm(qr))
        }
    }

    private fun toPgm(qr: QrCode): ByteArray {
        val scale = 6
        val border = 4
        val side = (qr.size + border * 2) * scale
        val header = "P5\n$side $side\n255\n".toByteArray()
        val pixels = ByteArray(side * side)
        for (py in 0 until side) {
            for (px in 0 until side) {
                val x = px / scale - border
                val y = py / scale - border
                val dark = x in 0 until qr.size && y in 0 until qr.size && qr[x, y]
                pixels[py * side + px] = if (dark) 0 else -1
            }
        }
        return header + pixels
    }
}
