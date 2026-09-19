package com.pduvall.whtz.data.tokens

import com.pduvall.whtz.data.local.dao.OracleCardDao
import com.pduvall.whtz.data.local.dao.TokenCardDao
import com.pduvall.whtz.data.local.entity.TokenCardEntity
import com.pduvall.whtz.data.scryfall.RelatedCardDto
import com.pduvall.whtz.data.scryfall.ScryfallApi
import com.pduvall.whtz.data.scryfall.toTokenEntity
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/** Offline token lookups (bundled in the `tokens` table), plus the tokens a given card creates. */
@Singleton
class TokenRepository @Inject constructor(
    private val tokenDao: TokenCardDao,
    private val oracleCardDao: OracleCardDao,
    private val api: ScryfallApi,
    private val json: Json,
) {
    suspend fun tokenCount(): Int = tokenDao.count()

    /** Local, offline token search. A blank query browses alphabetically. */
    suspend fun search(query: String): List<TokenCardEntity> {
        val q = query.trim().lowercase()
        return if (q.isEmpty()) tokenDao.browse() else tokenDao.search(q)
    }

    /**
     * The tokens a card creates, from its stored `all_parts`. Each token part is resolved to a
     * printable token — preferring the offline library (by name), falling back to a network fetch
     * of the part's uri — so it works offline once the token library is bundled.
     */
    suspend fun tokensCreatedBy(oracleId: String): List<TokenCardEntity> {
        val card = oracleCardDao.getByOracleId(oracleId) ?: return emptyList()
        val parts = card.allPartsJson?.let {
            runCatching { json.decodeFromString<List<RelatedCardDto>>(it) }.getOrNull()
        }.orEmpty().filter { it.component == "token" }
        val result = LinkedHashMap<String, TokenCardEntity>()
        for (part in parts) {
            val token = tokenDao.getByName(part.name.lowercase())
                ?: part.uri?.let { api.cardByUri(it)?.toTokenEntity(json) }
            if (token != null) result[token.id] = token
        }
        return result.values.toList()
    }
}
