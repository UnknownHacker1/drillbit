package dev.teima.drillbit.core.shapes

import dev.teima.drillbit.core.geometry.Vec2
import dev.teima.drillbit.core.model.Field
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/** Something a drill writer can ask the band to form. Produces exactly as many spots as there are performers. */
sealed interface Formation {
    /** Human-readable name shown in the UI. */
    val title: String

    /** Exactly [count] spots on the field. Order carries no meaning; the planner decides who goes where. */
    fun spots(count: Int): List<Vec2>
}

/** A formation drawn as lines; spots are spread along them by arc length. */
sealed interface StrokeFormation : Formation {
    fun strokes(): List<Stroke>

    override fun spots(count: Int): List<Vec2> = sampleStrokes(strokes(), count)
}

/** A word spelled in [StrokeFont], centred on [center], [height] steps tall. */
data class TextFormation(
    val text: String,
    val center: Vec2 = Field.center,
    val height: Double = DEFAULT_TEXT_HEIGHT,
) : StrokeFormation {
    override val title: String get() = "\"$text\""

    override fun strokes(): List<Stroke> {
        val upper = text.uppercase()
        val widest = (Field.LENGTH - FIELD_MARGIN * 2) / StrokeFont.width(upper) * StrokeFont.GLYPH_HEIGHT
        return StrokeFont.layout(upper, center, minOf(height, widest))
    }

    override fun spots(count: Int): List<Vec2> {
        val strokes = strokes()
        return if (strokes.isEmpty()) BlockFormation(center).spots(count) else sampleStrokes(strokes, count)
    }
}

/** A straight line between two points. */
data class LineFormation(
    val from: Vec2,
    val to: Vec2,
) : StrokeFormation {
    override val title: String get() = "Line"

    override fun strokes(): List<Stroke> = listOf(Stroke(listOf(from, to)))
}

/** Part of a circle. Angles are in degrees, counter-clockwise from side 2. */
data class ArcFormation(
    val center: Vec2 = Field.center,
    val radius: Double = ARC_RADIUS,
    val startDegrees: Double = ARC_START,
    val sweepDegrees: Double = ARC_SWEEP,
) : StrokeFormation {
    override val title: String get() = "Arc"

    override fun strokes(): List<Stroke> {
        val start = startDegrees * PI / DEGREES_HALF_TURN
        val sweep = sweepDegrees * PI / DEGREES_HALF_TURN
        val points = List(CURVE_RESOLUTION + 1) { i -> center + polar(radius, start + sweep * i / CURVE_RESOLUTION) }
        return listOf(Stroke(points))
    }
}

/** A full circle. */
data class CircleFormation(
    val center: Vec2 = Field.center,
    val radius: Double = DEFAULT_RADIUS,
) : StrokeFormation {
    override val title: String get() = "Circle"

    override fun strokes(): List<Stroke> {
        val points = List(CURVE_RESOLUTION) { i -> center + polar(radius, 2 * PI * i / CURVE_RESOLUTION) }
        return listOf(Stroke(points, closed = true))
    }
}

/** A V opening away from the audience, tip pointing at the front sideline. */
data class WedgeFormation(
    val center: Vec2 = Field.center,
    val width: Double = WEDGE_WIDTH,
    val depth: Double = WEDGE_DEPTH,
) : StrokeFormation {
    override val title: String get() = "Wedge"

    override fun strokes(): List<Stroke> =
        listOf(
            Stroke(
                listOf(
                    Vec2(center.x - width / 2, center.y + depth / 2),
                    Vec2(center.x, center.y - depth / 2),
                    Vec2(center.x + width / 2, center.y + depth / 2),
                ),
            ),
        )
}

