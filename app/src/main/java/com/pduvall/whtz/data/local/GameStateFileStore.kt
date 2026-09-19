package com.pduvall.whtz.data.local

import android.content.Context
import com.pduvall.whtz.domain.model.GameState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persists the current game to a file (not a Room row). The serialized state can be large — a
 * big deck yields thousands of card instances — and SQLite's ~2MB CursorWindow throws
 * SQLiteBlobTooBigException when reading such a row back. A file has no such limit.
 */
@Singleton
class GameStateFileStore @Inject constructor(
    @ApplicationContext context: Context,
    private val json: Json,
) {
    private val file = File(context.filesDir, "game_state.json")
    private val prefs = context.getSharedPreferences("game_meta", Context.MODE_PRIVATE)

    // Large libraries serialize to several MB; serialize writes so rapid actions can't
    // interleave and corrupt the file.
    private val writeMutex = Mutex()

    data class Saved(val state: GameState, val deckId: Long?, val deckName: String)

    suspend fun save(state: GameState, deckId: Long?, deckName: String) = withContext(Dispatchers.IO) {
        writeMutex.withLock {
            file.writeText(json.encodeToString(state))
            prefs.edit().apply {
                putString(KEY_NAME, deckName)
                if (deckId != null) putLong(KEY_ID, deckId) else remove(KEY_ID)
            }.apply()
        }
    }

    suspend fun load(): Saved? = withContext(Dispatchers.IO) {
        if (!file.exists()) return@withContext null
        val state = runCatching { json.decodeFromString<GameState>(file.readText()) }.getOrNull()
            ?: return@withContext null
        val deckId = if (prefs.contains(KEY_ID)) prefs.getLong(KEY_ID, -1L).takeIf { it >= 0 } else null
        Saved(state, deckId, prefs.getString(KEY_NAME, "").orEmpty())
    }

    suspend fun hasSaved(): Boolean = withContext(Dispatchers.IO) { file.exists() }

    suspend fun clear() = withContext(Dispatchers.IO) {
        file.delete()
        prefs.edit().clear().apply()
    }

    private companion object {
        const val KEY_NAME = "deckName"
        const val KEY_ID = "deckId"
    }
}
