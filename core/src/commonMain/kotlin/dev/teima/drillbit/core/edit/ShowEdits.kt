package dev.teima.drillbit.core.edit

import dev.teima.drillbit.core.geometry.Vec2
import dev.teima.drillbit.core.model.DrillSet
import dev.teima.drillbit.core.model.Field
import dev.teima.drillbit.core.model.Performer
import dev.teima.drillbit.core.model.Section
import dev.teima.drillbit.core.model.Show
import dev.teima.drillbit.core.plan.AssignmentStrategy
import dev.teima.drillbit.core.plan.assignSpots
import dev.teima.drillbit.core.plan.minimumCounts
import dev.teima.drillbit.core.shapes.Formation

// Pure, immutable edits on a Show. The UI keeps a History of the results for undo.

/** The three ways of marching from [from] into [formation], so the UI can compare them side by side. */
fun planMorph(
    from: List<Vec2>,
    formation: Formation,
    field: Field = Field.HIGH_SCHOOL,
): Map<AssignmentStrategy, List<Vec2>> {
    val spots = formation.spots(from.size).map(field::clamp)
    return AssignmentStrategy.entries.associateWith { assignSpots(from, spots, it) }
}

/** Inserts a new set after [afterIndex] with the given [positions]. */
fun Show.insertSet(
    afterIndex: Int,
    name: String,
    positions: List<Vec2>,
    counts: Int = suggestedCounts(sets[afterIndex].positions, positions),
): Show {
    val updated = sets.toMutableList()
    updated.add(afterIndex + 1, DrillSet(name, counts, positions))
    return copy(sets = updated)
}

/** Replaces the positions of set [index], keeping its counts. */
fun Show.replacePositions(
    index: Int,
    name: String,
    positions: List<Vec2>,
): Show = updateSet(index) { it.copy(name = name, positions = positions) }

fun Show.deleteSet(index: Int): Show {
    if (sets.size <= 1) return this
    return copy(sets = sets.filterIndexed { i, _ -> i != index })
}

fun Show.setCounts(
    index: Int,
    counts: Int,
): Show = updateSet(index) { it.copy(counts = counts.coerceIn(1, MAX_COUNTS)) }

fun Show.moveDot(
    setIndex: Int,
    performer: Int,
    to: Vec2,
): Show =
    updateSet(setIndex) { set ->
        set.copy(positions = set.positions.toMutableList().also { it[performer] = field.clamp(to) })
    }

/**
 * Re-decides who goes where for set [index] (the spots stay, the people swap) so the move
 * into it uses [strategy].
 */
fun Show.reassign(
    index: Int,
    strategy: AssignmentStrategy,
): Show {
    if (index == 0) return this
    return updateSet(index) { it.copy(positions = assignSpots(sets[index - 1].positions, it.positions, strategy)) }
}

/**
 * Runs the optimal planner on every move in order. Each set's spots stay the same; only who
 * stands on which spot changes, so the picture the audience sees is untouched.
 */
fun Show.optimizeAll(): Show {
    var result = this
    for (i in 1 until sets.size) result = result.reassign(i, AssignmentStrategy.OPTIMAL)
    return result
}

/** Counts for a move: enough for a comfortable stride, rounded up to a musical phrase of four. */
fun suggestedCounts(
    from: List<Vec2>,
    to: List<Vec2>,
): Int {
    val needed = minimumCounts(from, to)
    return ((needed + PHRASE - 1) / PHRASE * PHRASE).coerceIn(MIN_SUGGESTED, MAX_COUNTS)
}

/** An empty show: [roster] performers standing in a block on the 50. */
fun newShow(
    title: String,
    roster: Map<Section, Int>,
    opening: Formation,
    tempoBpm: Int = DEFAULT_TEMPO,
    field: Field = Field.HIGH_SCHOOL,
): Show {
    val performers = roster.flatMap { (section, count) -> (1..count).map { Performer(section, it) } }
    val spots = opening.spots(performers.size).map(field::clamp)
    return Show(title, tempoBpm, field, performers, listOf(DrillSet(opening.title, PHRASE * 2, spots)))
}

private fun Show.updateSet(
    index: Int,
    change: (DrillSet) -> DrillSet,
): Show = copy(sets = sets.mapIndexed { i, set -> if (i == index) change(set) else set })

const val DEFAULT_TEMPO: Int = 120
const val MAX_COUNTS: Int = 128
private const val PHRASE = 4
private const val MIN_SUGGESTED = 8
