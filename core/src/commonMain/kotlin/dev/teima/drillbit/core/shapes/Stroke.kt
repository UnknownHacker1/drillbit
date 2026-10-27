package dev.teima.drillbit.core.shapes

import dev.teima.drillbit.core.geometry.Vec2
import dev.teima.drillbit.core.geometry.lerp
import kotlin.math.floor

/** A polyline that performers stand along. A closed stroke joins its last point back to its first. */
data class Stroke(
    val points: List<Vec2>,
    val closed: Boolean = false,
) {
    init {
        require(points.size >= 2) { "A stroke needs at least two points" }
    }

    private val segments: List<Pair<Vec2, Vec2>> =
        points.zipWithNext() + if (closed) listOf(points.last() to points.first()) else emptyList()

    val length: Double = segments.sumOf { (a, b) -> a.distanceTo(b) }

    /** The point [distance] steps along the stroke from its first point. */
    fun pointAt(distance: Double): Vec2 {
        var remaining = distance.coerceIn(0.0, length)
        for ((a, b) in segments) {
            val segmentLength = a.distanceTo(b)
            if (remaining <= segmentLength && segmentLength > 0.0) {
                return lerp(a, b, remaining / segmentLength)
            }
            remaining -= segmentLength
        }
        return if (closed) points.first() else points.last()
    }

    /** [count] points spread evenly by arc length. Open strokes keep both end points. */
    fun sample(count: Int): List<Vec2> =
        when {
            count <= 0 -> emptyList()
            closed -> List(count) { pointAt(length * it / count) }
            count == 1 -> listOf(pointAt(length / 2))
            else -> List(count) { pointAt(length * it / (count - 1)) }
        }

    /** The point on this stroke nearest to [target]. */
    fun nearestPoint(target: Vec2): Vec2 =
        segments
            .map { (a, b) -> projectOntoSegment(target, a, b) }
            .minBy { target.distanceSquaredTo(it) }

    fun map(transform: (Vec2) -> Vec2): Stroke = Stroke(points.map(transform), closed)
}

private fun projectOntoSegment(
    p: Vec2,
    a: Vec2,
    b: Vec2,
): Vec2 {
    val ab = b - a
    val lengthSq = ab.lengthSquared
    if (lengthSq == 0.0) return a
    val t = (((p - a) dot ab) / lengthSq).coerceIn(0.0, 1.0)
    return a + ab * t
}

/**
 * Places exactly [count] spots along [strokes], spacing them as evenly as possible.
 *
 * An open stroke holding `k` spots has `k - 1` gaps and a closed one has `k`, so we solve for
 * one common spacing and round each stroke's share with the largest-remainder method. Spots
 * where strokes meet are then pushed apart by [spreadOut].
 */
fun sampleStrokes(
    strokes: List<Stroke>,
    count: Int,
    minSpacing: Double = MIN_INTERVAL,
): List<Vec2> {
    if (count <= 0 || strokes.isEmpty()) return emptyList()
    val openCount = strokes.count { !it.closed }
    val totalLength = strokes.sumOf { it.length }
    val gaps = (count - openCount).coerceAtLeast(1)
    val spacing = totalLength / gaps
    val ideal = strokes.map { if (spacing == 0.0) 0.0 else it.length / spacing + if (it.closed) 0.0 else 1.0 }
    val shares = largestRemainder(ideal, count)
    val raw = strokes.zip(shares).flatMap { (stroke, share) -> stroke.sample(share) }
    return spreadOut(raw, strokes, minOf(minSpacing, spacing * SPREAD_FRACTION))
}

/** Rounds [weights] to integers that add up to exactly [total], keeping them proportional. */
internal fun largestRemainder(
    weights: List<Double>,
    total: Int,
): List<Int> {
    val sum = weights.sum()
    if (sum <= 0.0) return List(weights.size) { if (it == 0) total else 0 }
    val scaled = weights.map { it / sum * total }
    val result = scaled.map { floor(it).toInt() }.toMutableList()
    var missing = total - result.sum()
    val byRemainder = scaled.indices.sortedByDescending { scaled[it] - floor(scaled[it]) }
    var i = 0
    while (missing > 0) {
        result[byRemainder[i % byRemainder.size]]++
        missing--
        i++
    }
    return result
}

/**
 * Nudges spots that are closer than [minSpacing] apart, keeping every spot on a stroke.
 * A few rounds of pairwise repulsion followed by projection back onto the nearest stroke.
 */
internal fun spreadOut(
    spots: List<Vec2>,
    strokes: List<Stroke>,
    minSpacing: Double,
    rounds: Int = SPREAD_ROUNDS,
): List<Vec2> {
    val current = spots.toMutableList()
    repeat(rounds) {
        var moved = false
        val push = MutableList(current.size) { Vec2.ZERO }
        for (i in current.indices) {
            for (j in i + 1 until current.size) {
                val delta = current[j] - current[i]
                val distance = delta.length
                if (distance < minSpacing) {
                    val direction =
                        if (distance > TINY) delta * (1.0 / distance) else Vec2(1.0, 0.0).rotated(i + j.toDouble())
                    val shove = direction * ((minSpacing - distance) / 2)
                    push[i] = push[i] - shove
                    push[j] = push[j] + shove
                    moved = true
                }
            }
        }
        if (!moved) return current
        for (i in current.indices) {
            val target = current[i] + push[i]
            current[i] = strokes.map { it.nearestPoint(target) }.minBy { it.distanceSquaredTo(target) }
        }
    }
    return current
}

/** The usual minimum interval between marchers, in steps. */
const val MIN_INTERVAL: Double = 2.0

private const val SPREAD_FRACTION = 0.85
private const val SPREAD_ROUNDS = 40
private const val TINY = 1e-9
