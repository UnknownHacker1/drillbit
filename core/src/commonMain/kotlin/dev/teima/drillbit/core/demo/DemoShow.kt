package dev.teima.drillbit.core.demo

import dev.teima.drillbit.core.edit.insertSet
import dev.teima.drillbit.core.edit.newShow
import dev.teima.drillbit.core.edit.planMorph
import dev.teima.drillbit.core.model.Section
import dev.teima.drillbit.core.model.Show
import dev.teima.drillbit.core.plan.AssignmentStrategy
import dev.teima.drillbit.core.shapes.BlockFormation
import dev.teima.drillbit.core.shapes.CircleFormation
import dev.teima.drillbit.core.shapes.Formation
import dev.teima.drillbit.core.shapes.HeartFormation
import dev.teima.drillbit.core.shapes.KotlinLogoFormation
import dev.teima.drillbit.core.shapes.TextFormation

/** A 64-member band roster, close to a real high school marching band. */
val DEMO_ROSTER: Map<Section, Int> =
    linkedMapOf(
        Section.WOODWIND to 16,
        Section.BRASS to 16,
        Section.LOW_BRASS to 12,
        Section.PERCUSSION to 10,
        Section.GUARD to 10,
    )

/** The show that opens on first launch: block, "KOTLIN", the logo, "KMP", a heart, a circle. */
fun demoShow(): Show {
    val formations: List<Formation> =
        listOf(
            TextFormation("KOTLIN"),
            KotlinLogoFormation(),
            TextFormation("KMP", height = 28.0),
            HeartFormation(size = 52.0),
            CircleFormation(radius = 26.0),
        )
    return formations.fold(newShow("Hello, KotlinConf", DEMO_ROSTER, BlockFormation())) { show, formation ->
        val last = show.sets.lastIndex
        val positions = planMorph(show.sets[last].positions, formation, show.field).getValue(AssignmentStrategy.OPTIMAL)
        show.insertSet(last, formation.title, positions)
    }
}
