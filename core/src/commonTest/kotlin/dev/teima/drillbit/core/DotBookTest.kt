package dev.teima.drillbit.core

import dev.teima.drillbit.core.demo.demoShow
import dev.teima.drillbit.core.dotbook.Move
import dev.teima.drillbit.core.dotbook.StrideDifficulty
import dev.teima.drillbit.core.dotbook.describeDirection
import dev.teima.drillbit.core.dotbook.describeFrontToBack
import dev.teima.drillbit.core.dotbook.describeSideToSide
import dev.teima.drillbit.core.dotbook.dotBook
import dev.teima.drillbit.core.dotbook.formatSteps
import dev.teima.drillbit.core.geometry.Vec2
import dev.teima.drillbit.core.model.Field
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DotBookTest {
    @Test
    fun sideToSideUsesYardLinesAndInsideOutside() {
        assertEquals("On the 50", describeSideToSide(80.0))
        assertEquals("Side 1: 2 steps from the 50", describeSideToSide(78.0))
        assertEquals("Side 2: 1 step from the 50", describeSideToSide(81.0))
        assertEquals("Side 1: On 35 yd line", describeSideToSide(56.0))
        assertEquals("Side 1: 2 steps inside 35 yd line", describeSideToSide(58.0))
        assertEquals("Side 1: 3 steps outside 35 yd line", describeSideToSide(53.0))
        assertEquals("Side 2: 2.5 steps inside 20 yd line", describeSideToSide(125.5))
        assertEquals("Side 1: On goal line", describeSideToSide(0.0))
        assertEquals("Side 2: On goal line", describeSideToSide(160.0))
    }

    @Test
    fun frontToBackUsesNearestHashOrSideline() {
        val field = Field.HIGH_SCHOOL
        assertEquals("On front sideline", describeFrontToBack(0.0, field))
        assertEquals("4 steps behind front hash", describeFrontToBack(field.frontHash + 4, field))
        assertEquals("2.25 steps in front of back hash", describeFrontToBack(field.backHash - 2.25, field))
        assertEquals("On front hash", describeFrontToBack(32.0, Field.COLLEGE))
    }

    @Test
    fun stepSizeReadsLikeADrillChart() {
        assertEquals("8 to 5", Move(16.0, 16, "").stepSizeLabel)
        assertEquals("6 to 5", Move(16.0, 12, "").stepSizeLabel)
        assertEquals("16 to 5", Move(8.0, 16, "").stepSizeLabel)
        assertEquals("hold", Move(0.0, 8, "").stepSizeLabel)
        assertEquals(StrideDifficulty.COMFORTABLE, Move(16.0, 16, "").difficulty)
        assertEquals(StrideDifficulty.LARGE, Move(16.0, 12, "").difficulty)
        assertEquals(StrideDifficulty.TOO_LARGE, Move(16.0, 8, "").difficulty)
    }

    @Test
    fun directionsAreFromThePerformersPointOfView() {
        assertEquals("forward", describeDirection(Vec2(0.0, -8.0)))
        assertEquals("backward and to side 2", describeDirection(Vec2(6.0, 6.0)))
        assertEquals("to side 1", describeDirection(Vec2(-8.0, 1.0)))
        assertEquals("mark time", describeDirection(Vec2.ZERO))
    }

    @Test
    fun formatsStepsWithoutNoise() {
        assertEquals("2", formatSteps(2.0))
        assertEquals("2.5", formatSteps(2.5))
        assertEquals("2.25", formatSteps(2.25))
        assertEquals("0.75", formatSteps(0.75))
    }

    @Test
    fun dotBookHasOneEntryPerSet() {
        val show = demoShow()
        val book = dotBook(show, performerIndex = 5)
        assertEquals(show.sets.size, book.size)
        assertNull(book.first().move)
        assertTrue(book.drop(1).all { it.move != null })
        assertEquals(show.sets[2].positions[5], book[2].position)
    }
}
