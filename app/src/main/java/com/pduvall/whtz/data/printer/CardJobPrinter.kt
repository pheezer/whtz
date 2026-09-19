package com.pduvall.whtz.data.printer

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Prints an already-rasterized [CardRasterizer.Job] to the configured printer, applying the saved
 * paper-width settings. Shared by the token library and the in-game token/ruling print actions.
 */
@Singleton
class CardJobPrinter @Inject constructor(
    private val printerManager: PrinterManager,
    private val settings: PrinterSettingsStore,
) {
    fun hasPrinter(): Boolean = settings.deviceAddress != null

    /** Throws IllegalStateException if no printer is configured. */
    suspend fun print(job: CardRasterizer.Job) {
        val address = settings.deviceAddress
            ?: error("No printer selected — open Printer settings first.")
        printerManager.printCard(
            job = job,
            deviceAddress = address,
            printableWidthMm = settings.printableWidthMm,
            charsPerLine = settings.charsPerLine,
        )
    }
}