/** A star outline with [points] tips. */
data class StarFormation(
    val center: Vec2 = Field.center,
    val outerRadius: Double = DEFAULT_RADIUS,
    val innerRadius: Double = DEFAULT_RADIUS * STAR_INNER_RATIO,
    val points: Int = STAR_POINTS,
) : StrokeFormation {
    override val title: String get() = "Star"

    override fun strokes(): List<Stroke> {
        val corners =
            List(points * 2) { i ->
                val radius = if (i % 2 == 0) outerRadius else innerRadius
                center + polar(radius, PI / 2 + PI * i / points)
            }
        return listOf(Stroke(corners, closed = true))
    }
}

/** The classic parametric heart curve, [size] steps across. */
data class HeartFormation(
    val center: Vec2 = Field.center,
    val size: Double = DEFAULT_SIZE,
) : StrokeFormation {
    override val title: String get() = "Heart"

    override fun strokes(): List<Stroke> {
        val scale = size / HEART_SPAN
        val points =
            List(CURVE_RESOLUTION) { i ->
                val t = 2 * PI * i / CURVE_RESOLUTION
                val x = HEART_X * sin(t).pow(3)
                val y = HEART_A * cos(t) - HEART_B * cos(2 * t) - HEART_C * cos(3 * t) - cos(4 * t)
                Vec2(center.x + x * scale, center.y + y * scale)
            }
        return listOf(Stroke(points, closed = true))
    }
}

/** The Kotlin logo: a square with a wedge cut out of its right side. */
data class KotlinLogoFormation(
    val center: Vec2 = Field.center,
    val size: Double = DEFAULT_SIZE,
) : StrokeFormation {
    override val title: String get() = "Kotlin logo"

    override fun strokes(): List<Stroke> {
        val unitShape = listOf(Vec2(0.0, 0.0), Vec2(1.0, 0.0), Vec2(0.5, 0.5), Vec2(1.0, 1.0), Vec2(0.0, 1.0))
        val half = size / 2
        val corners = unitShape.map { Vec2(center.x - half + it.x * size, center.y - half + it.y * size) }
        return listOf(Stroke(corners, closed = true))
    }
}

/** A filled rectangular block with an even [interval] between neighbours, roughly [aspect] wide per row. */
data class BlockFormation(
    val center: Vec2 = Field.center,
    val interval: Double = DEFAULT_INTERVAL,
    val aspect: Double = BLOCK_ASPECT,
) : Formation {
    override val title: String get() = "Block"

    override fun spots(count: Int): List<Vec2> {
        if (count <= 0) return emptyList()
        val columns = max(1, ceil(sqrt(count * aspect)).toInt())
        val rows = ceil(count.toDouble() / columns).toInt()
        val spots = mutableListOf<Vec2>()
        for (row in 0 until rows) {
            val inRow = minOf(columns, count - row * columns)
            val rowY = center.y + (row - (rows - 1) / 2.0) * interval
            for (col in 0 until inRow) {
                spots += Vec2(center.x + (col - (inRow - 1) / 2.0) * interval, rowY)
            }
        }
        return spots
    }
}

private fun polar(
    radius: Double,
    radians: Double,
): Vec2 = Vec2(radius * cos(radians), radius * sin(radians))

const val DEFAULT_TEXT_HEIGHT: Double = 24.0
const val DEFAULT_RADIUS: Double = 22.0
const val DEFAULT_SIZE: Double = 44.0
const val DEFAULT_INTERVAL: Double = 3.0

private const val FIELD_MARGIN = 4.0
private const val ARC_RADIUS = 40.0
private const val WEDGE_WIDTH = 96.0
private const val WEDGE_DEPTH = 40.0
private const val ARC_START = 20.0
private const val ARC_SWEEP = 140.0
private const val DEGREES_HALF_TURN = 180.0
private const val CURVE_RESOLUTION = 96
private const val STAR_POINTS = 5
private const val STAR_INNER_RATIO = 0.45
private const val BLOCK_ASPECT = 1.6
private const val HEART_X = 16.0
private const val HEART_A = 13.0
private const val HEART_B = 5.0
private const val HEART_C = 2.0
private const val HEART_SPAN = 32.0
