package com.pduvall.whtz.data.scryfall

import com.pduvall.whtz.data.local.dao.OracleCardDao
import com.pduvall.whtz.data.local.entity.OracleCardEntity
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Resolves card names to local rows, falling back to the Scryfall API for names missing from
 * the bulk import (and caching the result). Network calls go through a small semaphore so batch
 * resolution (deck import in M2) stays under Scryfall's ~10 req/s limit.
 */
@Singleton
class CardRepository @Inject constructor(
    private val dao: OracleCardDao,
    private val api: ScryfallApi,
    private val json: Json,
) {
    private val networkLimiter = Semaphore(permits = MAX_CONCURRENT_REQUESTS)

    suspend fun cardCount(): Int = dao.count()

    /** Local-first exact-name resolution with a network fallback. */
    suspend fun resolve(name: String): OracleCardEntity? {
        val key = name.trim().lowercase()
        if (key.isEmpty()) return null
        dao.findByExactName(key)?.let { return it }

        val dto = networkLimiter.withPermit { api.cardByExactName(name.trim()) } ?: return null
        val entity = dto.toEntity(json) ?: return null
        dao.upsertAll(listOf(entity))
        return entity
    }

    companion object {
        private const val MAX_CONCURRENT_REQUESTS = 8
    }
}
