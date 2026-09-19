package com.pduvall.whtz.data.scryfall

import com.pduvall.whtz.data.local.dao.OracleCardDao
import com.pduvall.whtz.data.local.dao.RulingDao
import com.pduvall.whtz.data.local.dao.TokenCardDao
import com.pduvall.whtz.data.local.entity.OracleCardEntity
import com.pduvall.whtz.data.local.entity.RulingEntity
import com.pduvall.whtz.data.local.entity.TokenCardEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.DecodeSequenceMode
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeToSequence
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.zip.GZIPInputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Populates the offline database from Scryfall in three phases:
 *  1. **cards** — the "Oracle Cards" bulk file (gzipped JSONL, streamed and upserted in chunks so a
 *     low-RAM device never materializes the whole list);
 *  2. **rulings** — the "rulings" bulk file (same gzipped-JSONL streaming), fully replacing the table;
 *  3. **tokens** — paginating `/cards/search?q=is:token` and upserting each page.
 * We request identity encoding and gunzip the bulk files ourselves since they're static .gz files.
 */
@Singleton
class BulkImporter @Inject constructor(
    private val api: ScryfallApi,
    private val client: OkHttpClient,
    private val json: Json,
    private val dao: OracleCardDao,
    private val tokenDao: TokenCardDao,
    private val rulingDao: RulingDao,
) {
    /** Progress for the currently running [phase] ("cards" / "rulings" / "tokens"). */
    data class Progress(val phase: String, val imported: Int)

    @OptIn(ExperimentalSerializationApi::class)
    fun import(): Flow<Progress> = flow {
        val index = api.bulkDataIndex()
        importCards(index)
        importRulings(index)
        importTokens()
    }.flowOn(Dispatchers.IO)

    @OptIn(ExperimentalSerializationApi::class)
    private suspend fun FlowCollector<Progress>.importCards(index: BulkDataListDto) {
        val entry = index.data.firstOrNull { it.type == "oracle_cards" }
            ?: error("Scryfall bulk-data has no 'oracle_cards' entry")
        gunzipJsonl(entry.jsonlDownloadUri) { seq: Sequence<CardDto> ->
            val buffer = ArrayList<OracleCardEntity>(CHUNK)
            var imported = 0
            for (dto in seq) {
                val entity = dto.toEntity(json) ?: continue
                buffer.add(entity)
                if (buffer.size >= CHUNK) {
                    dao.upsertAll(buffer)
                    imported += buffer.size
                    buffer.clear()
                    emit(Progress("cards", imported))
                }
            }
            if (buffer.isNotEmpty()) {
                dao.upsertAll(buffer)
                imported += buffer.size
                emit(Progress("cards", imported))
            }
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    private suspend fun FlowCollector<Progress>.importRulings(index: BulkDataListDto) {
        val entry = index.data.firstOrNull { it.type == "rulings" } ?: return
        rulingDao.deleteAll()
        gunzipJsonl(entry.jsonlDownloadUri) { seq: Sequence<RulingDto> ->
            val buffer = ArrayList<RulingEntity>(CHUNK)
            var imported = 0
            for (dto in seq) {
                val oracleId = dto.oracleId ?: continue
                buffer.add(
                    RulingEntity(
                        oracleId = oracleId,
                        source = dto.source,
                        publishedAt = dto.publishedAt,
                        comment = dto.comment,
                    ),
                )
                if (buffer.size >= CHUNK) {
                    rulingDao.insertAll(buffer)
                    imported += buffer.size
                    buffer.clear()
                    emit(Progress("rulings", imported))
                }
            }
            if (buffer.isNotEmpty()) {
                rulingDao.insertAll(buffer)
                imported += buffer.size
                emit(Progress("rulings", imported))
            }
        }
    }

    private suspend fun FlowCollector<Progress>.importTokens() {
        var url: String? = api.tokenSearchUrl()
        var imported = 0
        while (url != null) {
            val page = api.cardList(url)
            val entities = page.data.mapNotNull { it.toTokenEntity(json) }
            if (entities.isNotEmpty()) {
                tokenDao.upsertAll(entities)
                imported += entities.size
                emit(Progress("tokens", imported))
            }
            url = if (page.hasMore) page.nextPage else null
            if (url != null) delay(RATE_LIMIT_MS) // stay under Scryfall's request-rate ceiling
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    private inline fun <reified T> gunzipJsonl(url: String, consume: (Sequence<T>) -> Unit) {
        val req = Request.Builder()
            .url(url)
            .header("Accept-Encoding", "identity")
            .build()
        client.newCall(req).execute().use { resp ->
            require(resp.isSuccessful) { "bulk download HTTP ${resp.code}" }
            GZIPInputStream(resp.body.byteStream()).use { gzip ->
                consume(json.decodeToSequence(gzip, DecodeSequenceMode.WHITESPACE_SEPARATED))
            }
        }
    }

    companion object {
        private const val CHUNK = 500
        private const val RATE_LIMIT_MS = 120L
    }
}
