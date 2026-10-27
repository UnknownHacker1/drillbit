package dev.teima.drillbit.core.qr

import kotlin.math.abs
import kotlin.math.max

/** QR error-correction level: how much of the code can be damaged and still scan. */
enum class ErrorCorrection(
    internal val formatBits: Int,
) {
    LOW(1),
    MEDIUM(0),
    QUARTILE(3),
    HIGH(2),
}

/**
 * A QR code symbol (ISO/IEC 18004), encoded in byte mode.
 *
 * Written from the spec so the same Kotlin runs on Android, desktop and the browser with no
 * dependencies. Picks the smallest version (1..40) that fits, raises the error correction if
 * there is room left, and chooses the mask with the lowest penalty score.
 */
class QrCode private constructor(
    val version: Int,
    val errorCorrection: ErrorCorrection,
    val mask: Int,
    private val modules: Array<BooleanArray>,
) {
    /** Width and height in modules. */
    val size: Int get() = modules.size

    /** True if the module at column [x], row [y] is dark. */
    operator fun get(
        x: Int,
        y: Int,
    ): Boolean = modules[y][x]

    companion object {
        const val MIN_VERSION: Int = 1
        const val MAX_VERSION: Int = 40

        /** Encodes [text] as UTF-8. Throws [IllegalArgumentException] if it is too long for any QR code. */
        fun encodeText(
            text: String,
            minimumCorrection: ErrorCorrection = ErrorCorrection.LOW,
        ): QrCode = encodeBytes(text.encodeToByteArray(), minimumCorrection)

        fun encodeBytes(
            data: ByteArray,
            minimumCorrection: ErrorCorrection = ErrorCorrection.LOW,
        ): QrCode {
            val fits = { version: Int, correction: ErrorCorrection ->
                dataBitsNeeded(data.size, version) <= capacityBits(version, correction)
            }
            val version =
                (MIN_VERSION..MAX_VERSION).firstOrNull { fits(it, minimumCorrection) }
                    ?: throw IllegalArgumentException("${data.size} bytes is too long for a QR code")
            val correction =
                ErrorCorrection.entries
                    .filter { it.ordinal >= minimumCorrection.ordinal }
                    .last { fits(version, it) }
            val codewords = addErrorCorrection(dataCodewords(data, version, correction), version, correction)
            return QrBuilder(version, correction).build(codewords)
        }

        private fun characterCountBits(version: Int): Int = if (version <= SMALL_VERSION_LIMIT) 8 else 16

        private fun dataBitsNeeded(
            length: Int,
            version: Int,
        ): Int {
            val countBits = characterCountBits(version)
            return if (length >= 1 shl countBits) Int.MAX_VALUE else MODE_BITS + countBits + length * 8
        }

        internal fun capacityBits(
            version: Int,
            correction: ErrorCorrection,
        ): Int = dataCodewordCount(version, correction) * 8

        internal fun dataCodewordCount(
            version: Int,
            correction: ErrorCorrection,
        ): Int =
            rawDataModules(version) / 8 -
                QrTables.eccPerBlock(correction, version) * QrTables.blockCount(correction, version)

        /** Modules left for data and check bytes once all function patterns are drawn. */
        internal fun rawDataModules(version: Int): Int {
            var result = (16 * version + 128) * version + 64
            if (version >= 2) {
                val alignCount = version / 7 + 2
                result -= (25 * alignCount - 10) * alignCount - 55
                if (version >= 7) result -= VERSION_INFO_MODULES
            }
            return result
        }

        private fun dataCodewords(
            data: ByteArray,
            version: Int,
            correction: ErrorCorrection,
        ): IntArray {
            val bits = BitBuffer()
            bits.append(BYTE_MODE, MODE_BITS)
            bits.append(data.size, characterCountBits(version))
            data.forEach { bits.append(it.toInt() and BYTE_MASK, 8) }
            val capacity = capacityBits(version, correction)
            bits.append(0, minOf(TERMINATOR_BITS, capacity - bits.size))
            bits.append(0, (8 - bits.size % 8) % 8)
            var pad = PAD_A
            while (bits.size < capacity) {
                bits.append(pad, 8)
                pad = pad xor PAD_A xor PAD_B
            }
            return bits.toBytes()
        }

        /** Splits data into blocks, appends Reed-Solomon bytes to each, then interleaves them. */
        private fun addErrorCorrection(
            data: IntArray,
            version: Int,
            correction: ErrorCorrection,
        ): IntArray {
            val blockCount = QrTables.blockCount(correction, version)
            val eccLength = QrTables.eccPerBlock(correction, version)
            val rawCodewords = rawDataModules(version) / 8
            val shortBlocks = blockCount - rawCodewords % blockCount
            val shortLength = rawCodewords / blockCount
            val divisor = ReedSolomon.divisor(eccLength)

            var offset = 0
            val blocks =
                List(blockCount) { i ->
                    val dataLength = shortLength - eccLength + if (i < shortBlocks) 0 else 1
                    val chunk = data.copyOfRange(offset, offset + dataLength)
                    offset += dataLength
                    val block = IntArray(shortLength + 1)
                    chunk.copyInto(block)
                    ReedSolomon.remainder(chunk, divisor).copyInto(block, block.size - eccLength)
                    block
                }

            val result = ArrayList<Int>(rawCodewords)
            for (i in 0 until shortLength + 1) {
                for ((j, block) in blocks.withIndex()) {
                    // Short blocks have no byte at the gap position; skip it.
                    if (i != shortLength - eccLength || j >= shortBlocks) result += block[i]
                }
            }
            return result.toIntArray()
        }

        internal fun fromModules(
            version: Int,
            correction: ErrorCorrection,
            mask: Int,
            modules: Array<BooleanArray>,
        ): QrCode = QrCode(version, correction, mask, modules)

        private const val MODE_BITS = 4
        private const val BYTE_MODE = 0b0100
        private const val SMALL_VERSION_LIMIT = 9
        private const val TERMINATOR_BITS = 4
        private const val PAD_A = 0xEC
        private const val PAD_B = 0x11
        private const val BYTE_MASK = 0xFF
        private const val VERSION_INFO_MODULES = 36
    }
}

