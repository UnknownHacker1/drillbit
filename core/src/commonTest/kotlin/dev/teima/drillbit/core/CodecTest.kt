package dev.teima.drillbit.core

import dev.teima.drillbit.core.codec.InvalidShowCodeException
import dev.teima.drillbit.core.codec.ShowCodec
import dev.teima.drillbit.core.codec.extractShowCode
import dev.teima.drillbit.core.codec.shareLink
import dev.teima.drillbit.core.demo.demoShow
import dev.teima.drillbit.core.model.Show
import dev.teima.drillbit.core.plan.analyzeTransition
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CodecTest {
    @Test
    fun sharingKeepsEveryMoveExactlyTheSame() {
        // Spots are snapped to the quarter-step grid when they are made, so the code is lossless
        // and a scanned show gets the same proof badges as the drill writer's copy.
        val show = demoShow()
        val decoded = ShowCodec.decode(ShowCodec.encode(show))
        assertEquals(show.sets.map { it.positions }, decoded.sets.map { it.positions })

        fun Show.move(i: Int) = analyzeTransition(sets[i - 1].positions, sets[i].positions, sets[i].counts)
        for (i in 1 until show.sets.size) {
            val before = show.move(i)
            val after = decoded.move(i)
            assertTrue(before.provablySafe, "move into set ${i + 1} should be provably safe")
            assertEquals(before.provablySafe, after.provablySafe)
            assertEquals(before.collisions.size, after.collisions.size)
        }
    }

    @Test
    fun roundTripsTheDemoShowToAQuarterStep() {
        val show = demoShow()
        val decoded = ShowCodec.decode(ShowCodec.encode(show))
        assertEquals(show.title, decoded.title)
        assertEquals(show.tempoBpm, decoded.tempoBpm)
        assertEquals(show.field, decoded.field)
        assertEquals(show.performers, decoded.performers)
        assertEquals(show.sets.map { it.name to it.counts }, decoded.sets.map { it.name to it.counts })
        for ((original, copy) in show.sets.zip(decoded.sets)) {
            for ((a, b) in original.positions.zip(copy.positions)) {
                assertTrue(abs(a.x - b.x) <= 0.125 && abs(a.y - b.y) <= 0.125)
            }
        }
    }

    @Test
    fun encodingIsStableAfterTheFirstRoundTrip() {
        val once = ShowCodec.decode(ShowCodec.encode(demoShow()))
        assertEquals(once, ShowCodec.decode(ShowCodec.encode(once)))
    }

    @Test
    fun demoShowFitsComfortablyInAQrCode() {
        val code = ShowCodec.encode(demoShow())
        assertTrue(code.length < 1500, "code is ${code.length} chars")
        assertTrue(code.all { it.isLetterOrDigit() || it == '-' || it == '_' }, "code must be URL safe")
    }

    @Test
    fun rejectsDamagedCodes() {
        val code = ShowCodec.encode(demoShow())
        val flipped = code.substring(0, 20) + (if (code[20] == 'A') 'B' else 'A') + code.substring(21)
        assertFailsWith<InvalidShowCodeException> { ShowCodec.decode(flipped) }
        assertFailsWith<InvalidShowCodeException> { ShowCodec.decode(code.dropLast(10)) }
        assertFailsWith<InvalidShowCodeException> { ShowCodec.decode("hello") }
        assertFailsWith<InvalidShowCodeException> { ShowCodec.decode("***") }
    }

    @Test
    fun extractsCodesFromLinksAndPastes() {
        val code = ShowCodec.encode(demoShow())
        val link = shareLink("https://example.github.io/drillbit/", code)
        assertEquals("https://example.github.io/drillbit/#show=$code", link)
        assertEquals(code, extractShowCode(link))
        assertEquals(code, extractShowCode("  $code \n"))
        assertEquals("abc", extractShowCode("https://x.y/?show=abc&utm=1"))
        assertNull(extractShowCode("   "))
        assertNull(extractShowCode("not a code!"))
    }
}
