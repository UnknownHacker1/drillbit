package dev.teima.drillbit.ui.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.teima.drillbit.core.codec.InvalidShowCodeException
import dev.teima.drillbit.core.codec.ShowCodec
import dev.teima.drillbit.core.codec.extractShowCode
import dev.teima.drillbit.core.edit.History
import dev.teima.drillbit.core.edit.deleteSet
import dev.teima.drillbit.core.edit.insertSet
import dev.teima.drillbit.core.edit.moveDot
import dev.teima.drillbit.core.edit.optimizeAll
import dev.teima.drillbit.core.edit.planMorph
import dev.teima.drillbit.core.edit.replacePositions
import dev.teima.drillbit.core.edit.setCounts
import dev.teima.drillbit.core.edit.suggestedCounts
import dev.teima.drillbit.core.geometry.Vec2
import dev.teima.drillbit.core.model.Show
import dev.teima.drillbit.core.plan.AssignmentStrategy
import dev.teima.drillbit.core.plan.TransitionReport
import dev.teima.drillbit.core.plan.analyzeTransition
import dev.teima.drillbit.core.playback.Timeline
import dev.teima.drillbit.platform.NoStorage
import dev.teima.drillbit.platform.ShowStorage
import kotlin.math.floor

enum class Mode {
    /** Drill writer: build formations and transitions. */
    DESIGN,

    /** Performer: pick yourself and read your dot book. */
    PERFORM,
}

/** A formation being previewed before it is added to the show. */
data class MorphDraft(
    val spec: FormationSpec,
    /** Replace the selected set instead of adding a new one after it. */
    val replace: Boolean,
    val strategy: AssignmentStrategy = AssignmentStrategy.OPTIMAL,
)

/** The three candidate plans for a [MorphDraft], each with its own report. */
class MorphPreview(
    val from: List<Vec2>,
    val plans: Map<AssignmentStrategy, List<Vec2>>,
    val reports: Map<AssignmentStrategy, TransitionReport>,
)

/**
 * All UI state, kept in Compose snapshot state so screens recompose when it changes.
 * The show itself is immutable; every edit pushes a new value onto [history].
 */
