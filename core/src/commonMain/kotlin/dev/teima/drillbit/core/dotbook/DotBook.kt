package dev.teima.drillbit.core.dotbook

import dev.teima.drillbit.core.geometry.Vec2
import dev.teima.drillbit.core.model.Field
import dev.teima.drillbit.core.model.Show
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * One line of a performer's dot book: where to stand at the end of a set, written the way
 * drill charts write it, plus how to get there from the previous set.
 */
data class DotBookEntry(
    val setIndex: Int,
    val setName: String,
    val counts: Int,
    val position: Vec2,
    val sideToSide: String,
    val frontToBack: String,
    /** How the performer moves into this set; null for the opening set. */
    val move: Move?,
)

/** A move between two sets for one performer. */
data class Move(
    val distance: Double,
    val counts: Int,
    val direction: String,
) {
    /** Steps per count, where 1.0 is a standard 8-to-5 step. */
    val stride: Double get() = distance / counts

    /** True when the performer stays put (marks time). */
    val isHold: Boolean get() = distance < HOLD_DISTANCE

    /**
     * The stride as drill writers say it: "N to 5" means N steps cover five yards.
     * 8 to 5 is the standard step, smaller N is a bigger step.
     */
    val stepSizeLabel: String
        get() = if (isHold) "hold" else "${formatSteps(Field.STEPS_PER_FIVE_YARDS / stride)} to 5"

    val difficulty: StrideDifficulty
        get() =
            when {
                isHold -> StrideDifficulty.HOLD
                stride > TOO_LARGE_STRIDE -> StrideDifficulty.TOO_LARGE
                stride > LARGE_STRIDE -> StrideDifficulty.LARGE
                else -> StrideDifficulty.COMFORTABLE
            }
}

enum class StrideDifficulty {
    HOLD,
    COMFORTABLE,
    LARGE,
    TOO_LARGE,
}

/** Builds the dot book for performer [performerIndex] of [show]. */
fun dotBook(
    show: Show,
    performerIndex: Int,
): List<DotBookEntry> =
    show.sets.mapIndexed { index, set ->
        val position = set.positions[performerIndex]
        val move =
            if (index == 0) {
                null
            } else {
                val previous = show.sets[index - 1].positions[performerIndex]
                Move(previous.distanceTo(position), set.counts, describeDirection(position - previous))
            }
        DotBookEntry(
            setIndex = index,
            setName = set.name,
            counts = set.counts,
            position = position,
            sideToSide = describeSideToSide(position.x),
            frontToBack = describeFrontToBack(position.y, show.field),
            move = move,
        )
    }

/**
 * Side-to-side position relative to the nearest yard line, e.g. "Side 1: 2.5 steps inside the 35".
 * Side 1 is the audience's left. "Inside" means towards the 50.
 */
fun describeSideToSide(x: Double): String {
    val stepsPerLine = Field.STEPS_PER_FIVE_YARDS
    val lineIndex = (x / stepsPerLine).roundToInt().coerceIn(0, LINES_PER_FIELD)
    val lineX = lineIndex * stepsPerLine.toDouble()
    val offset = roundToQuarter(x - lineX)
    val yardLine = YARDS_PER_LINE * minOf(lineIndex, LINES_PER_FIELD - lineIndex)
    val lineName = if (yardLine == 0) "goal line" else "$yardLine yd line"

    if (lineIndex == LINES_PER_FIELD / 2) {
        return when {
            offset == 0.0 -> "On the 50"
            offset < 0 -> "Side 1: ${formatSteps(-offset)} ${stepWord(-offset)} from the 50"
            else -> "Side 2: ${formatSteps(offset)} ${stepWord(offset)} from the 50"
        }
    }
    val side = if (lineIndex < LINES_PER_FIELD / 2) 1 else 2
    if (offset == 0.0) return "Side $side: On $lineName"
    val towardsFifty = if (side == 1) offset > 0 else offset < 0
    val relation = if (towardsFifty) "inside" else "outside"
    return "Side $side: ${formatSteps(abs(offset))} ${stepWord(offset)} $relation $lineName"
}

/** Front-to-back position relative to the nearest sideline or hash, e.g. "4 steps behind front hash". */
fun describeFrontToBack(
    y: Double,
    field: Field,
): String {
    val references =
        listOf(
            "front sideline" to 0.0,
            "front hash" to field.frontHash,
            "back hash" to field.backHash,
            "back sideline" to Field.DEPTH,
        )
    val (name, refY) = references.minBy { abs(y - it.second) }
    val offset = roundToQuarter(y - refY)
    return when {
        offset == 0.0 -> "On $name"
        offset > 0 -> "${formatSteps(offset)} ${stepWord(offset)} behind $name"
        else -> "${formatSteps(-offset)} ${stepWord(offset)} in front of $name"
    }
}

/** Direction of travel as the performer sees it facing the audience. */
fun describeDirection(delta: Vec2): String {
    if (delta.length < HOLD_DISTANCE) return "mark time"
    val forwardBack =
        when {
            delta.y < -delta.length * AXIS_SHARE -> "forward"
            delta.y > delta.length * AXIS_SHARE -> "backward"
            else -> null
        }
    val sideways =
        when {
            delta.x < -delta.length * AXIS_SHARE -> "to side 1"
            delta.x > delta.length * AXIS_SHARE -> "to side 2"
            else -> null
        }
    return listOfNotNull(forwardBack, sideways).joinToString(" and ")
}

/** Formats steps with up to two decimals and no trailing zeros: 2.0 -> "2", 2.25 -> "2.25". */
fun formatSteps(value: Double): String {
    val hundredths = (value * HUNDRED).roundToInt()
    val whole = hundredths / HUNDRED_INT
    val fraction = abs(hundredths % HUNDRED_INT)
    return when {
        fraction == 0 -> "$whole"
        fraction % TEN_INT == 0 -> "$whole.${fraction / TEN_INT}"
        else -> "$whole.${fraction.toString().padStart(2, '0')}"
    }
}

private fun stepWord(steps: Double): String = if (abs(steps) == 1.0) "step" else "steps"

private fun roundToQuarter(value: Double): Double = (value * QUARTERS).roundToInt() / QUARTERS

private const val LINES_PER_FIELD = 20
private const val YARDS_PER_LINE = 5
private const val QUARTERS = 4.0
private const val HOLD_DISTANCE = 0.125
private const val LARGE_STRIDE = 1.0 + 1e-9
private const val TOO_LARGE_STRIDE = 8.0 / 6.0 + 1e-9

/** A component counts as a direction when it is at least sin(22.5 deg) of the move. */
private const val AXIS_SHARE = 0.3827
private const val HUNDRED = 100.0
private const val HUNDRED_INT = 100
private const val TEN_INT = 10
