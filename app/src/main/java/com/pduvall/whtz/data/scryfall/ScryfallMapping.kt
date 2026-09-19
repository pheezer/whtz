package com.pduvall.whtz.data.scryfall

import com.pduvall.whtz.data.local.entity.OracleCardEntity
import com.pduvall.whtz.data.local.entity.TokenCardEntity
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Maps a Scryfall card object to our Room row. Returns null for objects with no oracle_id
 * (e.g. some reversible/token layouts) since oracle_id is our primary key and identity.
 * For DFC/split layouts the top-level image/text fields are null, so we fall back to the
 * first face.
 */
fun CardDto.toEntity(json: Json): OracleCardEntity? {
    val oid = oracleId ?: return null
    val firstFace = cardFaces?.firstOrNull()
    return OracleCardEntity(
        oracleId = oid,
        name = name,
        nameLower = name.lowercase(),
        manaCost = manaCost ?: firstFace?.manaCost,
        typeLine = typeLine ?: firstFace?.typeLine,
        oracleText = oracleText ?: firstFace?.oracleText,
        power = power ?: firstFace?.power,
        toughness = toughness ?: firstFace?.toughness,
        layout = layout,
        artCropUrl = imageUris?.artCrop ?: firstFace?.imageUris?.artCrop,
        normalImageUrl = imageUris?.normal ?: firstFace?.imageUris?.normal,
        cardFacesJson = cardFaces?.let { json.encodeToString(it) },
        printingId = id,
        rarity = rarity,
        allPartsJson = allParts?.let { json.encodeToString(it) },
    )
}

/**
 * Maps a Scryfall token object to a token row. Unlike [toEntity] this tolerates a missing
 * oracle_id (falling back to the printing id) so no token is dropped.
 */
fun CardDto.toTokenEntity(json: Json): TokenCardEntity? {
    val key = oracleId ?: id ?: return null
    val firstFace = cardFaces?.firstOrNull()
    return TokenCardEntity(
        id = key,
        name = name,
        nameLower = name.lowercase(),
        manaCost = manaCost ?: firstFace?.manaCost,
        typeLine = typeLine ?: firstFace?.typeLine,
        oracleText = oracleText ?: firstFace?.oracleText,
        power = power ?: firstFace?.power,
        toughness = toughness ?: firstFace?.toughness,
        layout = layout,
        artCropUrl = imageUris?.artCrop ?: firstFace?.imageUris?.artCrop,
        normalImageUrl = imageUris?.normal ?: firstFace?.imageUris?.normal,
        cardFacesJson = cardFaces?.let { json.encodeToString(it) },
    )
}
