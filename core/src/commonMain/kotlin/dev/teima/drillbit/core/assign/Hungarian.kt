package dev.teima.drillbit.core.assign

/**
 * Minimum-cost perfect assignment of `n` workers to `n` jobs (the Hungarian algorithm with
 * potentials, O(n^3)).
 *
 * @param cost `cost[i][j]` is the price of giving job `j` to worker `i`. Must be square.
 * @return `result[i]` is the job given to worker `i`.
 */
fun solveAssignment(cost: Array<DoubleArray>): IntArray {
    val n = cost.size
    require(cost.all { it.size == n }) { "Cost matrix must be square" }
    if (n == 0) return IntArray(0)

    // 1-indexed arrays; index 0 is a virtual column used to start each augmenting search.
    val rowPotential = DoubleArray(n + 1)
    val colPotential = DoubleArray(n + 1)
    val rowOfCol = IntArray(n + 1)
    val previousCol = IntArray(n + 1)

    for (row in 1..n) {
        rowOfCol[0] = row
        var col0 = 0
        val minSlack = DoubleArray(n + 1) { Double.POSITIVE_INFINITY }
        val visited = BooleanArray(n + 1)
        do {
            visited[col0] = true
            val row0 = rowOfCol[col0]
            var delta = Double.POSITIVE_INFINITY
            var nextCol = 0
            for (col in 1..n) {
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
            for (col in 0..n) {
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
    for (col in 1..n) {
        result[rowOfCol[col] - 1] = col - 1
    }
    return result
}
