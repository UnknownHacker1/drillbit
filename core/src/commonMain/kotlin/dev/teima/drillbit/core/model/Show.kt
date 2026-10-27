package dev.teima.drillbit.core.model

import dev.teima.drillbit.core.geometry.Vec2

/** One marcher. [number] is unique inside a [section], so the label is unique inside a show. */
data class Performer(
    val section: Section,
    val number: Int,
) {
    val label: String get() = "${section.code}$number"
}

/**
 * A formation ("set") in the drill. [positions] is index-aligned with [Show.performers].
 * [counts] is how many beats the move *into* this set takes; the first set ignores it.
 */
data class DrillSet(
    val name: String,
    val counts: Int,
    val positions: List<Vec2>,
) {
    init {
        require(counts > 0) { "A set needs at least one count, got $counts" }
    }
}

/** A complete drill: who is marching, and every formation they hit in order. */
data class Show(
    val title: String,
    val tempoBpm: Int,
    val field: Field,
    val performers: List<Performer>,
    val sets: List<DrillSet>,
) {
    init {
        require(tempoBpm in TEMPO_RANGE) { "Tempo must be in $TEMPO_RANGE, got $tempoBpm" }
        require(sets.isNotEmpty()) { "A show needs at least one set" }
        require(sets.all { it.positions.size == performers.size }) {
            "Every set must place all ${performers.size} performers"
        }
        require(performers.map { it.label }.toSet().size == performers.size) { "Performer labels must be unique" }
    }

    /** Total length of the show in counts, not counting the first set (which is a starting position). */
    val totalCounts: Int get() = sets.drop(1).sumOf { it.counts }

    companion object {
        val TEMPO_RANGE: IntRange = 40..240
    }
}
