package dev.teima.drillbit.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.teima.drillbit.core.geometry.lerp
import dev.teima.drillbit.platform.playTick
import dev.teima.drillbit.ui.components.PlaybackBar
import dev.teima.drillbit.ui.components.SetStrip
import dev.teima.drillbit.ui.design.DesignPanel
import dev.teima.drillbit.ui.field.FieldCanvas
import dev.teima.drillbit.ui.field.FieldLayers
import dev.teima.drillbit.ui.field.FieldTransform
import dev.teima.drillbit.ui.perform.PerformPanel
import dev.teima.drillbit.ui.share.ImportDialog
import dev.teima.drillbit.ui.share.ShareDialog
import dev.teima.drillbit.ui.state.DrillbitState
import dev.teima.drillbit.ui.state.Mode
import dev.teima.drillbit.ui.theme.DrillbitColors
import kotlinx.coroutines.delay

/** The whole app: one screen that rearranges itself for phones and for wide windows. */
@Composable
fun DrillbitApp(state: DrillbitState) {
    var sharing by remember { mutableStateOf(false) }
    var importing by remember { mutableStateOf(false) }
    PlaybackLoop(state)

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        BoxWithConstraints(Modifier.fillMaxSize().safeDrawingPadding()) {
            val wide = maxWidth >= WIDE_LAYOUT
            Column(Modifier.fillMaxSize()) {
                TopBar(state, wide, onShare = { sharing = true }, onImport = { importing = true })
                if (wide) WideLayout(state) else NarrowLayout(state)
            }
            MessageBar(state, Modifier.align(Alignment.BottomCenter))
        }
    }

    if (sharing) ShareDialog(state.shareCode, state.show.performers.size) { sharing = false }
    if (importing) ImportDialog(onImport = state::importFrom, onDismiss = { importing = false })
}

@Composable
private fun PlaybackLoop(state: DrillbitState) {
    remember(state) { state.onTick = ::playTick }
    LaunchedEffect(state.playing) {
        if (!state.playing) return@LaunchedEffect
        var last = withFrameNanos { it }
        while (state.playing) {
            withFrameNanos { now ->
                state.advance((now - last) / NANOS_PER_SECOND)
                last = now
            }
        }
    }
}

@Composable
private fun TopBar(
    state: DrillbitState,
    wide: Boolean,
    onShare: () -> Unit,
    onImport: () -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Drillbit", fontWeight = FontWeight.Black, fontSize = 22.sp, color = DrillbitColors.Chalk)
                Text(
                    state.show.title,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
            if (wide) Actions(state, onShare, onImport)
            ModeSwitch(state)
        }
        if (!wide) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { Actions(state, onShare, onImport) }
        }
    }
}

@Composable
private fun Actions(
    state: DrillbitState,
    onShare: () -> Unit,
    onImport: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        TextButton(onClick = state::undo, enabled = state.history.canUndo) { Text("Undo") }
        TextButton(onClick = state::redo, enabled = state.history.canRedo) { Text("Redo") }
        TextButton(onClick = onImport) { Text("Open") }
        TextButton(onClick = onShare) { Text("Share QR") }
    }
}

@Composable
private fun ModeSwitch(state: DrillbitState) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(start = 8.dp)) {
        FilterChip(
            selected = state.mode == Mode.DESIGN,
            onClick = { state.mode = Mode.DESIGN },
            label = { Text("Design") },
        )
        FilterChip(
            selected = state.mode == Mode.PERFORM,
            onClick = {
                state.closeDraft()
                state.mode = Mode.PERFORM
            },
            label = { Text("Perform") },
        )
    }
}

@Composable
private fun WideLayout(state: DrillbitState) {
    Row(Modifier.fillMaxSize().padding(start = 8.dp, end = 16.dp, bottom = 12.dp)) {
        Column(Modifier.weight(1f).fillMaxHeight()) {
            FieldArea(state, Modifier.weight(1f).fillMaxWidth())
            PlaybackBar(state)
            SetStrip(state, onAddSet = addSetAction(state))
        }
        Column(
            Modifier
                .width(SIDE_PANEL)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
                .padding(start = 8.dp),
        ) {
            Panel(state)
        }
    }
}

@Composable
private fun NarrowLayout(state: DrillbitState) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
        FieldArea(state, Modifier.fillMaxWidth().aspectRatio(FieldTransform.ASPECT))
        PlaybackBar(state)
        SetStrip(state, onAddSet = addSetAction(state))
        Panel(state, Modifier.padding(horizontal = 12.dp, vertical = 12.dp))
    }
}

private fun addSetAction(state: DrillbitState): (() -> Unit)? =
    if (state.mode == Mode.DESIGN) {
        {
            state.selectSet(state.show.sets.lastIndex)
            state.openDraft(replace = false)
        }
    } else {
        null
    }

