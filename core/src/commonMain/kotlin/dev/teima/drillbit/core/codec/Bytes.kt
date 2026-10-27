package dev.teima.drillbit.core.codec

/** Appends bytes, LEB128 varints and zig-zag signed varints to a growable buffer. */
internal class ByteWriter {
    private var buffer = ByteArray(INITIAL_CAPACITY)
    var size: Int = 0
        private set

    fun byte(value: Int) {
        if (size == buffer.size) buffer = buffer.copyOf(buffer.size * 2)
        buffer[size++] = value.toByte()
    }

    fun bytes(values: ByteArray) = values.forEach { byte(it.toInt()) }

    /** Unsigned LEB128: 7 bits per byte, high bit set on every byte but the last. */
    fun varint(value: Int) {
        require(value >= 0) { "varint must be non-negative, got $value" }
        var rest = value
        while (rest >= CONTINUATION) {
            byte((rest and PAYLOAD) or CONTINUATION)
            rest = rest ushr PAYLOAD_BITS
        }
        byte(rest)
    }

    /** Zig-zag maps 0, -1, 1, -2, ... to 0, 1, 2, 3, ... so small negatives stay one byte. */
    fun signedVarint(value: Int) = varint((value shl 1) xor (value shr SIGN_SHIFT))

    fun string(value: String) {
        val utf8 = value.encodeToByteArray()
        varint(utf8.size)
        bytes(utf8)
    }

    fun toByteArray(): ByteArray = buffer.copyOf(size)
}

/** Reads what [ByteWriter] wrote. Throws [InvalidShowCodeException] on truncated input. */
internal class ByteReader(
    private val data: ByteArray,
    private val end: Int = data.size,
) {
    var position: Int = 0
        private set

    val remaining: Int get() = end - position

    fun byte(): Int {
        if (position >= end) throw InvalidShowCodeException("Show code is cut short")
        return data[position++].toInt() and BYTE_MASK
    }

    fun varint(): Int {
        var result = 0
        var shift = 0
        while (true) {
            val b = byte()
            result = result or ((b and PAYLOAD) shl shift)
            if (b and CONTINUATION == 0) return result
            shift += PAYLOAD_BITS
            if (shift > MAX_SHIFT) throw InvalidShowCodeException("Number too large in show code")
        }
    }

    fun signedVarint(): Int {
        val raw = varint()
        return (raw ushr 1) xor -(raw and 1)
    }

    fun string(): String {
        val length = varint()
        if (length > remaining) throw InvalidShowCodeException("Text runs past the end of the show code")
        val text = data.decodeToString(position, position + length)
        position += length
        return text
    }
}

/** CRC-32 (IEEE 802.3, the one zip and PNG use), so a mistyped or truncated code is rejected. */
internal object Crc32 {
    private val table =
        IntArray(TABLE_SIZE) { n ->
            var c = n
            repeat(BITS_PER_BYTE) { c = if (c and 1 != 0) POLYNOMIAL xor (c ushr 1) else c ushr 1 }
            c
        }

    fun of(
        data: ByteArray,
        length: Int = data.size,
    ): Int {
        var crc = -1
        for (i in 0 until length) {
            crc = table[(crc xor data[i].toInt()) and BYTE_MASK] xor (crc ushr BITS_PER_BYTE)
        }
        return crc.inv()
    }
}

/** A show code that cannot be decoded: mistyped, truncated or from a newer app version. */
class InvalidShowCodeException(
    message: String,
) : IllegalArgumentException(message)

private const val INITIAL_CAPACITY = 256
private const val CONTINUATION = 0x80
private const val PAYLOAD = 0x7F
private const val PAYLOAD_BITS = 7
private const val MAX_SHIFT = 28
private const val SIGN_SHIFT = 31
private const val BYTE_MASK = 0xFF
private const val TABLE_SIZE = 256
private const val BITS_PER_BYTE = 8
private const val POLYNOMIAL = 0xEDB88320.toInt()