class DrillbitState(
    initial: Show,
    private val storage: ShowStorage = NoStorage,
    initialMode: Mode = Mode.DESIGN,
) {
    var history by mutableStateOf(History(initial))
        private set

    val show: Show get() = history.present

    var mode by mutableStateOf(initialMode)

    var selectedSet by mutableIntStateOf(0)
        private set

    var selectedPerformer by mutableStateOf<Int?>(null)

    /** Show time in counts while playing or scrubbing. */
    var playhead by mutableDoubleStateOf(0.0)
        private set

    var playing by mutableStateOf(false)
        private set

    var soundOn by mutableStateOf(false)

    var draft by mutableStateOf<MorphDraft?>(null)
        private set

    var preview by mutableStateOf<MorphPreview?>(null)
        private set

    /** A dot being dragged on the field: performer index and where it is right now. */
    var drag by mutableStateOf<Pair<Int, Vec2>?>(null)
        private set

    /** One-line feedback shown at the bottom of the screen. */
    var message by mutableStateOf<String?>(null)

    private var reportCache: Pair<Show, List<TransitionReport?>>? = null

    val timeline: Timeline get() = Timeline(show)

    /** Report for the move into set [index], or null for the opening set. */
    fun reportFor(index: Int): TransitionReport? {
        val cached =
            reportCache?.takeIf { it.first === show }?.second ?: computeReports().also { reportCache = show to it }
        return cached.getOrNull(index)
    }

    private fun computeReports(): List<TransitionReport?> =
        listOf(null) +
            show.sets.zipWithNext { previous, next -> analyzeTransition(previous.positions, next.positions, next.counts) }

    // Editing

    fun commit(updated: Show) {
        history = history.push(updated)
        afterShowChanged()
    }

    fun undo() {
        history = history.undo()
        afterShowChanged()
    }

    fun redo() {
        history = history.redo()
        afterShowChanged()
    }

    private fun afterShowChanged() {
        selectedSet = selectedSet.coerceIn(0, show.sets.lastIndex)
        if (!playing) playhead = timeline.arrivalCount(selectedSet).toDouble()
        storage.save(ShowCodec.encode(show))
        draft?.let { refreshPreview(it) }
    }

    fun selectSet(index: Int) {
        playing = false
        selectedSet = index.coerceIn(0, show.sets.lastIndex)
        playhead = timeline.arrivalCount(selectedSet).toDouble()
        draft?.let { if (it.replace && selectedSet == 0) closeDraft() else refreshPreview(it) }
    }

    fun deleteSelectedSet() {
        if (show.sets.size <= 1) return
        commit(show.deleteSet(selectedSet))
    }

    fun changeCounts(delta: Int) {
        if (selectedSet == 0) return
        commit(show.setCounts(selectedSet, show.sets[selectedSet].counts + delta))
    }

    fun fixCounts() {
        if (selectedSet == 0) return
        val counts = suggestedCounts(show.sets[selectedSet - 1].positions, show.sets[selectedSet].positions)
        commit(show.setCounts(selectedSet, counts))
    }

    fun optimizeEverything() {
        val optimized = show.optimizeAll()
        if (optimized == show) {
            message = "Every move is already optimal."
        } else {
            commit(optimized)
            message = "Re-assigned performers in every move. The pictures did not change."
        }
    }

    fun startDrag(
        performer: Int,
        at: Vec2,
    ) {
        playing = false
        selectedPerformer = performer
        drag = performer to show.field.clamp(at)
    }

    fun dragTo(at: Vec2) {
        drag = drag?.let { it.first to show.field.clamp(at) }
    }

    fun endDrag() {
        val (performer, at) = drag ?: return
        drag = null
        commit(show.moveDot(selectedSet, performer, at))
    }

    // Formation drafts

    fun openDraft(replace: Boolean) {
        val spec = draft?.spec ?: FormationSpec()
        updateDraft(MorphDraft(spec, replace = replace && selectedSet > 0))
    }

    fun updateDraft(newDraft: MorphDraft) {
        draft = newDraft
        refreshPreview(newDraft)
    }

    fun closeDraft() {
        draft = null
        preview = null
    }

    fun applyDraft() {
        val current = draft ?: return
        val plan = preview?.plans?.get(current.strategy) ?: return
        val name = current.spec.setName
        if (current.replace) {
            commit(show.replacePositions(selectedSet, name, plan))
            selectSet(selectedSet)
        } else {
            commit(show.insertSet(selectedSet, name, plan))
            selectSet(selectedSet + 1)
        }
        closeDraft()
    }

    private fun refreshPreview(current: MorphDraft) {
        val baseIndex = if (current.replace) selectedSet - 1 else selectedSet
        if (baseIndex < 0) return
        val from = show.sets[baseIndex].positions
        val plans = planMorph(from, current.spec.toFormation(), show.field)
        val counts =
            if (current.replace) {
                show.sets[selectedSet].counts
            } else {
                suggestedCounts(from, plans.getValue(AssignmentStrategy.OPTIMAL))
            }
        val reports =
            plans.mapValues { (strategy, to) ->
                analyzeTransition(from, to, counts, optimal = strategy == AssignmentStrategy.OPTIMAL)
            }
        preview = MorphPreview(from, plans, reports)
    }

    // Playback

    fun togglePlay() {
        if (playing) {
            playing = false
            return
        }
        closeDraft()
        if (playhead >= timeline.totalCounts) playhead = 0.0
        playing = true
    }

    /** Moves the playhead by [seconds] of real time. Called every frame while playing. */
    fun advance(seconds: Double) {
        if (!playing) return
        val timeline = timeline
        val before = playhead
        val after = (before + seconds / timeline.secondsPerCount).coerceAtMost(timeline.totalCounts.toDouble())
        playhead = after
        selectedSet = timeline.frameAt(after).setIndex
        if (soundOn && floor(after) > floor(before)) {
            val count = floor(after).toInt()
            onTick(isSetStart(count))
        }
        if (after >= timeline.totalCounts) playing = false
    }

    fun scrubTo(count: Double) {
        playing = false
        playhead = count.coerceIn(0.0, timeline.totalCounts.toDouble())
        selectedSet = timeline.frameAt(playhead).setIndex
    }

    /** Called on each beat while sound is on; wired to the platform metronome by the UI. */
    var onTick: (accent: Boolean) -> Unit = {}

    private fun isSetStart(count: Int): Boolean = show.sets.indices.any { timeline.arrivalCount(it) == count }

    /** Positions to draw right now: the playhead while playing, otherwise the selected set. */
    fun currentPositions(): List<Vec2> {
        val base =
            if (playing || playhead != timeline.arrivalCount(selectedSet).toDouble()) {
                timeline.frameAt(playhead).positions
            } else {
                show.sets[selectedSet].positions
            }
        val (performer, at) = drag ?: return base
        return base.toMutableList().also { it[performer] = at }
    }

    // Sharing

    val shareCode: String get() = ShowCodec.encode(show)

    /** Loads a show from a pasted link or code. Returns false and sets [message] when it is not valid. */
    fun importFrom(text: String): Boolean {
        val code = extractShowCode(text)
        if (code == null) {
            message = "That does not look like a Drillbit link or code."
            return false
        }
        return try {
            val imported = ShowCodec.decode(code)
            history = History(imported)
            selectedPerformer = null
            closeDraft()
            selectSet(0)
            storage.save(code)
            message =
                "Loaded \"${imported.title}\": ${imported.performers.size} performers, ${imported.sets.size} sets."
            true
        } catch (e: InvalidShowCodeException) {
            message = e.message
            false
        }
    }
}
