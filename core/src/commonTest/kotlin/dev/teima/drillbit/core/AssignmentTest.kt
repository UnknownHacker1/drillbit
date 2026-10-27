package dev.teima.drillbit.core

import dev.teima.drillbit.core.assign.solveAssignment
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AssignmentTest {
    @Test
    fun solvesTextbookExample() {
        val cost =
            arrayOf(
                doubleArrayOf(4.0, 1.0, 3.0),
                doubleArrayOf(2.0, 0.0, 5.0),
                doubleArrayOf(3.0, 2.0, 2.0),
            )
        val result = solveAssignment(cost)
        assertEquals(5.0, totalCost(cost, result))
    }

    @Test
    fun matchesBruteForceOnRandomMatrices() {
        val random = Random(seed = 2027)
        repeat(200) {
            val n = random.nextInt(1, 7)
            val cost = Array(n) { DoubleArray(n) { random.nextInt(0, 50).toDouble() } }
            val result = solveAssignment(cost)
            assertEquals(n, result.toSet().size, "every job is used once")
            assertEquals(bruteForce(cost), totalCost(cost, result), 1e-9)
        }
    }

    @Test
    fun handlesEmptyAndLargeInputs() {
        assertEquals(0, solveAssignment(emptyArray()).size)
        val random = Random(seed = 7)
        val n = 200
        val cost = Array(n) { DoubleArray(n) { random.nextDouble() } }
        val result = solveAssignment(cost)
        assertEquals(n, result.toSet().size)
        assertTrue(totalCost(cost, result) < n * 0.1, "optimal cost of a random 200x200 matrix is small")
    }

    private fun totalCost(
        cost: Array<DoubleArray>,
        assignment: IntArray,
    ): Double = assignment.indices.sumOf { cost[it][assignment[it]] }

    private fun bruteForce(cost: Array<DoubleArray>): Double {
        val n = cost.size
        var best = Double.POSITIVE_INFINITY

        fun search(
            row: Int,
            used: BooleanArray,
            sum: Double,
        ) {
            if (sum >= best) return
            if (row == n) {
                best = sum
                return
            }
            for (col in 0 until n) {
                if (!used[col]) {
                    used[col] = true
                    search(row + 1, used, sum + cost[row][col])
                    used[col] = false
                }
            }
        }
        search(0, BooleanArray(n), 0.0)
        return best
    }
}
