package com.pduvall.whtz.data.printer

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import com.dantsu.escposprinter.EscPosPrinter
import com.dantsu.escposprinter.connection.bluetooth.BluetoothConnection
import com.dantsu.escposprinter.textparser.PrinterTextParserImg
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Connects to the paired thermal printer over Bluetooth Classic (SPP) via DantSu and prints
 * ESC/POS. The user must pair the printer in Android's Bluetooth settings first; we connect to
 * the chosen bonded device. Callers must hold BLUETOOTH_CONNECT (API 31+) before invoking.
 */
@Singleton
class PrinterManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    data class Device(val name: String, val address: String)

    private val adapter: BluetoothAdapter?
        get() = (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter

    fun isBluetoothReady(): Boolean = adapter?.isEnabled == true

    @SuppressLint("MissingPermission") // caller ensures BLUETOOTH_CONNECT is granted
    fun bondedDevices(): List<Device> =
        adapter?.bondedDevices?.map { Device(it.name ?: it.address, it.address) }.orEmpty()

    @SuppressLint("MissingPermission")
    suspend fun printCard(
        job: CardRasterizer.Job,
        deviceAddress: String,
        printableWidthMm: Float,
        charsPerLine: Int,
    ) = withContext(Dispatchers.IO) {
        // Estimate how long the printer needs to physically emit the art so we don't close the
        // socket mid-print (which truncates the tail — worst on 80mm, whose image is larger).
        val artDots = job.sections.sumOf { section -> section.artSegments.sumOf { it.height } }
        val drainMs = (2000L + artDots * 4L).coerceAtMost(12_000L)
        usePrinter(deviceAddress, printableWidthMm, charsPerLine, drainMs) { printer ->
            // Only the bordered card image(s) — no text outside the border. The image already
            // carries a blank bottom margin so the auto-cutter clips that, not the card.
            val markup = buildString {
                job.sections.forEach { section ->
                    section.artSegments.forEach { segment ->
                        append("[C]<img>")
                            .append(PrinterTextParserImg.bitmapToHexadecimalString(printer, segment))
                            .append("</img>\n")
                    }
                }
            }
            printer.printFormattedTextAndCut(markup, 12f)
        }
    }

    @SuppressLint("MissingPermission")
    suspend fun printTest(deviceAddress: String, printableWidthMm: Float, charsPerLine: Int) =
        withContext(Dispatchers.IO) {
            usePrinter(deviceAddress, printableWidthMm, charsPerLine, drainMs = 3000L) { printer ->
                // Feed ~a card's length before the cut so the slip is card-sized and shows the cut.
                printer.printFormattedTextAndCut(
                    "[C]<b>Whtz</b>\n[C]Card-size test\n[C]$charsPerLine cols\n",
                    75f,
                )
            }
        }

    @SuppressLint("MissingPermission")
    private inline fun <T> usePrinter(
        deviceAddress: String,
        printableWidthMm: Float,
        charsPerLine: Int,
        drainMs: Long,
        block: (EscPosPrinter) -> T,
    ): T {
        val device = adapter?.getRemoteDevice(deviceAddress)
            ?: error("Bluetooth is unavailable")
        val printer = EscPosPrinter(BluetoothConnection(device), 203, printableWidthMm, charsPerLine)
        try {
            return block(printer)
        } finally {
            // Give the printer time to finish emitting its buffer before the socket closes.
            try {
                Thread.sleep(drainMs)
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
            }
            printer.disconnectPrinter()
        }
    }
}
