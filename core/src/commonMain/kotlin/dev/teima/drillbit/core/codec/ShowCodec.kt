package dev.teima.drillbit.core.codec

import dev.teima.drillbit.core.geometry.Vec2
import dev.teima.drillbit.core.model.DrillSet
import dev.teima.drillbit.core.model.Field
import dev.teima.drillbit.core.model.Performer
import dev.teima.drillbit.core.model.Section
import dev.teima.drillbit.core.model.Show
import kotlin.io.encoding.Base64
import kotlin.math.roundToInt

/**
 * Packs a whole [Show] into a short URL-safe string, so it fits in a link or a QR code and
 * needs no server.
 *
 * Layout (version 1):
 * ```
 * "DB" version:u8 title:str tempo:varint field:u8
 * performers:varint { section:u8 number:varint }*
 * sets:varint { name:str counts:varint { dx:zigzag dy:zigzag }* }*
 * crc32:u32
 * ```
 * Positions are stored in quarter steps. The first set is delta-coded against the previous
 * performer, later sets against the same performer in the previous set, so a performer who
 * holds still costs two bytes.
 */
object ShowCodec {
    const val VERSION: Int = 1

    private val base64 = Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT_OPTIONAL)

    fun encode(show: Show): String = base64.encode(encodeBytes(show))

    fun decode(code: String): Show {
        val bytes =
            try {
                base64.decode(code.trim())
            } catch (e: IllegalArgumentException) {
                throw InvalidShowCodeException("Not a show code: ${e.message}")
            }
        return decodeBytes(bytes)
    }

    internal fun encodeBytes(show: Show): ByteArray {
        val out = ByteWriter()
        out.byte(MAGIC_0)
        out.byte(MAGIC_1)
        out.byte(VERSION)
        out.string(show.title)
        out.varint(show.tempoBpm)
        out.byte(show.field.ordinal)
        out.varint(show.performers.size)
        show.performers.forEach {
            out.byte(it.section.code.code)
            out.varint(it.number)
        }
        out.varint(show.sets.size)
        var previous: List<Pair<Int, Int>>? = null
        for (set in show.sets) {
            out.string(set.name)
            out.varint(set.counts)
            val quantized = set.positions.map { quantize(it) }
            quantized.forEachIndexed { i, (qx, qy) ->
                val (bx, by) = previous?.get(i) ?: quantized.getOrElse(i - 1) { 0 to 0 }
                out.signedVarint(qx - bx)
                out.signedVarint(qy - by)
            }
            previous = quantized
        }
        val body = out.toByteArray()
        val crc = Crc32.of(body)
        for (shift in CRC_SHIFTS) out.byte(crc ushr shift)
        return out.toByteArray()
    }

    internal fun decodeBytes(bytes: ByteArray): Show {
        if (bytes.size < HEADER_SIZE + CRC_SIZE) throw InvalidShowCodeException("Show code is too short")
        val bodyEnd = bytes.size - CRC_SIZE
        val storedCrc =
            CRC_SHIFTS.foldIndexed(0) { i, acc, shift ->
                val byte = bytes[bodyEnd + i].toInt() and BYTE
                acc or (byte shl shift)
            }
        if (storedCrc != Crc32.of(bytes, bodyEnd)) {
            throw InvalidShowCodeException("Show code is damaged (checksum mismatch)")
        }

        val input = ByteReader(bytes, bodyEnd)
        val hasMagic = input.byte() == MAGIC_0 && input.byte() == MAGIC_1
        if (!hasMagic) throw InvalidShowCodeException("Not a Drillbit show code")
        val version = input.byte()
        if (version != VERSION) throw InvalidShowCodeException("Show code version $version needs a newer Drillbit")

        val title = input.string()
        val tempo = input.varint()
        val field = Field.entries.getOrNull(input.byte()) ?: throw InvalidShowCodeException("Unknown field type")
        val performers =
            List(input.varint()) {
                val section =
                    try {
                        Section.fromCode(input.byte().toChar())
                    } catch (e: IllegalArgumentException) {
                        throw InvalidShowCodeException(e.message ?: "Unknown section")
                    }
                Performer(section, input.varint())
            }
        var previous: List<Pair<Int, Int>>? = null
        val sets =
            List(input.varint()) {
                val name = input.string()
                val counts = input.varint()
                val quantized = mutableListOf<Pair<Int, Int>>()
                repeat(performers.size) { i ->
                    val (bx, by) = previous?.get(i) ?: quantized.getOrElse(i - 1) { 0 to 0 }
                    quantized += (bx + input.signedVarint()) to (by + input.signedVarint())
                }
                previous = quantized
                DrillSet(
                    name,
                    counts.coerceAtLeast(1),
                    quantized.map { (qx, qy) ->
                        Vec2(qx / QUARTERS, qy / QUARTERS)
                    },
                )
            }
        return try {
            Show(title, tempo, field, performers, sets)
        } catch (e: IllegalArgumentException) {
            throw InvalidShowCodeException(e.message ?: "Show code describes an invalid show")
        }
    }

    private fun quantize(p: Vec2): Pair<Int, Int> = (p.x * QUARTERS).roundToInt() to (p.y * QUARTERS).roundToInt()
}

/** Builds the link a performer opens to load [code] in the web app. */
fun shareLink(
    baseUrl: String,
    code: String,
): String = "${baseUrl.substringBefore('#')}#$SHOW_PARAM=$code"

/**
 * Pulls the show code out of whatever the user pasted: a bare code, or any link containing
 * `#show=` / `?show=`. Returns null when there is nothing that looks like a code.
 */
fun extractShowCode(text: String): String? {
    val trimmed = text.trim()
    if (trimmed.isEmpty()) return null
    val marker = "$SHOW_PARAM="
    val index = trimmed.indexOf(marker)
    val candidate = if (index >= 0) trimmed.substring(index + marker.length).substringBefore('&') else trimmed
    return candidate.takeIf { it.isNotEmpty() && it.all { c -> c.isLetterOrDigit() || c == '-' || c == '_' } }
}

private const val SHOW_PARAM = "show"
private const val MAGIC_0 = 'D'.code
private const val MAGIC_1 = 'B'.code
private const val HEADER_SIZE = 3
private const val CRC_SIZE = 4
private const val BYTE = 0xFF
private const val QUARTERS = Field.GRID
private val CRC_SHIFTS = intArrayOf(24, 16, 8, 0)
