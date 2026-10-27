package dev.teima.drillbit.core.qr

/**
 * Reed-Solomon error correction over GF(2^8) with the QR polynomial x^8 + x^4 + x^3 + x^2 + 1.
 * QR codes append these check bytes so a scuffed or partly covered code still scans.
 */
internal object ReedSolomon {
    /** Generator polynomial of the given [degree], highest coefficient dropped (it is always 1). */
    fun divisor(degree: Int): IntArray {
        require(degree in 1..MAX_DEGREE) { "Degree out of range: $degree" }
        val result = IntArray(degree)
        result[degree - 1] = 1
        var root = 1
        repeat(degree) {
            for (j in result.indices) {
                result[j] = multiply(result[j], root)
                if (j + 1 < result.size) result[j] = result[j] xor result[j + 1]
            }
            root = multiply(root, GENERATOR)
        }
        return result
    }

    /** The check bytes for [data]: the remainder of `data(x) * x^n` divided by [divisor]. */
    fun remainder(
        data: IntArray,
        divisor: IntArray,
    ): IntArray {
        val result = IntArray(divisor.size)
        for (b in data) {
            val factor = b xor result[0]
            for (i in 0 until result.size - 1) result[i] = result[i + 1]
            result[result.size - 1] = 0
            for (i in result.indices) result[i] = result[i] xor multiply(divisor[i], factor)
        }
        return result
    }

    /** Carry-less multiplication modulo the field polynomial (Russian peasant method). */
    fun multiply(
        x: Int,
        y: Int,
    ): Int {
        var z = 0
        for (i in BITS - 1 downTo 0) {
            z = (z shl 1) xor ((z ushr (BITS - 1)) * FIELD_POLYNOMIAL)
            z = z xor (((y ushr i) and 1) * x)
        }
        return z
    }

    private const val BITS = 8
    private const val FIELD_POLYNOMIAL = 0x11D
    private const val GENERATOR = 0x02
    private const val MAX_DEGREE = 255
}
