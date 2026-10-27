package dev.teima.drillbit.core.model

import dev.teima.drillbit.core.geometry.Vec2
import kotlin.math.floor
import kotlin.math.round

/**
 * An American football field measured in 8-to-5 steps (8 steps = 5 yards).
 *
 * x runs from the left goal line (0) to the right goal line ([LENGTH]); y runs from the
 * front sideline (0, where the audience sits) to the back sideline ([DEPTH]).
 */
enum class Field(
    val displayName: String,
    /** Distance of each hash mark from its nearest sideline, in steps. */
    val hashInset: Double,
) {
    HIGH_SCHOOL("High school", HS_HASH_INSET),
    COLLEGE("College", COLLEGE_HASH_INSET),
    ;

    val frontHash: Double get() = hashInset
    val backHash: Double get() = DEPTH - hashInset

    /** Keeps [point] inside the playing field. */
    fun clamp(point: Vec2): Vec2 = Vec2(point.x.coerceIn(0.0, LENGTH), point.y.coerceIn(0.0, DEPTH))

    /**
     * Keeps [point] on the field and on the quarter-step grid. Every spot the app creates goes
     * through here, so a show survives the share code (which stores quarter steps) unchanged,
     * including its assignment and its collision-free proof.
     */
    fun snap(point: Vec2): Vec2 {
        val inside = clamp(point)
        // floor(DEPTH * 4) / 4 keeps a rounded point inside the field.
        return Vec2(toGrid(inside.x).coerceAtMost(LENGTH), toGrid(inside.y).coerceAtMost(floor(DEPTH * GRID) / GRID))
    }

    companion object {
        const val STEPS_PER_FIVE_YARDS: Int = 8

        /** Goal line to goal line: 100 yards. */
        const val LENGTH: Double = 160.0

        /** Sideline to sideline: 53 1/3 yards. */
        const val DEPTH: Double = 256.0 / 3.0

        /** The 50 yard line. */
        const val CENTER_X: Double = LENGTH / 2

        val center: Vec2 = Vec2(CENTER_X, DEPTH / 2)

        /** Grid divisions per step: positions are kept to the nearest quarter step. */
        const val GRID: Double = 4.0

        private fun toGrid(value: Double): Double = round(value * GRID) / GRID
    }
}

private const val HS_HASH_INSET = 28.0 + 4.0 / 9.0
private const val COLLEGE_HASH_INSET = 32.0
