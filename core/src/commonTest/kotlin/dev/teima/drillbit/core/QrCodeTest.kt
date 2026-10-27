package dev.teima.drillbit.core

import dev.teima.drillbit.core.qr.ErrorCorrection
import dev.teima.drillbit.core.qr.QrCode
import dev.teima.drillbit.core.qr.ReedSolomon
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class QrCodeTest {
    @Test
    fun reedSolomonMatchesTheHelloWorldExample() {
        // Version 1-M "HELLO WORLD": the worked example used by most QR tutorials.
        val data = intArrayOf(32, 91, 11, 120, 209, 114, 220, 77, 67, 64, 236, 17, 236, 17, 236, 17)
        val ecc = ReedSolomon.remainder(data, ReedSolomon.divisor(10))
        assertContentEquals(intArrayOf(196, 35, 39, 119, 235, 215, 231, 226, 93, 23), ecc)
    }

    @Test
    fun fieldMultiplicationIsCommutativeAndHasIdentity() {
        for (a in 0..255 step 7) {
            assertEquals(a, ReedSolomon.multiply(a, 1))
            for (b in 0..255 step 13) assertEquals(ReedSolomon.multiply(a, b), ReedSolomon.multiply(b, a))
        }
    }

    @Test
    fun picksTheSmallestVersionThatFits() {
        // Byte-mode capacities at level L: version 1 holds 17 bytes, version 2 holds 32.
        assertEquals(1, QrCode.encodeBytes(ByteArray(17)).version)
        assertEquals(2, QrCode.encodeBytes(ByteArray(18)).version)
        assertEquals(40, QrCode.encodeBytes(ByteArray(2953)).version)
        assertFailsWith<IllegalArgumentException> { QrCode.encodeBytes(ByteArray(2954)) }
    }

    @Test
    fun boostsErrorCorrectionWhenThereIsRoom() {
        val qr = QrCode.encodeText("DB")
        assertEquals(1, qr.version)
        assertEquals(ErrorCorrection.HIGH, qr.errorCorrection)
    }

    @Test
    fun drawsFinderPatternsInThreeCorners() {
        val qr = QrCode.encodeText("https://kotlinconf.com")
        val size = qr.size
        assertEquals(qr.version * 4 + 17, size)
        for ((cx, cy) in listOf(3 to 3, size - 4 to 3, 3 to size - 4)) {
            assertTrue(qr[cx, cy], "finder centre is dark")
            assertTrue(!qr[cx + 2, cy], "finder has a light ring")
            assertTrue(qr[cx + 3, cy], "finder has a dark outer ring")
        }
        assertTrue(qr[8, size - 8], "the always-dark module is set")
    }

    @Test
    fun timingPatternsAlternate() {
        val qr = QrCode.encodeText("timing")
        for (i in 8 until qr.size - 8) {
            assertEquals(i % 2 == 0, qr[i, 6])
            assertEquals(i % 2 == 0, qr[6, i])
        }
    }
}
