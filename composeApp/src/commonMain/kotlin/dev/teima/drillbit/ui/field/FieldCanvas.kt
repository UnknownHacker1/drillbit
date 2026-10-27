package dev.teima.drillbit.ui.field

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.teima.drillbit.core.geometry.Vec2
import dev.teima.drillbit.core.model.Field
import dev.teima.drillbit.core.model.Performer
import dev.teima.drillbit.ui.theme.DrillbitColors
import kotlin.math.max
import kotlin.math.min

/** Everything the field can draw on top of the turf. Only [positions] is required. */
data class FieldLayers(
    val positions: List<Vec2>,
    /** Where everyone came from; draws a path from here to [positions]. */
    val origins: List<Vec2>? = null,
    /** Hollow markers for the spots of a formation being previewed. */
    val targets: List<Vec2>? = null,
    /** Near misses to circle in red. */
    val collisions: List<Vec2> = emptyList(),
    val highlighted: Int? = null,
    /** A performer's full route through the show, drawn as a numbered line. */
    val route: List<Vec2>? = null,
    /** Fade everyone except [highlighted]. */
    val dimOthers: Boolean = false,
)

/** Maps between field steps and canvas pixels. The front sideline is at the bottom, like the audience sees it. */
internal class FieldTransform(
    size: Size,
) {
    val scale: Float = min(size.width / VIEW_WIDTH, size.height / VIEW_HEIGHT)
    private val left = (size.width - VIEW_WIDTH * scale) / 2
    private val top = (size.height - VIEW_HEIGHT * scale) / 2

    fun toScreen(p: Vec2): Offset =
        Offset(
            left + ((p.x - X_MIN) * scale).toFloat(),
            top + ((Y_MAX - p.y) * scale).toFloat(),
        )

    fun toField(o: Offset): Vec2 = Vec2((o.x - left) / scale + X_MIN, Y_MAX - (o.y - top) / scale)

    fun steps(value: Double): Float = (value * scale).toFloat()

    companion object {
        const val X_MIN = -8.0
        const val X_MAX = Field.LENGTH + 8.0
        const val Y_MIN = -6.0
        const val Y_MAX = Field.DEPTH + 6.0
        val VIEW_WIDTH = (X_MAX - X_MIN).toFloat()
        val VIEW_HEIGHT = (Y_MAX - Y_MIN).toFloat()

        /** Width / height of the drawn field, for sizing containers. */
        val ASPECT: Float = VIEW_WIDTH / VIEW_HEIGHT
    }
}

/**
 * The football field with every performer on it. Tapping selects the nearest performer;
 * dragging moves them when [onDrag] callbacks are provided.
 */
@Composable
fun FieldCanvas(
    field: Field,
    performers: List<Performer>,
    layers: FieldLayers,
    modifier: Modifier = Modifier,
    onTap: (Int?) -> Unit = {},
    onDragStart: ((Int, Vec2) -> Unit)? = null,
    onDrag: (Vec2) -> Unit = {},
    onDragEnd: () -> Unit = {},
) {
    val measurer = rememberTextMeasurer()
    val currentLayers by rememberUpdatedState(layers)
    val tap by rememberUpdatedState(onTap)
    val dragStart by rememberUpdatedState(onDragStart)
    val drag by rememberUpdatedState(onDrag)
    val dragEnd by rememberUpdatedState(onDragEnd)
    val numberStyle = remember { TextStyle(color = DrillbitColors.ChalkFaint, fontWeight = FontWeight.Bold) }

    Canvas(
        modifier
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val transform = FieldTransform(Size(size.width.toFloat(), size.height.toFloat()))
                    tap(hitTest(currentLayers.positions, transform, offset, 18.dp.toPx()))
                }
            }.pointerInput(Unit) {
                var dragging = false
                detectDragGestures(
                    onDragStart = { offset ->
                        val transform = FieldTransform(Size(size.width.toFloat(), size.height.toFloat()))
                        val hit = hitTest(currentLayers.positions, transform, offset, 18.dp.toPx())
                        val start = dragStart
                        dragging = hit != null && start != null
                        if (hit != null && start != null) start(hit, transform.toField(offset))
                    },
                    onDrag = { change, _ ->
                        if (dragging) {
                            change.consume()
                            val transform = FieldTransform(Size(size.width.toFloat(), size.height.toFloat()))
                            drag(transform.toField(change.position))
                        }
                    },
                    onDragEnd = {
                        if (dragging) dragEnd()
                        dragging = false
                    },
                    onDragCancel = {
                        if (dragging) dragEnd()
                        dragging = false
                    },
                )
            },
    ) {
        val transform = FieldTransform(size)
        drawTurf(transform, field)
        drawYardNumbers(transform, measurer, numberStyle)
        drawPerformers(transform, performers, currentLayers, measurer)
    }
}

