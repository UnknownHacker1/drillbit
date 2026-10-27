package dev.teima.drillbit.core.assign

/**
 * Minimum-cost assignment of `n` workers to `m >= n` jobs, every worker getting a different job
 * (the Hungarian algorithm with potentials, O(n^2 * m)).
 *
 * A rectangular matrix with more jobs than workers is fine: the spare jobs are left unused.
 * With more workers than jobs there is no valid assignment, so that is rejected; pad the
 * matrix with dummy jobs if you want some workers left out.
 *
 * @param cost `cost[i][j]` is the price of giving job `j` to worker `i`. Every row must have
 *   the same length and every entry must be finite (NaN or infinity would never converge).
 * @return `result[i]` is the job given to worker `i`.
 */
fun solveAssignment(cost: Array<DoubleArray>): IntArray {
    val n = cost.size
    if (n == 0) return IntArray(0)
    val m = cost[0].size
    require(cost.all { it.size == m }) { "Every row of the cost matrix must have the same length" }
    require(n <= m) { "Cannot assign $n workers to only $m jobs" }
    require(cost.all { row -> row.all { it.isFinite() } }) { "Costs must be finite numbers (no NaN or infinity)" }

    // 1-indexed arrays; index 0 is a virtual column used to start each augmenting search.
    val rowPotential = DoubleArray(n + 1)
    val colPotential = DoubleArray(m + 1)
    val rowOfCol = IntArray(m + 1)
    val previousCol = IntArray(m + 1)

    for (row in 1..n) {
        rowOfCol[0] = row
        var col0 = 0
        val minSlack = DoubleArray(m + 1) { Double.POSITIVE_INFINITY }
        val visited = BooleanArray(m + 1)
        do {
            visited[col0] = true
            val row0 = rowOfCol[col0]
            var delta = Double.POSITIVE_INFINITY
            var nextCol = 0
            for (col in 1..m) {
                if (visited[col]) continue
                val slack = cost[row0 - 1][col - 1] - rowPotential[row0] - colPotential[col]
                if (slack < minSlack[col]) {
                    minSlack[col] = slack
                    previousCol[col] = col0
                }
                if (minSlack[col] < delta) {
                    delta = minSlack[col]
                    nextCol = col
                }
            }
            for (col in 0..m) {
                if (visited[col]) {
                    rowPotential[rowOfCol[col]] += delta
                    colPotential[col] -= delta
                } else {
                    minSlack[col] -= delta
                }
            }
            col0 = nextCol
        } while (rowOfCol[col0] != 0)

        // Walk the alternating path back, flipping matched edges.
        do {
            val col1 = previousCol[col0]
            rowOfCol[col0] = rowOfCol[col1]
            col0 = col1
        } while (col0 != 0)
    }

    val result = IntArray(n)
    for (col in 1..m) {
        if (rowOfCol[col] != 0) result[rowOfCol[col] - 1] = col - 1
    }
    return result
}
