package com.pduvall.whtz.data.deck

enum class DeckSection { MAINBOARD, SIDEBOARD, COMMANDER, COMPANION, MAYBEBOARD }

/** One parsed decklist line: a quantity of a named card, optionally pinned to a printing. */
data class DeckEntry(
    val quantity: Int,
    val name: String,
    val setCode: String? = null,
    val collectorNumber: String? = null,
    val section: DeckSection = DeckSection.MAINBOARD,
)

data class DecklistParseResult(
    val entries: List<DeckEntry>,
    /** Non-empty lines that could not be parsed as a card at all. */
    val unparseableLines: List<String>,
)

/**
 * Parses plaintext decklists across the common exports (MTG Arena, MTGO, Moxfield, Archidekt).
 * Handles: quantities (`4`, `4x`), optional `(SET) 123` printing suffixes, foil/etched markers
 * (`*F*`, `*E*`, `(foil)`), section headers (Deck/Sideboard/Commander/…) and blank-line breaks,
 * `SB:` prefixes, `//` and `#` comments, and `//` inside double-faced card names.
 */
object DecklistParser {

    // qty? name (SET)? collector#?
    private val LINE = Regex("""^(?:(\d+)\s*[xX]?\s+)?(.+?)(?:\s+\(([^)]+)\)(?:\s+(\S+))?)?$""")
    private val FOIL = Regex("""\s*(?:\*[fFeE]\*|\((?:foil|etched)\))\s*$""", RegexOption.IGNORE_CASE)

    private val HEADERS = mapOf(
        "deck" to DeckSection.MAINBOARD,
        "mainboard" to DeckSection.MAINBOARD,
        "main" to DeckSection.MAINBOARD,
        "sideboard" to DeckSection.SIDEBOARD,
        "commander" to DeckSection.COMMANDER,
        "companion" to DeckSection.COMPANION,
        "maybeboard" to DeckSection.MAYBEBOARD,
    )

    // Lines that are metadata, not cards (e.g. Arena's "About"/"Name ..." block).
    private val SKIP_LINES = setOf("about")
    private val SKIP_PREFIXES = listOf("name ")

    fun parse(text: String): DecklistParseResult {
        val entries = mutableListOf<DeckEntry>()
        val unparseable = mutableListOf<String>()
        var section = DeckSection.MAINBOARD

        for (raw in text.lineSequence()) {
            var line = raw.trim().replace('’', '\'')
            if (line.isEmpty()) continue
            if (line.startsWith("//") || line.startsWith("#")) continue

            val lower = line.lowercase()
            if (lower in SKIP_LINES || SKIP_PREFIXES.any { lower.startsWith(it) }) continue

            HEADERS[line.trimEnd(':').trim().lowercase()]?.let {
                section = it
                continue
            }

            var entrySection = section
            if (lower.startsWith("sb:")) {
                entrySection = DeckSection.SIDEBOARD
                line = line.substring(3).trim()
            }

            line = FOIL.replace(line, "").trim()
            if (line.isEmpty()) continue

            val m = LINE.matchEntire(line)
            if (m == null) {
                unparseable.add(raw)
                continue
            }
            val qty = m.groupValues[1].toIntOrNull() ?: 1
            val name = m.groupValues[2].trim()
            val set = m.groupValues[3].takeIf { it.isNotBlank() }
            val collector = m.groupValues[4].takeIf { it.isNotBlank() }
            if (name.isEmpty()) {
                unparseable.add(raw)
                continue
            }
            entries.add(DeckEntry(qty, name, set, collector, entrySection))
        }
        return DecklistParseResult(entries, unparseable)
    }
}
