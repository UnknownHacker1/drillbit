package dev.teima.drillbit.core.plan

import dev.teima.drillbit.core.geometry.Vec2
import dev.teima.drillbit.core.geometry.lerp

/**
 * The closest two performers get while both walk straight lines at constant speed over the
 * same counts (the way a band moves between sets).
 *
 * @property time when it happens, from 0 (start of the move) to 1 (end of the move).
 */
data class ClosestApproach(
    val distance: Double,
    val time: Double,
)

/**
 * Exact closest approach of performer A walking `a0 -> a1` and B walking `b0 -> b1`.
 *
 * Their separation is `d(t) = d0 + t * (d1 - d0)`, so `|d(t)|^2` is a quadratic in `t`
 * that we minimise in closed form and clamp to `[0, 1]`.
 */
fun closestApproach(
    a0: Vec2,
    a1: Vec2,
    b0: Vec2,
    b1: Vec2,
): ClosestApproach {
    val start = a0 - b0
    val drift = (a1 - b1) - start
    val driftSq = drift.lengthSquared
    val t = if (driftSq < EPSILON) 0.0 else (-(start dot drift) / driftSq).coerceIn(0.0, 1.0)
    return ClosestApproach(distance = (start + drift * t).length, time = t)
}

/** Two performers that get closer than the allowed clearance during a move. */
data class Collision(
    val first: Int,
    val second: Int,
    val distance: Double,
    val time: Double,
) {
    /** Where on the field the near miss happens (midpoint of the two performers). */
    fun location(
        from: List<Vec2>,
        to: List<Vec2>,
    ): Vec2 {
        val a = lerp(from[first], to[first], time)
        val b = lerp(from[second], to[second], time)
        return lerp(a, b, HALF)
    }
}

/**
 * Every pair of performers that come within [clearance] steps of each other while moving
 * from [from] to [to]. Pairs are checked exactly, O(n^2) per move.
 */
fun findCollisions(
    from: List<Vec2>,
    to: List<Vec2>,
    clearance: Double = DEFAULT_CLEARANCE,
): List<Collision> {
    require(from.size == to.size) { "Both sets must place the same performers" }
    val collisions = mutableListOf<Collision>()
    for (i in from.indices) {
        for (j in i + 1 until from.size) {
            val approach = closestApproach(from[i], to[i], from[j], to[j])
            if (approach.distance < clearance) {
                collisions += Collision(i, j, approach.distance, approach.time)
            }
        }
    }
    return collisions
}

/** Two performers closer than one step (22.5 inches) centre to centre are bumping shoulders. */
const val DEFAULT_CLEARANCE: Double = 1.0

private const val EPSILON = 1e-12
private const val HALF = 0.5