private fun hitTest(
    positions: List<Vec2>,
    transform: FieldTransform,
    offset: Offset,
    minRadiusPx: Float,
): Int? {
    val radius = max(minRadiusPx, transform.steps(HIT_RADIUS_STEPS))
    return positions.indices
        .map { it to (transform.toScreen(positions[it]) - offset).getDistance() }
        .filter { it.second <= radius }
        .minByOrNull { it.second }
        ?.first
}

private fun DrawScope.drawTurf(
    t: FieldTransform,
    field: Field,
) {
    drawRect(
        DrillbitColors.TurfDark,
        topLeft = t.toScreen(Vec2(FieldTransform.X_MIN, FieldTransform.Y_MAX)),
        size = t.viewSize(),
    )
    val stepsPerLine = Field.STEPS_PER_FIVE_YARDS.toDouble()
    for (i in 0 until LINES) {
        val color = if (i % 2 == 0) DrillbitColors.TurfLight else DrillbitColors.TurfDark
        val topLeft = t.toScreen(Vec2(i * stepsPerLine, Field.DEPTH))
        drawRect(color, topLeft = topLeft, size = Size(t.steps(stepsPerLine), t.steps(Field.DEPTH)))
    }
    val chalk = DrillbitColors.Chalk
    for (i in 0..LINES) {
        val x = i * stepsPerLine
        val width = if (i == LINES / 2) 2.4f else 1.2f
        drawLine(chalk.copy(alpha = 0.55f), t.toScreen(Vec2(x, 0.0)), t.toScreen(Vec2(x, Field.DEPTH)), width)
    }
    val yard = stepsPerLine / YARDS_PER_LINE
    for (i in 1 until LINES * YARDS_PER_LINE) {
        if (i % YARDS_PER_LINE == 0) continue
        val x = i * yard
        for (hashY in listOf(field.frontHash, field.backHash, HASH_TICK, Field.DEPTH - HASH_TICK)) {
            drawLine(
                chalk.copy(alpha = 0.35f),
                t.toScreen(Vec2(x, hashY - HASH_TICK / 2)),
                t.toScreen(Vec2(x, hashY + HASH_TICK / 2)),
                1f,
            )
        }
    }
    drawRect(
        chalk,
        topLeft = t.toScreen(Vec2(0.0, Field.DEPTH)),
        size = Size(t.steps(Field.LENGTH), t.steps(Field.DEPTH)),
        style = Stroke(width = 2.5f),
    )
}

private fun FieldTransform.viewSize(): Size = Size(steps(X_MAX_SPAN), steps(Y_MAX_SPAN))

private fun DrawScope.drawYardNumbers(
    t: FieldTransform,
    measurer: TextMeasurer,
    style: TextStyle,
) {
    val fontSize = (t.steps(NUMBER_HEIGHT_STEPS) / density).sp
    for (k in 2 until LINES step 2) {
        val yards = YARDS_PER_LINE * min(k, LINES - k)
        val layout = measurer.measure(yards.toString(), style.copy(fontSize = fontSize))
        for (rowY in listOf(NUMBER_ROW_STEPS, Field.DEPTH - NUMBER_ROW_STEPS)) {
            val center = t.toScreen(Vec2(k * Field.STEPS_PER_FIVE_YARDS.toDouble(), rowY))
            drawText(layout, topLeft = Offset(center.x - layout.size.width / 2f, center.y - layout.size.height / 2f))
        }
    }
}

