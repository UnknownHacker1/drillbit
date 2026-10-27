package dev.teima.drillbit.ui.design

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.teima.drillbit.core.dotbook.formatSteps
import dev.teima.drillbit.core.dotbook.strideLabel
import dev.teima.drillbit.core.geometry.Vec2
import dev.teima.drillbit.core.model.Field
import dev.teima.drillbit.core.plan.AssignmentStrategy
import dev.teima.drillbit.core.plan.MAX_COMFORTABLE_STRIDE
import dev.teima.drillbit.core.plan.TransitionReport
import dev.teima.drillbit.ui.components.Badge
import dev.teima.drillbit.ui.components.PanelCard
import dev.teima.drillbit.ui.components.StatRow
import dev.teima.drillbit.ui.state.DrillbitState
import dev.teima.drillbit.ui.state.FormationKind
import dev.teima.drillbit.ui.state.MorphDraft
import dev.teima.drillbit.ui.theme.DrillbitColors

/** Side panel of the drill writer: the formation builder, the selected move, and show settings. */
@Composable
fun DesignPanel(
    state: DrillbitState,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        val draft = state.draft
        if (draft != null) {
            FormationCard(state, draft)
        } else {
            QuickActions(state)
            TransitionCard(state)
        }
        ShowCard(state)
    }
}

@Composable
private fun QuickActions(state: DrillbitState) {
    PanelCard("Formations") {
        Text(
            "Pick a shape or type a word. Drillbit works out who walks where and checks every path for collisions.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { state.openDraft(replace = false) }) { Text("New set after ${state.selectedSet + 1}") }
            if (state.selectedSet > 0) {
                OutlinedButton(onClick = { state.openDraft(replace = true) }) { Text("Reshape set") }
            }
        }
    }
}

