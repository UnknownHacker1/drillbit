package dev.teima.drillbit.ui.perform

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.teima.drillbit.core.dotbook.DotBookEntry
import dev.teima.drillbit.core.dotbook.dotBook
import dev.teima.drillbit.core.dotbook.formatSteps
import dev.teima.drillbit.ui.components.PanelCard
import dev.teima.drillbit.ui.state.DrillbitState
import dev.teima.drillbit.ui.theme.DrillbitColors

/** Performer side: choose yourself, then read your dot book set by set. */
@Composable
fun PerformPanel(
    state: DrillbitState,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        val performer = state.selectedPerformer
        if (performer == null) {
            PerformerPicker(state)
        } else {
            DotBookCard(state, performer)
        }
    }
}

@Composable
private fun PerformerPicker(state: DrillbitState) {
    PanelCard("Who are you?") {
        Text(
            "Tap your dot on the field or pick your label to get your own dot book.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        state.show.performers
            .withIndex()
            .groupBy { it.value.section }
            .forEach { (section, members) ->
                Text(
                    section.displayName,
                    fontSize = 12.sp,
                    color = DrillbitColors.section(section),
                    fontWeight = FontWeight.Bold,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    members.forEach { (index, performer) ->
                        Text(
                            performer.label,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            modifier =
                                Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(
                                        1.dp,
                                        DrillbitColors.section(section).copy(alpha = 0.6f),
                                        RoundedCornerShape(8.dp),
                                    ).clickable { state.selectedPerformer = index }
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                        )
                    }
                }
            }
    }
}

@Composable
private fun DotBookCard(
    state: DrillbitState,
    performerIndex: Int,
) {
    val performer = state.show.performers[performerIndex]
    val book = dotBook(state.show, performerIndex)
    PanelCard(
        "Dot book · ${performer.label} · ${performer.section.displayName}",
        trailing = { TextButton(onClick = { state.selectedPerformer = null }) { Text("Change") } },
    ) {
        book.forEach { entry ->
            DotBookRow(
                entry,
                selected = entry.setIndex == state.selectedSet,
                onClick = { state.selectSet(entry.setIndex) },
            )
        }
    }
}

@Composable
private fun DotBookRow(
    entry: DotBookEntry,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (selected) DrillbitColors.AccentStrong.copy(alpha = 0.22f) else DrillbitColors.SurfaceHigh)
            .border(1.dp, if (selected) DrillbitColors.Accent else DrillbitColors.SurfaceHigh, shape)
            .clickable(onClick = onClick)
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.width(44.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("SET", fontSize = 10.sp, color = DrillbitColors.Muted, fontWeight = FontWeight.Bold)
            Text("${entry.setIndex + 1}", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(entry.setName, fontWeight = FontWeight.SemiBold)
            Text(entry.sideToSide, fontSize = 13.sp)
            Text(entry.frontToBack, fontSize = 13.sp)
            val move = entry.move
            if (move == null) {
                Text("Starting position", fontSize = 12.sp, color = DrillbitColors.Muted)
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(DrillbitColors.stride(move.difficulty)))
                    val description =
                        if (move.isHold) {
                            "${move.counts} counts: hold your spot"
                        } else {
                            val steps = formatSteps(move.distance)
                            "${move.counts} counts · ${move.stepSizeLabel} · $steps steps ${move.direction}"
                        }
                    Text(description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
