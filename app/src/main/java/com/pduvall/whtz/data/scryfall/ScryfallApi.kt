package com.pduvall.whtz.data.scryfall

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Thin Scryfall client. The required User-Agent / Accept headers are added globally by an
 * OkHttp interceptor (see NetworkModule). Bulk-file streaming lives in BulkImporter so the
 * response body can be consumed lazily.
 */
@Singleton
class ScryfallApi @Inject constructor(
    private val client: OkHttpClient,
    private val json: Json,
) {
    suspend fun bulkDataIndex(): BulkDataListDto = withContext(Dispatchers.IO) {
        val req = Request.Builder().url("$BASE/bulk-data").build()
        client.newCall(req).execute().use { resp ->
            require(resp.isSuccessful) { "bulk-data HTTP ${resp.code}" }
            json.decodeFromString<BulkDataListDto>(resp.body.string())
        }
    }

    /** Exact-name lookup. Returns null on 404 (unknown name). */
    suspend fun cardByExactName(name: String): CardDto? = withContext(Dispatchers.IO) {
        val url = "$BASE/cards/named".toHttpUrl().newBuilder()
            .addQueryParameter("exact", name)
            .build()
        val req = Request.Builder().url(url).build()
        client.newCall(req).execute().use { resp ->
            when {
                resp.isSuccessful -> json.decodeFromString<CardDto>(resp.body.string())
                resp.code == 404 -> null
                else -> error("cards/named HTTP ${resp.code}")
            }
        }
    }

    /** GET a card list page by full URL (used to paginate /cards/search via next_page). */
    suspend fun cardList(url: String): CardListDto = withContext(Dispatchers.IO) {
        val req = Request.Builder().url(url).build()
        client.newCall(req).execute().use { resp ->
            when {
                resp.isSuccessful -> json.decodeFromString<CardListDto>(resp.body.string())
                // A search that matches nothing returns 404 with an error object.
                resp.code == 404 -> CardListDto()
                else -> error("cards/search HTTP ${resp.code}")
            }
        }
    }

    /** GET a single card by its Scryfall API URI (e.g. an all_parts token link). */
    suspend fun cardByUri(uri: String): CardDto? = withContext(Dispatchers.IO) {
        val req = Request.Builder().url(uri).build()
        client.newCall(req).execute().use { resp ->
            when {
                resp.isSuccessful -> json.decodeFromString<CardDto>(resp.body.string())
                resp.code == 404 -> null
                else -> error("cards/{id} HTTP ${resp.code}")
            }
        }
    }

    /** Builds the first-page URL for a token search (`is:token`, one row per token oracle). */
    fun tokenSearchUrl(): String =
        "$BASE/cards/search".toHttpUrl().newBuilder()
            .addQueryParameter("q", "is:token")
            .addQueryParameter("unique", "oracle")
            .addQueryParameter("order", "name")
            .build()
            .toString()

    companion object {
        const val BASE = "https://api.scryfall.com"
    }
}
