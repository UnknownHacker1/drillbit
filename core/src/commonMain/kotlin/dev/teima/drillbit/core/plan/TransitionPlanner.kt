package dev.teima.drillbit.core.plan

import dev.teima.drillbit.core.assign.solveAssignment
import dev.teima.drillbit.core.geometry.Vec2
import kotlin.math.ceil
import kotlin.math.sqrt

/** How performers are matched to the spots of a new formation. */
enum class AssignmentStrategy(
    val displayName: String,
) {
    /** Performer `i` takes spot `i`, in whatever order the shape drew them. What you get without thinking. */
    IN_ORDER("In order"),

    /** Each performer, in roster order, grabs the nearest free spot. Looks smart, still collides. */
    GREEDY("Nearest free spot"),

    /**
     * Minimise the total distance walked (Hungarian algorithm on plain distances). Sounds
     * optimal, but crossing paths can be just as short as parallel ones, so it still collides.
     */
    MIN_DISTANCE("Least total walking"),

    /**
     * Minimise the sum of *squared* distances (Hungarian algorithm). With synchronised
     * straight-line moves this is the CAPT planner, which is provably collision-free when
     * start and end spots are far enough apart (see [captSpacing]).
     */
    OPTIMAL("Least squared walking (CAPT)"),
}

/**
 * Decides who goes where. Returns the new positions index-aligned with [from]: entry `i`
 * is the spot performer `i` walks to.
 */
fun assignSpots(
    from: List<Vec2>,
    spots: List<Vec2>,
    strategy: AssignmentStrategy = AssignmentStrategy.OPTIMAL,
): List<Vec2> {
    require(from.size == spots.size) { "Need exactly one spot per performer (${from.size} vs ${spots.size})" }
    return when (strategy) {
        AssignmentStrategy.IN_ORDER -> {
            spots
        }

        AssignmentStrategy.GREEDY -> {
            greedy(from, spots)
        }

        AssignmentStrategy.MIN_DISTANCE -> {
            hungarian(from, spots) { a, b -> a.distanceTo(b) }
        }

        AssignmentStrategy.OPTIMAL -> {
            hungarian(from, spots) { a, b -> a.distanceSquaredTo(b) }
        }
    }
}

private fun hungarian(
    from: List<Vec2>,
    spots: List<Vec2>,
    cost: (Vec2, Vec2) -> Double,
): List<Vec2> {
    val matrix = Array(from.size) { i -> DoubleArray(spots.size) { j -> cost(from[i], spots[j]) } }
    val choice = solveAssignment(matrix)
    return List(from.size) { spots[choice[it]] }
}

private fun greedy(
    from: List<Vec2>,
    spots: List<Vec2>,
): List<Vec2> {
    val taken = BooleanArray(spots.size)
    return from.map { start ->
        val best = spots.indices.filter { !taken[it] }.minBy { start.distanceSquaredTo(spots[it]) }
        taken[best] = true
        spots[best]
    }
}

/** Everything a drill writer wants to know about one move between sets. */
data class TransitionReport(
    val counts: Int,
    /** Sum of every performer's distance, in steps. */
    val totalTravel: Double,
    /** The performer who has the farthest to go, and how far. */
    val longestMover: Int,
    val longestDistance: Double,
    val collisions: List<Collision>,
    /** Smallest distance between two spots in the start or end formation. */
    val tightestSpacing: Double,
    /**
     * True when the CAPT theorem applies: spots at both ends are at least `2 * sqrt(2) * R`
     * apart (R = half the clearance) *and* the assignment is the squared-distance optimum.
     * Then the move is collision-free by proof, not just by test.
     */
    val provablySafe: Boolean,
) {
    /** The biggest stride anyone needs, as steps per count (1.0 = an 8-to-5 step). */
    val largestStride: Double get() = if (counts == 0) 0.0 else longestDistance / counts

    val isCollisionFree: Boolean get() = collisions.isEmpty()
}

/**
 * Analyses the move [from] -> [to] over [counts] beats.
 *
 * @param optimal whether [to] came from [AssignmentStrategy.OPTIMAL]; only then can the CAPT
 *   guarantee be claimed.
 */
fun analyzeTransition(
    from: List<Vec2>,
    to: List<Vec2>,
    counts: Int,
    optimal: Boolean = isSquaredDistanceOptimal(from, to),
    clearance: Double = DEFAULT_CLEARANCE,
): TransitionReport {
    require(from.size == to.size) { "Both sets must place the same performers" }
    val distances = from.indices.map { from[it].distanceTo(to[it]) }
    val longest = distances.indices.maxByOrNull { distances[it] } ?: 0
    val spacing = minOf(tightestSpacing(from), tightestSpacing(to))
    return TransitionReport(
        counts = counts,
        totalTravel = distances.sum(),
        longestMover = longest,
        longestDistance = distances.getOrElse(longest) { 0.0 },
        collisions = findCollisions(from, to, clearance),
        tightestSpacing = spacing,
        provablySafe = optimal && spacing >= captSpacing(clearance),
    )
}

/** Minimum spot spacing for the CAPT guarantee: 2 * sqrt(2) * R, where R = clearance / 2. */
fun captSpacing(clearance: Double = DEFAULT_CLEARANCE): Double = sqrt(2.0) * clearance

/** True if no other matching of the same spots has a smaller sum of squared distances. */
fun isSquaredDistanceOptimal(
    from: List<Vec2>,
    to: List<Vec2>,
): Boolean {
    if (from.size != to.size) return false
    val best = assignSpots(from, to, AssignmentStrategy.OPTIMAL)
    val bestCost = from.indices.sumOf { from[it].distanceSquaredTo(best[it]) }
    val cost = from.indices.sumOf { from[it].distanceSquaredTo(to[it]) }
    return cost <= bestCost + OPTIMALITY_TOLERANCE * (1.0 + bestCost)
}

/** Smallest distance between any two of [spots], or infinity for fewer than two. */
fun tightestSpacing(spots: List<Vec2>): Double {
    var best = Double.POSITIVE_INFINITY
    for (i in spots.indices) {
        for (j in i + 1 until spots.size) {
            best = minOf(best, spots[i].distanceTo(spots[j]))
        }
    }
    return best
}

/**
 * The fewest counts that keep everyone at or under [maxStride] steps per count.
 * Drill writers use 6-to-5 (1.33 steps per count) as the largest comfortable stride.
 */
fun minimumCounts(
    from: List<Vec2>,
    to: List<Vec2>,
    maxStride: Double = MAX_COMFORTABLE_STRIDE,
): Int {
    val longest = from.indices.maxOfOrNull { from[it].distanceTo(to[it]) } ?: 0.0
    return ceil(longest / maxStride - STRIDE_TOLERANCE).toInt().coerceAtLeast(1)
}

/** A 6-to-5 step: 6 steps cover 5 yards, so each step is 8/6 of a standard step. */
const val MAX_COMFORTABLE_STRIDE: Double = 8.0 / 6.0

private const val OPTIMALITY_TOLERANCE = 1e-9
private const val STRIDE_TOLERANCE = 1e-9
