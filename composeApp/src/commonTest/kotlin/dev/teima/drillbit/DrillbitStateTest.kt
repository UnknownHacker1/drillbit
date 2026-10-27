package dev.teima.drillbit

import dev.teima.drillbit.core.codec.ShowCodec
import dev.teima.drillbit.core.codec.shareLink
import dev.teima.drillbit.core.demo.demoShow
import dev.teima.drillbit.core.geometry.Vec2
import dev.teima.drillbit.core.plan.AssignmentStrategy
import dev.teima.drillbit.platform.ShowStorage
import dev.teima.drillbit.ui.state.DrillbitState
import dev.teima.drillbit.ui.state.FormationKind
import dev.teima.drillbit.ui.state.FormationSpec
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DrillbitStateTest {
    private class MemoryStorage : ShowStorage {
        var saved: String? = null

        override fun load(): String? = saved

        override fun save(code: String) {
            saved = code
        }
    }

    @Test
    fun everyFormationKindPlacesTheWholeBand() {
        FormationKind.entries.forEach { kind ->
            assertEquals(
                64,
                FormationSpec()
                    .withKind(kind)
                    .toFormation()
                    .spots(64)
                    .size,
                kind.label,
            )
        }
    }

    @Test
    fun addingASetFromADraftInsertsAndSelectsIt() {
        val storage = MemoryStorage()
        val state = DrillbitState(demoShow(), storage)
        val before = state.show.sets.size
        state.selectSet(0)
        state.openDraft(replace = false)
        state.updateDraft(state.draft!!.copy(spec = FormationSpec(FormationKind.STAR)))
        val preview = assertNotNull(state.preview)
        assertTrue(preview.reports.getValue(AssignmentStrategy.OPTIMAL).isCollisionFree)
        state.applyDraft()
        assertEquals(before + 1, state.show.sets.size)
        assertEquals(1, state.selectedSet)
        assertEquals("Star", state.show.sets[1].name)
        assertNull(state.draft)
        assertNotNull(storage.saved, "every edit is saved")
    }

    @Test
    fun draggingADotIsOneUndoableEdit() {
        val state = DrillbitState(demoShow())
        state.selectSet(1)
        state.startDrag(performer = 3, at = Vec2(10.0, 10.0))
        state.dragTo(Vec2(12.0, 12.0))
        state.dragTo(Vec2(14.0, 14.0))
        assertEquals(Vec2(14.0, 14.0), state.currentPositions()[3])
        state.endDrag()
        assertEquals(Vec2(14.0, 14.0), state.show.sets[1].positions[3])
        state.undo()
        assertEquals(demoShow().sets[1].positions[3], state.show.sets[1].positions[3])
        assertFalse(state.history.canUndo)
    }

    @Test
    fun playbackAdvancesThroughTheShowAndStops() {
        val state = DrillbitState(demoShow())
        var ticks = 0
        state.soundOn = true
        state.onTick = { ticks++ }
        state.togglePlay()
        repeat(1_000) { state.advance(0.1) }
        assertFalse(state.playing)
        assertEquals(state.show.sets.lastIndex, state.selectedSet)
        assertEquals(state.timeline.totalCounts, ticks)
    }

    @Test
    fun importsSharedLinksAndRejectsGarbage() {
        val state = DrillbitState(demoShow())
        val other = demoShow().copy(title = "Away game")
        assertTrue(state.importFrom(shareLink("https://example.com/", ShowCodec.encode(other))))
        assertEquals("Away game", state.show.title)
        assertFalse(state.importFrom("definitely not a show"))
        assertEquals("Away game", state.show.title)
    }
}
