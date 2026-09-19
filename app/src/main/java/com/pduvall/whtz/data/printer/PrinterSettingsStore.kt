package com.pduvall.whtz.data.printer

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Persists the chosen printer + paper width. */
@Singleton
class PrinterSettingsStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs = context.getSharedPreferences("printer_prefs", Context.MODE_PRIVATE)

    var deviceAddress: String?
        get() = prefs.getString(KEY_ADDRESS, null)
        set(value) = prefs.edit().putString(KEY_ADDRESS, value).apply()

    var deviceName: String?
        get() = prefs.getString(KEY_NAME, null)
        set(value) = prefs.edit().putString(KEY_NAME, value).apply()

    /** 58 or 80 (mm). Defaults to 80mm (user's NETUM NT-861). */
    var widthMm: Int
        get() = prefs.getInt(KEY_WIDTH, 80)
        set(value) = prefs.edit().putInt(KEY_WIDTH, value).apply()

    /**
     * Brightness lift fed to the ditherer. Higher = lighter print (thermal heads run dark).
     * Presets: [BRIGHTNESS_DARK], [BRIGHTNESS_NORMAL], [BRIGHTNESS_LIGHT].
     */
    var brightness: Int
        get() = prefs.getInt(KEY_BRIGHTNESS, BRIGHTNESS_NORMAL)
        set(value) = prefs.edit().putInt(KEY_BRIGHTNESS, value).apply()

    val widthDots: Int get() = if (widthMm >= 80) 576 else 384
    val charsPerLine: Int get() = if (widthMm >= 80) 48 else 32
    val printableWidthMm: Float get() = if (widthMm >= 80) 72f else 48f

    companion object {
        const val BRIGHTNESS_DARK = 0
        const val BRIGHTNESS_NORMAL = 24
        const val BRIGHTNESS_LIGHT = 48

        private const val KEY_ADDRESS = "device_address"
        private const val KEY_NAME = "device_name"
        private const val KEY_WIDTH = "width_mm"
        private const val KEY_BRIGHTNESS = "brightness"
    }
}
