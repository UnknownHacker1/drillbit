package dev.teima.drillbit.core

import dev.teima.drillbit.core.demo.DEMO_ROSTER
import dev.teima.drillbit.core.demo.demoShow
import dev.teima.drillbit.core.edit.History
import dev.teima.drillbit.core.edit.deleteSet
import dev.teima.drillbit.core.edit.moveDot
import dev.teima.drillbit.core.edit.newShow
import dev.teima.drillbit.core.edit.reassign
import dev.teima.drillbit.core.edit.setCounts
import dev.teima.drillbit.core.edit.suggestedCounts
import dev.teima.drillbit.core.geometry.Vec2
import dev.teima.drillbit.core.model.Field
import dev.teima.drillbit.core.plan.AssignmentStrategy
import dev.teima.drillbit.core.playback.Timeline
import dev.teima.drillbit.core.shapes.BlockFormation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EditingTest {
    @Test
    fun newShowPlacesTheWholeRoster() {
        val show = newShow("Test", DEMO_ROSTER, BlockFormation())
        assertEquals(64, show.performers.size)
        assertEquals("W1", show.performers.first().label)
        assertEquals("G10", show.performers.last().label)
    }

    @Test
    fun reassignKeepsThePictureAndSwapsThePeople() {
        val show = demoShow()
        val scrambled = show.reassign(2, AssignmentStrategy.IN_ORDER)
        assertEquals(show.sets[2].positions.toSet(), scrambled.sets[2].positions.toSet())
    }

    @Test
    fun editsAreClampedAndBounded() {
        val show = demoShow()
        val moved = show.moveDot(1, 0, Vec2(-10.0, 999.0))
        assertEquals(Vec2(0.0, Field.DEPTH), moved.sets[1].positions[0])
        assertEquals(128, show.setCounts(1, 10_000).sets[1].counts)
        assertEquals(1, show.setCounts(1, 0).sets[1].counts)
        val single = newShow("One", DEMO_ROSTER, BlockFormation())
        assertEquals(single, single.deleteSet(0))
        assertEquals(show.sets.size - 1, show.deleteSet(1).sets.size)
    }

    @Test
    fun suggestedCountsAreWholePhrases() {
        val from = listOf(Vec2(0.0, 0.0))
        assertEquals(8, suggestedCounts(from, from))
        assertEquals(12, suggestedCounts(from, listOf(Vec2(16.0, 0.0))))
        assertEquals(16, suggestedCounts(from, listOf(Vec2(17.0, 0.0))))
    }

    @Test
    fun timelineInterpolatesBetweenSets() {
        val show = demoShow()
        val timeline = Timeline(show)
        assertEquals(show.sets.drop(1).sumOf { it.counts }, timeline.totalCounts)
        assertEquals(show.sets[0].positions, timeline.frameAt(0.0).positions)
        assertEquals(show.sets.last().positions, timeline.frameAt(1e9).positions)
        val firstMove = show.sets[1].counts
        val halfway = timeline.frameAt(firstMove / 2.0)
        assertEquals(1, halfway.setIndex)
        val expected = (show.sets[0].positions[0] + show.sets[1].positions[0]) * 0.5
        assertEquals(expected.x, halfway.positions[0].x, 1e-9)
        assertEquals(firstMove, timeline.arrivalCount(1))
        assertEquals(2, timeline.frameAt(firstMove + 0.5).setIndex)
        assertEquals(0.5, 60.0 / show.tempoBpm)
    }

    @Test
    fun historyUndoesAndRedoes() {
        var history = History(1)
        history = history.push(2).push(3)
        assertEquals(3, history.present)
        history = history.undo().undo()
        assertEquals(1, history.present)
        assertFalse(history.canUndo)
        history = history.redo()
        assertEquals(2, history.present)
        history = history.push(9)
        assertFalse(history.canRedo)
        assertTrue(history.canUndo)
        assertEquals(history, history.push(9))
    }
}