@Composable
private fun Panel(
    state: DrillbitState,
    modifier: Modifier = Modifier,
) {
    when (state.mode) {
        Mode.DESIGN -> DesignPanel(state, modifier)
        Mode.PERFORM -> PerformPanel(state, modifier)
    }
}

/** The field plus the floating set / count label. */
@Composable
private fun FieldArea(
    state: DrillbitState,
    modifier: Modifier = Modifier,
) {
    val show = state.show
    val layers = fieldLayers(state)
    val designing = state.mode == Mode.DESIGN && state.draft == null
    Box(modifier) {
        FieldCanvas(
            field = show.field,
            performers = show.performers,
            layers = layers,
            modifier = Modifier.fillMaxSize(),
            onTap = { tapped ->
                // In perform mode a tap on empty turf keeps your dot book open.
                if (tapped != null || state.mode == Mode.DESIGN) state.selectedPerformer = tapped
            },
            onDragStart = if (designing) state::startDrag else null,
            onDrag = state::dragTo,
            onDragEnd = state::endDrag,
        )
        CountLabel(state, Modifier.align(Alignment.TopStart).padding(12.dp))
    }
}

@Composable
private fun fieldLayers(state: DrillbitState): FieldLayers {
    val show = state.show
    val draft = state.draft
    val preview = state.preview
    if (state.mode == Mode.DESIGN && draft != null && preview != null) {
        val plan = preview.plans.getValue(draft.strategy)
        val report = preview.reports.getValue(draft.strategy)
        val t by rememberInfiniteTransition().animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec =
                infiniteRepeatable(
                    keyframes {
                        durationMillis = PREVIEW_MILLIS
                        0f at PREVIEW_HOLD_MILLIS
                        1f at PREVIEW_MILLIS - PREVIEW_HOLD_MILLIS
                    },
                    RepeatMode.Restart,
                ),
        )
        val walking = preview.from.indices.map { lerp(preview.from[it], plan[it], t.toDouble()) }
        return FieldLayers(
            positions = walking,
            origins = preview.from,
            targets = plan,
            collisions = report.collisions.map { it.location(preview.from, plan) },
        )
    }
    val positions = state.currentPositions()
    val selected = state.selectedPerformer
    if (state.mode == Mode.PERFORM) {
        return FieldLayers(
            positions = positions,
            highlighted = selected,
            route = selected?.let { p -> show.sets.map { it.positions[p] } },
            dimOthers = selected != null,
        )
    }
    val index = state.selectedSet
    val atRest = !state.playing && state.drag == null && positions == show.sets[index].positions
    val report = state.reportFor(index)
    return if (atRest && index > 0 && report != null) {
        val from = show.sets[index - 1].positions
        FieldLayers(
            positions = positions,
            origins = from,
            collisions = report.collisions.map { it.location(from, positions) },
            highlighted = selected,
        )
    } else {
        FieldLayers(positions = positions, highlighted = selected)
    }
}

@Composable
private fun CountLabel(
    state: DrillbitState,
    modifier: Modifier = Modifier,
) {
    val frame = state.timeline.frameAt(state.playhead)
    val set = state.show.sets[state.selectedSet]
    val text =
        when {
            state.draft != null -> {
                "Previewing: ${state.draft?.spec?.setName}"
            }

            state.playing && frame.setIndex > 0 -> {
                "SET ${frame.setIndex + 1} · count ${frame.countInSet.toInt() + 1} of ${state.show.sets[frame.setIndex].counts}"
            }

            else -> {
                "SET ${state.selectedSet + 1} · ${set.name}"
            }
        }
    Text(
        text,
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
        color = DrillbitColors.Chalk,
        modifier =
            modifier
                .clip(RoundedCornerShape(8.dp))
                .background(DrillbitColors.Background.copy(alpha = 0.75f))
                .padding(horizontal = 10.dp, vertical = 5.dp),
    )
}

@Composable
private fun MessageBar(
    state: DrillbitState,
    modifier: Modifier = Modifier,
) {
    val message = state.message ?: return
    LaunchedEffect(message) {
        delay(MESSAGE_MILLIS)
        state.message = null
    }
    Text(
        message,
        color = DrillbitColors.Chalk,
        modifier =
            modifier
                .padding(16.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(DrillbitColors.SurfaceHigh)
                .padding(horizontal = 16.dp, vertical = 12.dp),
    )
}

private val WIDE_LAYOUT = 900.dp
private val SIDE_PANEL = 380.dp
private const val NANOS_PER_SECOND = 1e9
private const val MESSAGE_MILLIS = 4_000L
private const val PREVIEW_MILLIS = 3_200
private const val PREVIEW_HOLD_MILLIS = 400
