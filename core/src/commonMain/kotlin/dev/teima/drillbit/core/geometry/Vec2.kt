package dev.teima.drillbit.core.geometry

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** A point or vector on the field, measured in 8-to-5 marching steps. */
data class Vec2(
    val x: Double,
    val y: Double,
) {
    operator fun plus(other: Vec2): Vec2 = Vec2(x + other.x, y + other.y)

    operator fun minus(other: Vec2): Vec2 = Vec2(x - other.x, y - other.y)

    operator fun times(factor: Double): Vec2 = Vec2(x * factor, y * factor)

    infix fun dot(other: Vec2): Double = x * other.x + y * other.y

    val lengthSquared: Double get() = x * x + y * y

    val length: Double get() = sqrt(lengthSquared)

    /** Angle in radians, counter-clockwise from the +x axis. */
    val angle: Double get() = atan2(y, x)

    fun distanceTo(other: Vec2): Double = (this - other).length

    fun distanceSquaredTo(other: Vec2): Double = (this - other).lengthSquared

    /** Rotates this vector around the origin by [radians]. */
    fun rotated(radians: Double): Vec2 {
        val c = cos(radians)
        val s = sin(radians)
        return Vec2(x * c - y * s, x * s + y * c)
    }

    companion object {
        val ZERO: Vec2 = Vec2(0.0, 0.0)
    }
}

/** Linear interpolation between [a] and [b]; `t = 0` gives [a] and `t = 1` gives [b]. */
fun lerp(
    a: Vec2,
    b: Vec2,
    t: Double,
): Vec2 = Vec2(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t)
