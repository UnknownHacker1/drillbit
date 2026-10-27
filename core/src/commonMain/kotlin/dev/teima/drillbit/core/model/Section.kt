package dev.teima.drillbit.core.model

/** The ensemble section a performer belongs to. The [code] prefixes their label, like "T3". */
enum class Section(
    val code: Char,
    val displayName: String,
) {
    WOODWIND('W', "Woodwinds"),
    BRASS('B', "Brass"),
    LOW_BRASS('L', "Low brass"),
    PERCUSSION('P', "Percussion"),
    GUARD('G', "Color guard"),
    ;

    companion object {
        fun fromCode(code: Char): Section =
            entries.firstOrNull { it.code == code } ?: throw IllegalArgumentException("Unknown section code '$code'")
    }
}
