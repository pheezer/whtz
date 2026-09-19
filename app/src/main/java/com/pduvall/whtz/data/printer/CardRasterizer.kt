package com.pduvall.whtz.data.printer

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.pduvall.whtz.data.local.entity.PrintableCard
import com.pduvall.whtz.data.scryfall.CardFaceDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min

/**
 * Turns a card into a printable job. Each [Section] is one face rendered as a single bordered
 * image — dithered (halftone) art above crisp black text, framed by a black rectangle — split into
 * ≤256px-tall segments for DantSu. Single-faced cards produce one section; double-faced / split
 * cards produce one per face so both print back to back.
 */
@Singleton
class CardRasterizer @Inject constructor(
    private val client: OkHttpClient,
    private val json: Json,
) {
    data class Section(val artSegments: List<Bitmap>, val textMarkup: String)
    data class Job(val sections: List<Section>)

    /**
     * Renders a single card image. For a single-faced card it renders the whole card; for a
     * multi-faced card (adventure / DFC / split) it renders one chosen face ([faceIndex], default
     * the front). Multi-faced cards without their own per-face art (adventure, split) fall back to
     * the card's top-level art.
     */
    suspend fun build(
        card: PrintableCard,
        widthDots: Int,
        brightness: Int,
        faceIndex: Int? = null,
    ): Job = withContext(Dispatchers.IO) {
        val faces = parseFaces(card)
        val section = if (faces.isEmpty()) {
            renderFace(
                art = card.artCropUrl?.let { downloadBitmap(it) },
                name = card.name,
                manaCost = card.manaCost,
                typeLine = card.typeLine,
                oracleText = card.oracleText,
                power = card.power,
                toughness = card.toughness,
                widthDots = widthDots,
                brightness = brightness,
            )
        } else {
            val face = faces[(faceIndex ?: 0).coerceIn(faces.indices)]
            val artUrl = face.imageUris?.artCrop ?: card.artCropUrl
            renderFace(
                art = artUrl?.let { downloadBitmap(it) },
                name = face.name ?: card.name,
                manaCost = face.manaCost,
                typeLine = face.typeLine,
                oracleText = face.oracleText,
                power = face.power,
                toughness = face.toughness,
                widthDots = widthDots,
                brightness = brightness,
            )
        }
        Job(listOf(section))
    }

    private fun parseFaces(card: PrintableCard): List<CardFaceDto> =
        card.cardFacesJson?.let {
            runCatching { json.decodeFromString<List<CardFaceDto>>(it) }.getOrNull()
        }.orEmpty()

    /**
     * Renders a card's rulings as one bordered text image (no art): the card name, a "Rulings"
     * subhead, then each ruling wrapped. Split into ≤256px segments with the same border + blank
     * bottom margin as a card, so the auto-cutter clips blank paper.
     */
    suspend fun buildRulings(title: String, rulings: List<String>, widthDots: Int): Job =
        withContext(Dispatchers.Default) {
            val scale = widthDots / 576f
            val outer = 6f * scale
            val stroke = (4f * scale).coerceAtLeast(2f)
            val innerPad = 14f * scale
            val left = outer + stroke + innerPad
            val contentWidth = (widthDots - 2f * left).toInt().coerceAtLeast(1)
            val gap = 10f * scale

            val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.BLACK
                textSize = 32f * scale
                isFakeBoldText = true
            }
            val subPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.BLACK
                textSize = 24f * scale
                isFakeBoldText = true
            }
            val bodyPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.BLACK
                textSize = 24f * scale
            }

            fun layout(text: String, paint: TextPaint, align: Layout.Alignment): StaticLayout =
                StaticLayout.Builder.obtain(text, 0, text.length, paint, contentWidth)
                    .setAlignment(align).build()

            val titleLayout = layout(title, titlePaint, Layout.Alignment.ALIGN_CENTER)
            val subLayout = layout("Rulings", subPaint, Layout.Alignment.ALIGN_CENTER)
            val lines = if (rulings.isEmpty()) listOf("No rulings for this card.") else rulings
            val bodyLayouts = lines.map { layout(it, bodyPaint, Layout.Alignment.ALIGN_NORMAL) }

            val topInset = outer + stroke + innerPad
            var contentH = titleLayout.height.toFloat() + gap + subLayout.height
            for (bl in bodyLayouts) contentH += gap + bl.height
            val frameBottom = topInset + contentH + innerPad + stroke
            val bottomSafety = 96f * scale
            val totalHeight = (frameBottom + bottomSafety).toInt().coerceAtLeast(1)

            val bmp = Bitmap.createBitmap(widthDots, totalHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)
            canvas.drawColor(Color.WHITE)

            var y = topInset
            fun draw(l: StaticLayout) {
                canvas.save()
                canvas.translate(left, y)
                l.draw(canvas)
                canvas.restore()
                y += l.height
            }
            draw(titleLayout)
            y += gap
            draw(subLayout)
            for (bl in bodyLayouts) {
                y += gap
                draw(bl)
            }

            val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                color = Color.BLACK
                strokeWidth = stroke
            }
            val half = stroke / 2f
            canvas.drawRect(
                outer + half,
                outer + half,
                widthDots - outer - half,
                frameBottom - half,
                borderPaint,
            )
            Job(listOf(Section(splitVertical(bmp, MAX_SEGMENT_HEIGHT), "")))
        }

    /** Renders one card face (art + text) as a single black-bordered image, split for printing. */
    private fun renderFace(
        art: Bitmap?,
        name: String,
        manaCost: String?,
        typeLine: String?,
        oracleText: String?,
        power: String?,
        toughness: String?,
        widthDots: Int,
        brightness: Int,
    ): Section {
        val scale = widthDots / 576f
        val outer = 6f * scale
        val stroke = (4f * scale).coerceAtLeast(2f)
        val innerPad = 14f * scale
        val left = outer + stroke + innerPad
        val contentWidth = (widthDots - 2f * left).toInt().coerceAtLeast(1)
        val gap = 10f * scale

        val title = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 32f * scale
            isFakeBoldText = true
        }
        // Fixed-size paint for the short lines (mana cost, type, power/toughness).
        val meta = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 26f * scale
        }

        val artBmp = art?.let {
            val scaled = capHeight(scaleToWidth(it, contentWidth), (contentWidth * MAX_ART_ASPECT).toInt())
            Dither.floydSteinberg(scaled, brightness)
        }
        val artHeight = artBmp?.height ?: 0
        val costAscii = ManaText.toAscii(manaCost)
        val typeText = typeLine?.takeIf { it.isNotBlank() }
        val oracleAscii = oracleText?.let { ManaText.toAscii(it) }?.takeIf { it.isNotBlank() }
        val ptText = if (!power.isNullOrBlank() && !toughness.isNullOrBlank()) "$power/$toughness" else null

        fun lineHeight(p: Paint): Float = p.fontMetrics.let { it.descent - it.ascent }

        // Every card is normalised to a real card's proportions (63x88mm). Text-light cards are
        // padded up to this minimum (their text centred in the blank space); text-heavy cards shrink
        // their oracle font to try to fit, overflowing only if even the smallest size won't do.
        val cardAspect = 88f / 63f
        val targetContentH =
            ((widthDots - 2f * outer) * cardAspect - 2f * stroke - 2f * innerPad).coerceAtLeast(1f)

        val artBlockH = artHeight + (if (artHeight > 0) gap else 0f)
        val fixedTextH = lineHeight(title) +
            (if (costAscii.isNotBlank()) lineHeight(meta) else 0f) +
            (if (typeText != null) lineHeight(meta) else 0f) +
            (if (ptText != null) gap + lineHeight(meta) else 0f)

        fun oracleLayoutAt(size: Float): StaticLayout? =
            oracleAscii?.let {
                val p = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.BLACK
                    textSize = size
                }
                StaticLayout.Builder.obtain(it, 0, it.length, p, contentWidth).build()
            }
        fun contentHeightWith(layout: StaticLayout?): Float =
            artBlockH + fixedTextH + (if (layout != null) gap + layout.height else 0f)

        // Floor kept high so wordy cards (e.g. Bureaucracy) stay legible — they grow taller instead.
        val minOracle = 22f * scale
        var oracleSize = 26f * scale
        var oracleLayout = oracleLayoutAt(oracleSize)
        while (oracleLayout != null && oracleSize > minOracle &&
            contentHeightWith(oracleLayout) > targetContentH
        ) {
            oracleSize -= scale
            oracleLayout = oracleLayoutAt(oracleSize)
        }

        val textHeight = fixedTextH + (if (oracleLayout != null) gap + oracleLayout.height else 0f)
        val naturalContentH = artBlockH + textHeight
        val frameContentH = maxOf(targetContentH, naturalContentH)

        val topInset = outer + stroke + innerPad
        val frameBottom = topInset + frameContentH + innerPad + stroke
        // Blank paper below the frame so the auto-cutter's fixed clip lands here, not on the card.
        // (Increasing the pre-cut feed had no effect — this printer ignores it — so the margin is
        // baked into the image itself.)
        val bottomSafety = 96f * scale
        val totalHeight = (frameBottom + bottomSafety).toInt().coerceAtLeast(1)

        val bmp = Bitmap.createBitmap(widthDots, totalHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.drawColor(Color.WHITE)

        var y = topInset
        if (artBmp != null) {
            canvas.drawBitmap(artBmp, left, y, null)
            y += artHeight + gap
        }

        // Centre the text block in whatever space remains below the art (blank space on light cards).
        val textRegionH = (topInset + frameContentH) - y
        y += ((textRegionH - textHeight) / 2f).coerceAtLeast(0f)

        title.textAlign = Paint.Align.CENTER
        y -= title.fontMetrics.ascent
        canvas.drawText(name, widthDots / 2f, y, title)
        y += title.fontMetrics.descent

        if (costAscii.isNotBlank()) {
            meta.textAlign = Paint.Align.CENTER
            y -= meta.fontMetrics.ascent
            canvas.drawText(costAscii, widthDots / 2f, y, meta)
            y += meta.fontMetrics.descent
        }
        if (typeText != null) {
            meta.textAlign = Paint.Align.LEFT
            y -= meta.fontMetrics.ascent
            canvas.drawText(typeText, left, y, meta)
            y += meta.fontMetrics.descent
        }
        if (oracleLayout != null) {
            y += gap
            canvas.save()
            canvas.translate(left, y)
            oracleLayout.draw(canvas)
            canvas.restore()
            y += oracleLayout.height
        }
        if (ptText != null) {
            y += gap
            meta.textAlign = Paint.Align.RIGHT
            y -= meta.fontMetrics.ascent
            canvas.drawText(ptText, widthDots - left, y, meta)
        }

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = Color.BLACK
            strokeWidth = stroke
        }
        val half = stroke / 2f
        canvas.drawRect(
            outer + half,
            outer + half,
            widthDots - outer - half,
            frameBottom - half,
            borderPaint,
        )

        return Section(artSegments = splitVertical(bmp, MAX_SEGMENT_HEIGHT), textMarkup = "")
    }

    private fun downloadBitmap(url: String): Bitmap? {
        val req = Request.Builder().url(url).build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) return null
            return BitmapFactory.decodeStream(resp.body.byteStream())
        }
    }

    private fun scaleToWidth(bmp: Bitmap, widthDots: Int): Bitmap {
        if (bmp.width == widthDots) return bmp
        val height = (bmp.height.toFloat() * widthDots / bmp.width).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bmp, widthDots, height, true)
    }

    /** Crops a too-tall art crop (e.g. Saga portrait art) to [maxHeight], keeping the top. */
    private fun capHeight(bmp: Bitmap, maxHeight: Int): Bitmap =
        if (bmp.height <= maxHeight) bmp else Bitmap.createBitmap(bmp, 0, 0, bmp.width, maxHeight)

    private fun splitVertical(bmp: Bitmap, maxHeight: Int): List<Bitmap> {
        val segments = ArrayList<Bitmap>()
        var y = 0
        while (y < bmp.height) {
            val h = min(maxHeight, bmp.height - y)
            segments.add(Bitmap.createBitmap(bmp, 0, y, bmp.width, h))
            y += h
        }
        return segments
    }

    private companion object {
        const val MAX_SEGMENT_HEIGHT = 256

        /** Max art height as a multiple of card width; taller crops (Sagas) are cropped to this. */
        const val MAX_ART_ASPECT = 1.0f
    }
}
