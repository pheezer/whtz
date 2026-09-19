package com.pduvall.whtz.data.printer

/**
 * Thermal printers have no mana-symbol glyphs, so we render Scryfall's {W}{U}{2}{T} tokens as
 * plain ASCII by stripping the braces: {W}->W, {2}->2, {2/W}->2/W, {T}->T, {W/P}->W/P.
 */
object ManaText {
    private val TOKEN = Regex("""\{([^}]*)\}""")

    fun toAscii(text: String?): String =
        text?.replace(TOKEN) { it.groupValues[1] } ?: ""
}
