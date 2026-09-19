package com.pduvall.whtz.data.local.entity

/**
 * The printable fields [com.pduvall.whtz.data.printer.CardRasterizer] needs to render a card image.
 * Implemented by both real cards ([OracleCardEntity]) and [TokenCardEntity] so either can be printed.
 */
interface PrintableCard {
    val name: String
    val manaCost: String?
    val typeLine: String?
    val oracleText: String?
    val power: String?
    val toughness: String?
    val artCropUrl: String?
    val cardFacesJson: String?
}
