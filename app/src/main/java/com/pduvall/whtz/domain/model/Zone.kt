package com.pduvall.whtz.domain.model

import kotlinx.serialization.Serializable

/** The zones we track for a single player. Library order matters (index 0 = top). */
@Serializable
enum class Zone {
    LIBRARY,
    HAND,
    GRAVEYARD,
    EXILE,
    BATTLEFIELD,
    COMMAND,
}
