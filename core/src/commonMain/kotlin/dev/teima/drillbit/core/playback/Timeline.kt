package dev.teima.drillbit.core.playback

import dev.teima.drillbit.core.geometry.Vec2
import dev.teima.drillbit.core.geometry.lerp
import dev.teima.drillbit.core.model.Show

/** Where everyone is at one moment of the show. */
data class Frame(
    /** The set being marched into, or 0 while standing in the opening set. */
    val setIndex: Int,
    /** Beats completed inside the current move, from 0 up to that set's counts. */
    val countInSet: Double,
    val positions: List<Vec2>,
)

/** Maps show time (in counts) to performer positions. Everyone moves in straight lines at a steady stride. */
class Timeline(
    val show: Show,
) {
    /** Count at which the move into each set starts. `startCounts[0]` is 0 and has no move. */
    private val startCounts: IntArray =
        IntArray(show.sets.size).also { starts ->
            for (i in 1 until show.sets.size) {
                starts[i] = if (i == 1) 0 else starts[i - 1] + show.sets[i - 1].counts
            }
        }

    val totalCounts: Int get() = show.totalCounts

    /** Seconds of show time per count at the show's tempo. */
    val secondsPerCount: Double get() = SECONDS_PER_MINUTE / show.tempoBpm

    /** The count at which the band arrives in set [index]. */
    fun arrivalCount(index: Int): Int = if (index == 0) 0 else startCounts[index] + show.sets[index].counts

    /** Positions at show time [count] (clamped to the show). */
    fun frameAt(count: Double): Frame {
        val sets = show.sets
        if (sets.size == 1 || count <= 0.0) return Frame(0, 0.0, sets[0].positions)
        if (count >= totalCounts) return Frame(sets.size - 1, sets.last().counts.toDouble(), sets.last().positions)
        val index = (1 until sets.size).last { startCounts[it] <= count }
        val inSet = count - startCounts[index]
        val t = inSet / sets[index].counts
        val from = sets[index - 1].positions
        val to = sets[index].positions
        return Frame(index, inSet, List(from.size) { lerp(from[it], to[it], t) })
    }

    private companion object {
        const val SECONDS_PER_MINUTE = 60.0
    }
}
