package dev.teima.drillbit.core.shapes

import dev.teima.drillbit.core.geometry.Vec2

/**
 * A tiny single-line font for spelling words with people.
 *
 * Every glyph lives on a 4 x 6 grid with y pointing up (away from the audience). Strokes are
 * written as `"x,y x,y ..."` and separated by `|`. A stroke whose last point equals its first
 * is closed.
 */
object StrokeFont {
    const val GLYPH_WIDTH: Double = 4.0
    const val GLYPH_HEIGHT: Double = 6.0
    const val LETTER_GAP: Double = 1.6
    const val SPACE_WIDTH: Double = 3.0

    private val source: Map<Char, String> =
        mapOf(
            'A' to "0,0 2,6 4,0 | 1,2.4 3,2.4",
            'B' to "0,0 0,6 3,6 4,5 4,4 3,3 0.6,3 | 3,3 4,2 4,1 3,0 0.6,0",
            'C' to "4,5 3,6 1,6 0,5 0,1 1,0 3,0 4,1",
            'D' to "0,0 0,6 2.5,6 4,4.5 4,1.5 2.5,0 0,0",
            'E' to "4,6 0,6 0,0 4,0 | 0.8,3 3,3",
            'F' to "4,6 0,6 0,0 | 0.8,3 3,3",
            'G' to "4,5 3,6 1,6 0,5 0,1 1,0 3,0 4,1 4,3 2,3",
            'H' to "0,0 0,6 | 4,0 4,6 | 0.8,3 3.2,3",
            'I' to "1,6 3,6 | 2,5.2 2,0.8 | 1,0 3,0",
            'J' to "1,6 4,6 | 3,5.2 3,1 2,0 1,0 0,1",
            'K' to "0,0 0,6 | 4,6 1,3 4,0",
            'L' to "0,6 0,0 4,0",
            'M' to "0,0 0,6 2,3 4,6 4,0",
            'N' to "0,0 0,6 4,0 4,6",
            'O' to "1,0 3,0 4,1 4,5 3,6 1,6 0,5 0,1 1,0",
            'P' to "0,0 0,6 3,6 4,5 4,4 3,3 0.6,3",
            'Q' to "1,0 3,0 4,1 4,5 3,6 1,6 0,5 0,1 1,0 | 2.5,1.5 4,-0.5",
            'R' to "0,0 0,6 3,6 4,5 4,4 3,3 0.6,3 | 2,2.4 4,0",
            'S' to "4,5 3,6 1,6 0,5 0,4 1,3 3,3 4,2 4,1 3,0 1,0 0,1",
            'T' to "0,6 4,6 | 2,5.2 2,0",
            'U' to "0,6 0,1 1,0 3,0 4,1 4,6",
            'V' to "0,6 2,0 4,6",
            'W' to "0,6 1,0 2,4 3,0 4,6",
            'X' to "0,6 4,0 | 0,0 1.6,2.4 | 2.4,3.6 4,6",
            'Y' to "0,6 2,3 4,6 | 2,2.2 2,0",
            'Z' to "0,6 4,6 0,0 4,0",
            '0' to "1,0 3,0 4,1 4,5 3,6 1,6 0,5 0,1 1,0",
            '1' to "1,5 2,6 2,0.8 | 1,0 3,0",
            '2' to "0,5 1,6 3,6 4,5 4,4 0,0 4,0",
            '3' to "0,5 1,6 3,6 4,5 4,4 3,3 4,2 4,1 3,0 1,0 0,1 | 1.4,3 2.4,3",
            '4' to "3,0 3,6 0,2 4,2",
            '5' to "4,6 0,6 0,3.5 3,3.5 4,2.5 4,1 3,0 1,0 0,1",
            '6' to "4,5 3,6 1,6 0,5 0,1 1,0 3,0 4,1 4,2 3,3 0.8,3",
            '7' to "0,6 4,6 1.5,0",
            '8' to "1,3 0,4 0,5 1,6 3,6 4,5 4,4 3,3 1,3 0,2 0,1 1,0 3,0 4,1 4,2 3,3",
            '9' to "3.2,3 1,3 0,4 0,5 1,6 3,6 4,5 4,1 3,0 1,0 0,1",
            '-' to "0.5,3 3.5,3",
            '+' to "0.5,3 3.5,3 | 2,4.5 2,1.5",
        )

    private val glyphs: Map<Char, List<Stroke>> = source.mapValues { (_, text) -> parseGlyph(text) }

    /** Characters this font can draw. Anything else is drawn as a space. */
    val supported: Set<Char> = glyphs.keys + ' '

    /** The strokes for [char] in glyph units, or an empty list for spaces and unknown characters. */
    fun glyph(char: Char): List<Stroke> = glyphs[char.uppercaseChar()].orEmpty()

    /** How far the pen moves after drawing [char], in glyph units. */
    fun advance(char: Char): Double = if (glyphs.containsKey(char.uppercaseChar())) GLYPH_WIDTH else SPACE_WIDTH

    /** Width of [text] in glyph units, without a trailing gap. */
    fun width(text: String): Double {
        if (text.isEmpty()) return 0.0
        return text.sumOf { advance(it) } + LETTER_GAP * (text.length - 1)
    }

    /**
     * Lays [text] out centred on [center] with capital height [height] steps.
     * Returns the strokes in field coordinates.
     */
    fun layout(
        text: String,
        center: Vec2,
        height: Double,
    ): List<Stroke> {
        val unit = height / GLYPH_HEIGHT
        var penX = -width(text) / 2
        val strokes = mutableListOf<Stroke>()
        for (char in text) {
            val originX = penX
            glyph(char).mapTo(strokes) { stroke ->
                stroke.map { p -> Vec2(center.x + (originX + p.x) * unit, center.y + (p.y - GLYPH_HEIGHT / 2) * unit) }
            }
            penX += advance(char) + LETTER_GAP
        }
        return strokes
    }

    private fun parseGlyph(text: String): List<Stroke> =
        text.split('|').map { part ->
            val points =
                part.trim().split(' ').filter { it.isNotBlank() }.map { pair ->
                    val (x, y) = pair.split(',').map { it.toDouble() }
                    Vec2(x, y)
                }
            val closed = points.size > 2 && points.first() == points.last()
            Stroke(if (closed) points.dropLast(1) else points, closed)
        }
}
