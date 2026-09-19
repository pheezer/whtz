package com.pduvall.whtz.data.scryfall

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Response of GET /bulk-data (the index of downloadable bulk files). */
@Serializable
data class BulkDataListDto(
    val data: List<BulkDataEntryDto> = emptyList(),
)

@Serializable
data class BulkDataEntryDto(
    val type: String,
    // Scryfall serves bulk data as gzipped JSONL under this field.
    @SerialName("jsonl_download_uri") val jsonlDownloadUri: String,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("compressed_size") val compressedSize: Long? = null,
)

@Serializable
data class ImageUrisDto(
    @SerialName("art_crop") val artCrop: String? = null,
    val normal: String? = null,
    val large: String? = null,
    val png: String? = null,
)

@Serializable
data class CardFaceDto(
    val name: String? = null,
    @SerialName("mana_cost") val manaCost: String? = null,
    @SerialName("type_line") val typeLine: String? = null,
    @SerialName("oracle_text") val oracleText: String? = null,
    val power: String? = null,
    val toughness: String? = null,
    @SerialName("image_uris") val imageUris: ImageUrisDto? = null,
)

/** An entry in a card's `all_parts` — links to related cards (tokens, combo pieces, melds). */
@Serializable
data class RelatedCardDto(
    val component: String? = null,
    val name: String = "",
    @SerialName("type_line") val typeLine: String? = null,
    val uri: String? = null,
)

/** A Scryfall card object (subset of fields we use). */
@Serializable
data class CardDto(
    val id: String? = null,
    @SerialName("oracle_id") val oracleId: String? = null,
    val name: String = "",
    @SerialName("mana_cost") val manaCost: String? = null,
    @SerialName("type_line") val typeLine: String? = null,
    @SerialName("oracle_text") val oracleText: String? = null,
    val power: String? = null,
    val toughness: String? = null,
    val layout: String = "normal",
    @SerialName("image_uris") val imageUris: ImageUrisDto? = null,
    @SerialName("card_faces") val cardFaces: List<CardFaceDto>? = null,
    val rarity: String? = null,
    @SerialName("all_parts") val allParts: List<RelatedCardDto>? = null,
)

/** Response of GET /cards/search (and other list endpoints), paginated via `next_page`. */
@Serializable
data class CardListDto(
    val data: List<CardDto> = emptyList(),
    @SerialName("has_more") val hasMore: Boolean = false,
    @SerialName("next_page") val nextPage: String? = null,
    @SerialName("total_cards") val totalCards: Int? = null,
)

/** A single ruling (from the "rulings" bulk file), keyed by the card's oracle_id. */
@Serializable
data class RulingDto(
    @SerialName("oracle_id") val oracleId: String? = null,
    val source: String? = null,
    @SerialName("published_at") val publishedAt: String? = null,
    val comment: String = "",
)
