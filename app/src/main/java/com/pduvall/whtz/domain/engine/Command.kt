package com.pduvall.whtz.domain.engine

import com.pduvall.whtz.domain.model.Zone

/** Every mutation to game state goes through one of these commands (so undo can snapshot). */
sealed interface Command {
    /** Draw from the top of the library into hand. */
    data class Draw(val count: Int = 1) : Command

    /** Mill: move the top of the library to the graveyard. */
    data class MillTop(val count: Int = 1) : Command

    /** Exile the top of the library. */
    data class ExileTop(val count: Int = 1) : Command

    /** Shuffle the library. */
    data object Shuffle : Command

    /** Move a specific card (from any zone) to the top of the library. */
    data class MoveToTop(val instanceId: String) : Command

    /** Move a specific card (from any zone) to the bottom of the library. */
    data class MoveToBottom(val instanceId: String) : Command

    /** Move a specific card to a zone (tutor to hand, discard, exile, put on battlefield, …). */
    data class MoveCard(val instanceId: String, val to: Zone) : Command

    /** Scry/surveil: reorder the top N cards of the library. The list must be exactly those top N. */
    data class ReorderTop(val orderedInstanceIds: List<String>) : Command

    /** Record that a physical copy of this oracle_id has been printed this game. */
    data class MarkPrinted(val oracleId: String) : Command

    /** Cast the commander from the command zone to the battlefield, adding +{2} commander tax. */
    data class CastCommander(val instanceId: String) : Command

    /** Wheel: discard the entire hand to the graveyard, then draw 7. */
    data object Wheel : Command
}
