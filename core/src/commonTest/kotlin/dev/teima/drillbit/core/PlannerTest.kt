package dev.teima.drillbit.core

import dev.teima.drillbit.core.demo.demoShow
import dev.teima.drillbit.core.edit.optimizeAll
import dev.teima.drillbit.core.edit.planMorph
import dev.teima.drillbit.core.geometry.Vec2
import dev.teima.drillbit.core.model.Field
import dev.teima.drillbit.core.plan.AssignmentStrategy
import dev.teima.drillbit.core.plan.analyzeTransition
import dev.teima.drillbit.core.plan.assignSpots
import dev.teima.drillbit.core.plan.closestApproach
import dev.teima.drillbit.core.plan.findCollisions
import dev.teima.drillbit.core.plan.isSquaredDistanceOptimal
import dev.teima.drillbit.core.plan.minimumCounts
import dev.teima.drillbit.core.shapes.BlockFormation
import dev.teima.drillbit.core.shapes.TextFormation
import kotlin.math.sqrt
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PlannerTest {
    @Test
    fun headOnSwapMeetsInTheMiddle() {
        val approach = closestApproach(Vec2(0.0, 0.0), Vec2(10.0, 0.0), Vec2(10.0, 0.0), Vec2(0.0, 0.0))
        assertEquals(0.0, approach.distance, 1e-9)
        assertEquals(0.5, approach.time, 1e-9)
    }

    @Test
    fun parallelMarchersKeepTheirInterval() {
        val approach = closestApproach(Vec2(0.0, 0.0), Vec2(16.0, 0.0), Vec2(0.0, 2.0), Vec2(16.0, 2.0))
        assertEquals(2.0, approach.distance, 1e-9)
    }

    @Test
    fun closedFormMatchesDenseSampling() {
        val random = Random(seed = 11)
        repeat(100) {
            val points = List(4) { Vec2(random.nextDouble(0.0, 40.0), random.nextDouble(0.0, 40.0)) }
            val exact = closestApproach(points[0], points[1], points[2], points[3]).distance
            val sampled =
                (0..2000).minOf { step ->
                    val t = step / 2000.0
                    val a = points[0] + (points[1] - points[0]) * t
                    val b = points[2] + (points[3] - points[2]) * t
                    a.distanceTo(b)
                }
            assertTrue(exact <= sampled + 1e-9)
            assertEquals(sampled, exact, 0.05)
        }
    }

    @Test
    fun optimalAssignmentNeverCostsMoreThanTheAlternatives() {
        val random = Random(seed = 3)
        repeat(20) {
            val from = randomSpread(random, 30)
            val spots = randomSpread(random, 30)
            val costs =
                AssignmentStrategy.entries.associateWith { strategy ->
                    val to = assignSpots(from, spots, strategy)
                    from.indices.sumOf { from[it].distanceSquaredTo(to[it]) }
                }
            val optimal = costs.getValue(AssignmentStrategy.OPTIMAL)
            assertTrue(costs.values.all { optimal <= it + 1e-9 })
        }
    }

    @Test
    fun captPlansAreCollisionFreeWhenSpotsAreSpreadOut() {
        // CAPT theorem: optimal squared-distance assignment + synchronised straight lines is
        // collision-free when start and goal spots are at least 2 * sqrt(2) * R apart.
        val random = Random(seed = 42)
        repeat(30) {
            val from = randomSpread(random, 40)
            val spots = randomSpread(random, 40)
            val to = assignSpots(from, spots, AssignmentStrategy.OPTIMAL)
            val report = analyzeTransition(from, to, counts = 16)
            assertTrue(report.provablySafe)
            assertTrue(report.isCollisionFree, "CAPT promised no collisions but found ${report.collisions}")
        }
    }

    @Test
    fun naiveOrderCollidesWhereOptimalDoesNot() {
        val from = BlockFormation().spots(64)
        val plans = planMorph(from, TextFormation("KOTLIN"))
        val naive = findCollisions(from, plans.getValue(AssignmentStrategy.IN_ORDER))
        val optimal = findCollisions(from, plans.getValue(AssignmentStrategy.OPTIMAL))
        assertTrue(naive.isNotEmpty(), "drawing order should cross paths")
        assertTrue(optimal.isEmpty(), "optimal should not: $optimal")
    }

    @Test
    fun demoShowIsCollisionFreeAndOptimal() {
        val show = demoShow()
        for (i in 1 until show.sets.size) {
            val from = show.sets[i - 1].positions
            val to = show.sets[i].positions
            assertTrue(findCollisions(from, to).isEmpty(), "move into set $i collides")
            assertTrue(isSquaredDistanceOptimal(from, to))
            assertTrue(analyzeTransition(from, to, show.sets[i].counts).largestStride <= 8.0 / 6.0 + 1e-9)
        }
        val reoptimized = show.optimizeAll()
        for (i in show.sets.indices) {
            assertEquals(show.sets[i].positions.toSet(), reoptimized.sets[i].positions.toSet())
        }
    }

    @Test
    fun minimumCountsKeepsStrideComfortable() {
        val from = listOf(Vec2(0.0, 0.0))
        assertEquals(12, minimumCounts(from, listOf(Vec2(16.0, 0.0))))
        assertEquals(1, minimumCounts(from, from))
    }

    @Test
    fun scrambledPlanIsDetectedAsNotOptimal() {
        val from = listOf(Vec2(0.0, 0.0), Vec2(10.0, 0.0))
        assertFalse(isSquaredDistanceOptimal(from, from.reversed()))
        assertTrue(isSquaredDistanceOptimal(from, from))
    }

    /** Random spots on a jittered grid, at least 2 * sqrt(2) * 0.5 steps apart. */
    private fun randomSpread(
        random: Random,
        count: Int,
    ): List<Vec2> {
        val cell = 4.0
        val cells = (0 until 30 * 15).shuffled(random).take(count)
        val jitter = (cell - sqrt(2.0)) / 2
        return cells.map { c ->
            Vec2(
                20 + (c % 30) * cell + random.nextDouble(-jitter, jitter) / 2,
                10 + (c / 30) * cell + random.nextDouble(-jitter, jitter) / 2,
            ).let(Field.HIGH_SCHOOL::clamp)
        }
    }
}