private fun DrawScope.drawPerformers(
    t: FieldTransform,
    performers: List<Performer>,
    layers: FieldLayers,
    measurer: TextMeasurer,
) {
    val radius = t.steps(DOT_RADIUS_STEPS).coerceIn(2.5.dp.toPx(), 9.dp.toPx())

    layers.route?.let { route ->
        val path = Path()
        route.forEachIndexed { i, p ->
            val o = t.toScreen(p)
            if (i == 0) path.moveTo(o.x, o.y) else path.lineTo(o.x, o.y)
        }
        drawPath(path, DrillbitColors.Accent, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
        route.forEachIndexed { i, p ->
            val o = t.toScreen(p)
            drawCircle(DrillbitColors.Background, radius * 1.6f, o)
            drawCircle(DrillbitColors.Accent, radius * 1.6f, o, style = Stroke(1.5.dp.toPx()))
            val label = measurer.measure((i + 1).toString(), TextStyle(color = DrillbitColors.Chalk, fontSize = 10.sp))
            drawText(label, topLeft = Offset(o.x - label.size.width / 2f, o.y - label.size.height / 2f))
        }
    }

    layers.origins?.let { origins ->
        for (i in layers.positions.indices) {
            val color =
                DrillbitColors
                    .section(
                        performers[i].section,
                    ).copy(alpha = if (layers.dimOthers) 0.12f else 0.4f)
            drawLine(color, t.toScreen(origins[i]), t.toScreen(layers.positions[i]), 1.5.dp.toPx(), StrokeCap.Round)
        }
    }

    layers.targets?.forEach { target ->
        drawCircle(
            DrillbitColors.Chalk.copy(alpha = 0.8f),
            radius * 1.1f,
            t.toScreen(target),
            style = Stroke(1.2.dp.toPx()),
        )
    }

    layers.positions.forEachIndexed { i, p ->
        val isHighlighted = i == layers.highlighted
        val alpha = if (layers.dimOthers && !isHighlighted) 0.25f else 1f
        val center = t.toScreen(p)
        drawCircle(Color.Black.copy(alpha = 0.35f * alpha), radius * 1.15f, center + Offset(0f, radius * 0.25f))
        drawCircle(DrillbitColors.section(performers[i].section).copy(alpha = alpha), radius, center)
    }

    for (spot in layers.collisions) {
        val center = t.toScreen(spot)
        drawCircle(DrillbitColors.Danger, radius * 2.6f, center, style = Stroke(2.dp.toPx()))
        drawCircle(DrillbitColors.Danger.copy(alpha = 0.25f), radius * 2.6f, center)
    }

    layers.highlighted?.let { i ->
        if (i !in layers.positions.indices) return@let
        val center = t.toScreen(layers.positions[i])
        drawCircle(DrillbitColors.Chalk, radius + 3.dp.toPx(), center, style = Stroke(2.dp.toPx()))
        val label =
            measurer.measure(
                performers[i].label,
                TextStyle(color = DrillbitColors.Chalk, fontSize = 12.sp, fontWeight = FontWeight.Bold),
            )
        val labelTop = center.y - radius - 6.dp.toPx() - label.size.height
        drawRect(
            DrillbitColors.Background.copy(alpha = 0.8f),
            topLeft = Offset(center.x - label.size.width / 2f - 4.dp.toPx(), labelTop - 2.dp.toPx()),
            size = Size(label.size.width + 8.dp.toPx(), label.size.height + 4.dp.toPx()),
        )
        drawText(label, topLeft = Offset(center.x - label.size.width / 2f, labelTop))
    }
}

private const val LINES = 20
private const val YARDS_PER_LINE = 5
private const val HASH_TICK = 1.2
private const val DOT_RADIUS_STEPS = 1.25
private const val HIT_RADIUS_STEPS = 3.0
private const val NUMBER_HEIGHT_STEPS = 4.5
private const val NUMBER_ROW_STEPS = 19.0
private const val X_MAX_SPAN = FieldTransform.X_MAX - FieldTransform.X_MIN
private const val Y_MAX_SPAN = FieldTransform.Y_MAX - FieldTransform.Y_MIN
