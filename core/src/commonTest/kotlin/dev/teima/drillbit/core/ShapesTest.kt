package dev.teima.drillbit.core

import dev.teima.drillbit.core.geometry.Vec2
import dev.teima.drillbit.core.model.Field
import dev.teima.drillbit.core.plan.tightestSpacing
import dev.teima.drillbit.core.shapes.ArcFormation
import dev.teima.drillbit.core.shapes.BlockFormation
import dev.teima.drillbit.core.shapes.CircleFormation
import dev.teima.drillbit.core.shapes.HeartFormation
import dev.teima.drillbit.core.shapes.KotlinLogoFormation
import dev.teima.drillbit.core.shapes.LineFormation
import dev.teima.drillbit.core.shapes.StarFormation
import dev.teima.drillbit.core.shapes.Stroke
import dev.teima.drillbit.core.shapes.StrokeFont
import dev.teima.drillbit.core.shapes.TextFormation
import dev.teima.drillbit.core.shapes.WedgeFormation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ShapesTest {
    private val formations =
        listOf(
            TextFormation("KOTLIN"),
            TextFormation("KMP 2027"),
            LineFormation(Vec2(20.0, 20.0), Vec2(140.0, 20.0)),
            ArcFormation(),
            CircleFormation(),
            WedgeFormation(),
            StarFormation(),
            HeartFormation(),
            KotlinLogoFormation(),
            BlockFormation(),
        )

    @Test
    fun everyFormationReturnsExactlyOneSpotPerPerformer() {
        for (formation in formations) {
            for (count in listOf(1, 2, 7, 24, 64, 150)) {
                assertEquals(count, formation.spots(count).size, "${formation.title} with $count")
            }
        }
    }

    @Test
    fun sixtyFourPerformersNeverShareASpot() {
        for (formation in formations) {
            val spots = formation.spots(64)
            assertTrue(tightestSpacing(spots) >= 1.5, "${formation.title} packs spots ${tightestSpacing(spots)} apart")
        }
    }

    @Test
    fun formationsStayOnTheField() {
        for (formation in formations) {
            formation.spots(64).forEach { assertEquals(it, Field.HIGH_SCHOOL.clamp(it), formation.title) }
        }
    }

    @Test
    fun openStrokeKeepsEndPointsAndSpacesEvenly() {
        val stroke = Stroke(listOf(Vec2(0.0, 0.0), Vec2(12.0, 0.0)))
        assertEquals(listOf(0.0, 4.0, 8.0, 12.0), stroke.sample(4).map { it.x })
    }

    @Test
    fun closedStrokeDoesNotDoubleItsStart() {
        val square = Stroke(listOf(Vec2(0.0, 0.0), Vec2(4.0, 0.0), Vec2(4.0, 4.0), Vec2(0.0, 4.0)), closed = true)
        assertEquals(16.0, square.length)
        assertEquals(listOf(Vec2(0.0, 0.0), Vec2(4.0, 0.0), Vec2(4.0, 4.0), Vec2(0.0, 4.0)), square.sample(4))
    }

    @Test
    fun fontCoversLettersAndDigits() {
        (('A'..'Z') + ('0'..'9')).forEach { assertTrue(StrokeFont.glyph(it).isNotEmpty(), "missing glyph $it") }
        assertTrue(StrokeFont.glyph('k').isNotEmpty(), "lower case maps to upper case")
        assertTrue(StrokeFont.glyph('~').isEmpty())
    }

    @Test
    fun textIsCentredOnTheRequestedPoint() {
        val spots = TextFormation("HOH", center = Vec2(60.0, 40.0)).spots(40)
        val minX = spots.minOf { it.x }
        val maxX = spots.maxOf { it.x }
        assertEquals(60.0, (minX + maxX) / 2, 0.5)
    }
}
