package com.pduvall.whtz.data.printer

import android.graphics.Bitmap

/**
 * Floyd–Steinberg dithering to pure black/white. Card art printed on a monochrome thermal head
 * looks muddy with a plain luminance threshold; error diffusion preserves tone far better.
 * Returns a new ARGB_8888 bitmap whose pixels are all 0xFF000000 or 0xFFFFFFFF.
 */
object Dither {
    fun floydSteinberg(src: Bitmap, brightness: Int = 24): Bitmap {
        val w = src.width
        val h = src.height
        val pixels = IntArray(w * h)
        src.getPixels(pixels, 0, w, 0, 0, w, h)

        // Grayscale + brightness lift (thermal heads run dark, so we lighten a touch).
        val gray = FloatArray(w * h)
        for (i in pixels.indices) {
            val p = pixels[i]
            val r = (p ushr 16) and 0xFF
            val g = (p ushr 8) and 0xFF
            val b = p and 0xFF
            gray[i] = (0.299f * r + 0.587f * g + 0.114f * b) + brightness
        }

        for (y in 0 until h) {
            for (x in 0 until w) {
                val i = y * w + x
                val old = gray[i]
                val newVal = if (old < 128f) 0f else 255f
                val err = old - newVal
                gray[i] = newVal
                if (x + 1 < w) gray[i + 1] += err * 7f / 16f
                if (x - 1 >= 0 && y + 1 < h) gray[i - 1 + w] += err * 3f / 16f
                if (y + 1 < h) gray[i + w] += err * 5f / 16f
                if (x + 1 < w && y + 1 < h) gray[i + 1 + w] += err * 1f / 16f
                pixels[i] = if (newVal == 0f) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()
            }
        }

        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        out.setPixels(pixels, 0, w, 0, 0, w, h)
        return out
    }
}
