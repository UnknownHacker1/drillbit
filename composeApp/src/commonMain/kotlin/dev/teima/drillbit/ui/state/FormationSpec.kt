package dev.teima.drillbit.ui.state

import dev.teima.drillbit.core.geometry.Vec2
import dev.teima.drillbit.core.model.Field
import dev.teima.drillbit.core.shapes.ArcFormation
import dev.teima.drillbit.core.shapes.BlockFormation
import dev.teima.drillbit.core.shapes.CircleFormation
import dev.teima.drillbit.core.shapes.Formation
import dev.teima.drillbit.core.shapes.HeartFormation
import dev.teima.drillbit.core.shapes.KotlinLogoFormation
import dev.teima.drillbit.core.shapes.LineFormation
import dev.teima.drillbit.core.shapes.StarFormation
import dev.teima.drillbit.core.shapes.TextFormation
import dev.teima.drillbit.core.shapes.WedgeFormation

/** The shapes offered in the formation picker, with the meaning and range of their size slider. */
enum class FormationKind(
    val label: String,
    val sizeLabel: String,
    val sizeRange: ClosedFloatingPointRange<Double>,
    val defaultSize: Double,
) {
    TEXT("Text", "Letter height", 10.0..40.0, 24.0),
    LOGO("Kotlin", "Size", 20.0..80.0, 44.0),
    CIRCLE("Circle", "Radius", 8.0..40.0, 24.0),
    ARC("Arc", "Radius", 12.0..60.0, 40.0),
    STAR("Star", "Radius", 12.0..42.0, 26.0),
    HEART("Heart", "Width", 24.0..90.0, 52.0),
    WEDGE("Wedge", "Width", 30.0..150.0, 96.0),
    LINE("Line", "Length", 20.0..150.0, 120.0),
    BLOCK("Block", "Interval", 2.0..6.0, 3.0),
}

/** What the formation panel is currently describing. Turned into a core [Formation] on demand. */
data class FormationSpec(
    val kind: FormationKind = FormationKind.TEXT,
    val text: String = "HELLO",
    val center: Vec2 = Field.center,
    val size: Double = kind.defaultSize,
) {
    fun withKind(newKind: FormationKind): FormationSpec = copy(kind = newKind, size = newKind.defaultSize)

    fun toFormation(): Formation =
        when (kind) {
            FormationKind.TEXT -> {
                TextFormation(text.ifBlank { " " }, center, size)
            }

            FormationKind.LOGO -> {
                KotlinLogoFormation(center, size)
            }

            FormationKind.CIRCLE -> {
                CircleFormation(center, size)
            }

            FormationKind.ARC -> {
                ArcFormation(center, size)
            }

            FormationKind.STAR -> {
                StarFormation(center, outerRadius = size, innerRadius = size * STAR_INNER)
            }

            FormationKind.HEART -> {
                HeartFormation(center, size)
            }

            FormationKind.WEDGE -> {
                WedgeFormation(center, width = size, depth = size * WEDGE_DEPTH)
            }

            FormationKind.LINE -> {
                LineFormation(Vec2(center.x - size / 2, center.y), Vec2(center.x + size / 2, center.y))
            }

            FormationKind.BLOCK -> {
                BlockFormation(center, interval = size)
            }
        }

    /** Name given to a set made from this spec. */
    val setName: String
        get() = if (kind == FormationKind.TEXT) text.uppercase().trim().ifEmpty { "Block" } else kind.label

    private companion object {
        const val STAR_INNER = 0.45
        const val WEDGE_DEPTH = 0.42
    }
}