/** Draws function patterns and data onto the module grid and picks the best mask. */
private class QrBuilder(
    private val version: Int,
    private val correction: ErrorCorrection,
) {
    private val size = version * 4 + 17
    private val modules = Array(size) { BooleanArray(size) }
    private val isFunction = Array(size) { BooleanArray(size) }

    fun build(codewords: IntArray): QrCode {
        drawFunctionPatterns()
        drawCodewords(codewords)
        val bestMask =
            (0 until MASK_COUNT).minBy { mask ->
                applyMask(mask)
                drawFormatBits(mask)
                val score = penaltyScore()
                applyMask(mask)
                score
            }
        applyMask(bestMask)
        drawFormatBits(bestMask)
        return QrCode.fromModules(version, correction, bestMask, modules)
    }

    private fun set(
        x: Int,
        y: Int,
        dark: Boolean,
    ) {
        modules[y][x] = dark
        isFunction[y][x] = true
    }

    private fun drawFunctionPatterns() {
        for (i in 0 until size) {
            set(TIMING, i, i % 2 == 0)
            set(i, TIMING, i % 2 == 0)
        }
        drawFinder(FINDER_CENTER, FINDER_CENTER)
        drawFinder(size - 1 - FINDER_CENTER, FINDER_CENTER)
        drawFinder(FINDER_CENTER, size - 1 - FINDER_CENTER)

        val positions = alignmentPositions()
        val last = positions.size - 1
        for (i in positions.indices) {
            for (j in positions.indices) {
                val overlapsFinder = (i == 0 && j == 0) || (i == 0 && j == last) || (i == last && j == 0)
                if (!overlapsFinder) drawAlignment(positions[i], positions[j])
            }
        }
        drawFormatBits(0)
        drawVersionBits()
    }

    private fun drawFinder(
        cx: Int,
        cy: Int,
    ) {
        for (dy in -FINDER_REACH..FINDER_REACH) {
            for (dx in -FINDER_REACH..FINDER_REACH) {
                val x = cx + dx
                val y = cy + dy
                if (x in 0 until size && y in 0 until size) {
                    val ring = max(abs(dx), abs(dy))
                    set(x, y, ring != 2 && ring != FINDER_REACH)
                }
            }
        }
    }

    private fun drawAlignment(
        cx: Int,
        cy: Int,
    ) {
        for (dy in -2..2) {
            for (dx in -2..2) {
                set(cx + dx, cy + dy, max(abs(dx), abs(dy)) != 1)
            }
        }
    }

    private fun alignmentPositions(): IntArray {
        if (version == 1) return IntArray(0)
        val count = version / 7 + 2
        val step = (version * 8 + count * 3 + 5) / (count * 4 - 4) * 2
        val result = IntArray(count)
        result[0] = TIMING
        var position = size - 7
        for (i in count - 1 downTo 1) {
            result[i] = position
            position -= step
        }
        return result
    }

    /** 15 format bits (error correction + mask, BCH protected) in two copies around the finders. */
    private fun drawFormatBits(mask: Int) {
        val data = (correction.formatBits shl 3) or mask
        var remainder = data
        repeat(FORMAT_ECC_BITS) { remainder = (remainder shl 1) xor ((remainder ushr 9) * FORMAT_POLYNOMIAL) }
        val bits = ((data shl FORMAT_ECC_BITS) or remainder) xor FORMAT_MASK

        for (i in 0..5) set(8, i, bit(bits, i))
        set(8, 7, bit(bits, 6))
        set(8, 8, bit(bits, 7))
        set(7, 8, bit(bits, 8))
        for (i in 9 until FORMAT_BITS) set(FORMAT_BITS - 1 - i, 8, bit(bits, i))

        for (i in 0 until 8) set(size - 1 - i, 8, bit(bits, i))
        for (i in 8 until FORMAT_BITS) set(8, size - FORMAT_BITS + i, bit(bits, i))
        set(8, size - 8, true)
    }

    /** 18 version bits (versions 7+), BCH protected, in two 6 x 3 blocks. */
    private fun drawVersionBits() {
        if (version < FIRST_VERSION_WITH_INFO) return
        var remainder = version
        repeat(VERSION_ECC_BITS) { remainder = (remainder shl 1) xor ((remainder ushr 11) * VERSION_POLYNOMIAL) }
        val bits = (version shl VERSION_ECC_BITS) or remainder
        for (i in 0 until VERSION_BITS) {
            val dark = bit(bits, i)
            val a = size - 11 + i % 3
            val b = i / 3
            set(a, b, dark)
            set(b, a, dark)
        }
    }

    /** Fills data in the two-column zig-zag from the bottom-right corner, skipping function modules. */
    private fun drawCodewords(codewords: IntArray) {
        var bitIndex = 0
        val totalBits = codewords.size * 8
        var right = size - 1
        while (right >= 1) {
            if (right == TIMING) right = TIMING - 1
            for (vertical in 0 until size) {
                for (j in 0..1) {
                    val x = right - j
                    val upward = ((right + 1) and 2) == 0
                    val y = if (upward) size - 1 - vertical else vertical
                    if (!isFunction[y][x] && bitIndex < totalBits) {
                        modules[y][x] = bit(codewords[bitIndex ushr 3], 7 - (bitIndex and 7))
                        bitIndex++
                    }
                }
            }
            right -= 2
        }
    }

    /** XOR-ing twice undoes a mask, which is how candidate masks are tried. */
    private fun applyMask(mask: Int) {
        for (y in 0 until size) {
            for (x in 0 until size) {
                if (!isFunction[y][x] && maskBit(mask, x, y)) modules[y][x] = !modules[y][x]
            }
        }
    }

    private fun maskBit(
        mask: Int,
        x: Int,
        y: Int,
    ): Boolean =
        when (mask) {
            0 -> (x + y) % 2 == 0
            1 -> y % 2 == 0
            2 -> x % 3 == 0
            3 -> (x + y) % 3 == 0
            4 -> (x / 3 + y / 2) % 2 == 0
            5 -> x * y % 2 + x * y % 3 == 0
            6 -> (x * y % 2 + x * y % 3) % 2 == 0
            else -> ((x + y) % 2 + x * y % 3) % 2 == 0
        }

    /** The four penalty rules from the spec; lower is easier for scanners. */
    private fun penaltyScore(): Int {
        var score = 0
        for (line in 0 until size) {
            score += runPenalty { modules[line][it] }
            score += runPenalty { modules[it][line] }
            score += finderLikePenalty { modules[line][it] }
            score += finderLikePenalty { modules[it][line] }
        }
        for (y in 0 until size - 1) {
            for (x in 0 until size - 1) {
                val c = modules[y][x]
                if (c == modules[y][x + 1] && c == modules[y + 1][x] && c == modules[y + 1][x + 1]) score += PENALTY_BOX
            }
        }
        val dark = modules.sumOf { row -> row.count { it } }
        val total = size * size
        val k = (abs(dark * 20 - total * 10) + total - 1) / total - 1
        return score + k * PENALTY_BALANCE
    }

    private inline fun runPenalty(at: (Int) -> Boolean): Int {
        var score = 0
        var runLength = 1
        for (i in 1..size) {
            if (i < size && at(i) == at(i - 1)) {
                runLength++
            } else {
                if (runLength >= RUN_MIN) score += PENALTY_RUN + runLength - RUN_MIN
                runLength = 1
            }
        }
        return score
    }

    private inline fun finderLikePenalty(at: (Int) -> Boolean): Int {
        var score = 0
        for (start in 0..size - FINDER_LIKE.size) {
            val matchesForward = FINDER_LIKE.indices.all { at(start + it) == FINDER_LIKE[it] }
            val matchesBackward = FINDER_LIKE.indices.all { at(start + it) == FINDER_LIKE[FINDER_LIKE.size - 1 - it] }
            if (matchesForward) score += PENALTY_FINDER
            if (matchesBackward) score += PENALTY_FINDER
        }
        return score
    }

    private fun bit(
        value: Int,
        index: Int,
    ): Boolean = (value ushr index) and 1 != 0

    companion object {
        private const val MASK_COUNT = 8
        private const val TIMING = 6
        private const val FINDER_CENTER = 3
        private const val FINDER_REACH = 4
        private const val FORMAT_BITS = 15
        private const val FORMAT_ECC_BITS = 10
        private const val FORMAT_POLYNOMIAL = 0x537
        private const val FORMAT_MASK = 0x5412
        private const val FIRST_VERSION_WITH_INFO = 7
        private const val VERSION_BITS = 18
        private const val VERSION_ECC_BITS = 12
        private const val VERSION_POLYNOMIAL = 0x1F25
        private const val RUN_MIN = 5
        private const val PENALTY_RUN = 3
        private const val PENALTY_BOX = 3
        private const val PENALTY_FINDER = 40
        private const val PENALTY_BALANCE = 10

        /** Dark-light-dark-dark-dark-light-dark followed by four light modules. */
        private val FINDER_LIKE =
            booleanArrayOf(true, false, true, true, true, false, true, false, false, false, false)
    }
}

/** Appends bit fields most-significant bit first. */
private class BitBuffer {
    private val bits = ArrayList<Boolean>()
    val size: Int get() = bits.size

    fun append(
        value: Int,
        length: Int,
    ) {
        for (i in length - 1 downTo 0) bits += (value ushr i) and 1 != 0
    }

    fun toBytes(): IntArray =
        IntArray(bits.size / 8) { byteIndex ->
            (0 until 8).fold(0) { acc, i -> (acc shl 1) or if (bits[byteIndex * 8 + i]) 1 else 0 }
        }
}
