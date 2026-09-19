package com.pduvall.whtz.domain.model

import kotlinx.serialization.Serializable

/**
 * One physical copy of a card in a game. [instanceId] is the zone-tracking identity (unique per
 * copy); [oracleId] is the "same card" identity used for the print-once / mark-duplicates rule.
 * Full card data (art, text) is looked up by [oracleId] in the print/UI layer.
 */
@Serializable
data class CardInstance(
    val instanceId: String,
    val oracleId: String,
    val name: String,
)