@Composable
private fun FormationCard(
    state: DrillbitState,
    draft: MorphDraft,
) {
    val spec = draft.spec
    val title = if (draft.replace) "Reshape set ${state.selectedSet + 1}" else "New set after ${state.selectedSet + 1}"
    PanelCard(title, trailing = { TextButton(onClick = state::closeDraft) { Text("Cancel") } }) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            FormationKind.entries.forEach { kind ->
                FilterChip(
                    selected = kind == spec.kind,
                    onClick = { state.updateDraft(draft.copy(spec = spec.withKind(kind))) },
                    label = { Text(kind.label) },
                )
            }
        }
        if (spec.kind == FormationKind.TEXT) {
            OutlinedTextField(
                value = spec.text,
                onValueChange = { state.updateDraft(draft.copy(spec = spec.copy(text = it.take(MAX_TEXT)))) },
                label = { Text("Word to spell") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        LabeledSlider(spec.kind.sizeLabel, spec.size, spec.kind.sizeRange) {
            state.updateDraft(draft.copy(spec = spec.copy(size = it)))
        }
        LabeledSlider("Side to side", spec.center.x, 20.0..(Field.LENGTH - 20.0)) {
            state.updateDraft(draft.copy(spec = spec.copy(center = Vec2(it, spec.center.y))))
        }
        LabeledSlider("Front to back", spec.center.y, 12.0..(Field.DEPTH - 12.0)) {
            state.updateDraft(draft.copy(spec = spec.copy(center = Vec2(spec.center.x, it))))
        }

        Text(
            "Who goes where?",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(top = 4.dp),
        )
        val preview = state.preview
        AssignmentStrategy.entries.forEach { strategy ->
            val report = preview?.reports?.get(strategy)
            if (report != null) {
                StrategyRow(
                    strategy = strategy,
                    report = report,
                    selected = strategy == draft.strategy,
                    onClick = { state.updateDraft(draft.copy(strategy = strategy)) },
                )
            }
        }
        Button(onClick = state::applyDraft, modifier = Modifier.fillMaxWidth()) {
            Text(if (draft.replace) "Apply to set ${state.selectedSet + 1}" else "Add set ${state.selectedSet + 2}")
        }
    }
}

@Composable
private fun StrategyRow(
    strategy: AssignmentStrategy,
    report: TransitionReport,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(12.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (selected) DrillbitColors.AccentStrong.copy(alpha = 0.22f) else DrillbitColors.SurfaceHigh)
            .border(1.dp, if (selected) DrillbitColors.Accent else DrillbitColors.SurfaceHigh, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(strategy.displayName, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            SafetyBadge(report, short = true)
        }
        val longest = formatSteps(report.longestDistance)
        Text(
            "Total walking ${report.totalTravel.toInt()} steps  ·  longest $longest steps",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun TransitionCard(state: DrillbitState) {
    val index = state.selectedSet
    val set = state.show.sets[index]
    val report = state.reportFor(index)
    PanelCard("Set ${index + 1} · ${set.name}") {
        if (report == null) {
            Text(
                "The opening set is where everyone starts. Select a later set to inspect its move, or drag dots to adjust.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Counts", modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedButton(onClick = { state.changeCounts(-COUNT_STEP) }) { Text("-4") }
                Text(
                    "${set.counts}",
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(44.dp).padding(horizontal = 8.dp),
                    style = MaterialTheme.typography.titleMedium,
                )
                OutlinedButton(onClick = { state.changeCounts(COUNT_STEP) }) { Text("+4") }
            }
            val collisionColor = if (report.isCollisionFree) DrillbitColors.Safe else DrillbitColors.Danger
            StatRow("Collisions (exact check)", "${report.collisions.size}", collisionColor)
            val stride = report.largestStride
            val strideColor =
                when {
                    stride > MAX_COMFORTABLE_STRIDE + EPSILON -> DrillbitColors.Danger
                    stride > 1.0 + EPSILON -> DrillbitColors.Warning
                    else -> DrillbitColors.Safe
                }
            val mover = state.show.performers[report.longestMover].label
            val strideText = if (stride < EPSILON) "everyone holds" else "${strideLabel(stride)} ($mover)"
            StatRow("Biggest step", strideText, strideColor)
            StatRow("Total walking", "${report.totalTravel.toInt()} steps")
            StatRow("Tightest interval", "${formatSteps(report.tightestSpacing)} steps")
            SafetyBadge(report, short = false)
            Text(safetyExplanation(report), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (stride > MAX_COMFORTABLE_STRIDE + EPSILON) {
                    OutlinedButton(onClick = state::fixCounts) { Text("Fix counts") }
                }
                OutlinedButton(onClick = state::optimizeEverything) { Text("Optimize all moves") }
                TextButton(onClick = state::deleteSelectedSet) { Text("Delete set", color = DrillbitColors.Danger) }
            }
        }
    }
}

/** "Provably safe" only when the CAPT condition holds; otherwise the result of the exact check. */
@Composable
private fun SafetyBadge(
    report: TransitionReport,
    short: Boolean,
) {
    when {
        report.provablySafe -> Badge(if (short) "PROVABLY SAFE" else "PROVABLY COLLISION-FREE", DrillbitColors.Safe)
        report.isCollisionFree -> Badge("CHECKED: 0 COLLISIONS", DrillbitColors.Safe)
        else -> Badge("${report.collisions.size} COLLISIONS", DrillbitColors.Danger)
    }
}

private fun safetyExplanation(report: TransitionReport): String {
    val spacing = formatSteps(report.tightestSpacing)
    val required = formatSteps(report.requiredSpacing)
    val checked = "The exact pairwise check finds ${report.collisions.size} collisions."
    return when {
        report.provablySafe -> {
            "Squared-distance optimal assignment, and every start and end spot is at least $spacing steps apart " +
                "(the CAPT proof needs $required). So these straight, synchronised paths stay at least " +
                "1 step apart by proof. $checked"
        }

        !report.optimalAssignment -> {
            "No proof for this move: the assignment is not the squared-distance optimum " +
                "(Optimize all moves fixes that). $checked"
        }

        else -> {
            "No proof for this move: spots are only $spacing steps apart and the CAPT proof needs $required. $checked"
        }
    }
}

@Composable
private fun ShowCard(state: DrillbitState) {
    val show = state.show
    PanelCard("Show") {
        StatRow("Title", show.title)
        StatRow("Performers", "${show.performers.size}")
        StatRow("Sets", "${show.sets.size}")
        StatRow("Length", "${show.totalCounts} counts at ${show.tempoBpm} bpm")
        Spacer(Modifier.padding(top = 2.dp))
        Text(
            "Tip: tap a dot to see who it is, drag it to fine-tune. Undo is always one click away.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun LabeledSlider(
    label: String,
    value: Double,
    range: ClosedFloatingPointRange<Double>,
    onChange: (Double) -> Unit,
) {
    Column {
        Row {
            Text(
                label,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Text("${formatSteps(value)} steps", fontSize = 13.sp)
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { onChange(it.toDouble()) },
            valueRange = range.start.toFloat()..range.endInclusive.toFloat(),
        )
    }
}

private const val MAX_TEXT = 12
private const val COUNT_STEP = 4
private const val EPSILON = 1e-9
