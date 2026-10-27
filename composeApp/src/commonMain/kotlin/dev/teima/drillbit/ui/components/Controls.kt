package dev.teima.drillbit.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.teima.drillbit.ui.state.DrillbitState
import dev.teima.drillbit.ui.theme.DrillbitColors
import kotlin.math.floor

/** A titled card used for every side-panel section. */
@Composable
fun PanelCard(
    title: String,
    modifier: Modifier = Modifier,
    trailing: @Composable () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    title.uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.5.sp,
                    modifier = Modifier.weight(1f),
                )
                trailing()
            }
            content()
        }
    }
}

/** A label on the left and a value on the right, optionally coloured. */
@Composable
fun StatRow(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium, color = valueColor, fontWeight = FontWeight.SemiBold)
    }
}

/** A small rounded tag, e.g. "PROVABLY SAFE". */
@Composable
fun Badge(
    text: String,
    color: Color,
) {
    Text(
        text,
        color = color,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.8.sp,
        modifier =
            Modifier
                .clip(RoundedCornerShape(50))
                .background(color.copy(alpha = 0.14f))
                .border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(50))
                .padding(horizontal = 10.dp, vertical = 3.dp),
    )
}

/** Play / pause, the show scrubber and the current count. */
@Composable
fun PlaybackBar(
    state: DrillbitState,
    modifier: Modifier = Modifier,
) {
    val timeline = state.timeline
    val total = timeline.totalCounts
    Row(
        modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        FilledIconButton(
            onClick = state::togglePlay,
            enabled = total > 0,
            colors = IconButtonDefaults.filledIconButtonColors(containerColor = DrillbitColors.AccentStrong),
        ) {
            PlayPauseIcon(playing = state.playing)
        }
        Slider(
            value = state.playhead.toFloat(),
            onValueChange = { state.scrubTo(it.toDouble()) },
            valueRange = 0f..maxOf(1, total).toFloat(),
            modifier = Modifier.weight(1f),
        )
        Column(horizontalAlignment = Alignment.End) {
            Text(
                "${floor(state.playhead).toInt()} / $total",
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text("${state.show.tempoBpm} bpm", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        TextButton(onClick = { state.soundOn = !state.soundOn }, contentPadding = PaddingValues(horizontal = 8.dp)) {
            Text(if (state.soundOn) "Click on" else "Click off", fontSize = 12.sp)
        }
    }
}

/** Triangle or two bars, drawn so no icon font is needed on any platform. */
@Composable
private fun PlayPauseIcon(playing: Boolean) {
    Canvas(Modifier.size(16.dp)) {
        val w = size.width
        val h = size.height
        if (playing) {
            val bar = w * 0.32f
            drawRect(Color.White, topLeft = Offset(w * 0.08f, 0f), size = Size(bar, h))
            drawRect(Color.White, topLeft = Offset(w * 0.6f, 0f), size = Size(bar, h))
        } else {
            val path =
                Path().apply {
                    moveTo(w * 0.15f, 0f)
                    lineTo(w, h / 2)
                    lineTo(w * 0.15f, h)
                    close()
                }
            drawPath(path, Color.White)
        }
    }
}

/** The sets of the show as a horizontal strip, with a status dot for each move. */
@Composable
fun SetStrip(
    state: DrillbitState,
    modifier: Modifier = Modifier,
    onAddSet: (() -> Unit)? = null,
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        itemsIndexed(state.show.sets) { index, set ->
            val selectedFill = DrillbitColors.AccentStrong.copy(alpha = 0.3f)
            val selected = index == state.selectedSet
            val report = state.reportFor(index)
            val statusColor =
                when {
                    report == null -> DrillbitColors.Muted
                    !report.isCollisionFree -> DrillbitColors.Danger
                    else -> DrillbitColors.Safe
                }
            Column(
                Modifier
                    .width(112.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (selected) selectedFill else MaterialTheme.colorScheme.surface)
                    .border(
                        1.dp,
                        if (selected) DrillbitColors.Accent else MaterialTheme.colorScheme.outline,
                        RoundedCornerShape(12.dp),
                    ).clickable { state.selectSet(index) }
                    .padding(horizontal = 10.dp, vertical = 8.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "SET ${index + 1}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = DrillbitColors.Muted,
                    )
                    Spacer(Modifier.weight(1f))
                    Box(Modifier.size(8.dp).clip(CircleShape).background(statusColor))
                }
                Text(set.name, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
                Text(
                    if (index == 0) "opening" else "${set.counts} counts",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (onAddSet != null) {
            item {
                Box(
                    Modifier
                        .width(96.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, DrillbitColors.Accent.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                        .clickable(onClick = onAddSet)
                        .padding(vertical = 22.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("+ New set", color = DrillbitColors.Accent, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
