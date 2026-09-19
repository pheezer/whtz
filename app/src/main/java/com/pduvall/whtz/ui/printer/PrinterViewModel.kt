package com.pduvall.whtz.ui.printer

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pduvall.whtz.data.printer.PrinterManager
import com.pduvall.whtz.data.printer.PrinterSettingsStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PrinterViewModel @Inject constructor(
    private val printerManager: PrinterManager,
    private val settings: PrinterSettingsStore,
) : ViewModel() {

    data class State(
        val hasPermission: Boolean = false,
        val bluetoothReady: Boolean = false,
        val devices: List<PrinterManager.Device> = emptyList(),
        val selectedAddress: String? = null,
        val widthMm: Int = 80,
        val brightness: Int = PrinterSettingsStore.BRIGHTNESS_NORMAL,
        val busy: Boolean = false,
        val message: String? = null,
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    fun refresh(hasPermission: Boolean) {
        _state.value = _state.value.copy(
            hasPermission = hasPermission,
            bluetoothReady = printerManager.isBluetoothReady(),
            devices = if (hasPermission) printerManager.bondedDevices() else emptyList(),
            selectedAddress = settings.deviceAddress,
            widthMm = settings.widthMm,
            brightness = settings.brightness,
        )
    }

    fun setBrightness(value: Int) {
        settings.brightness = value
        _state.value = _state.value.copy(brightness = value)
    }

    fun select(device: PrinterManager.Device) {
        settings.deviceAddress = device.address
        settings.deviceName = device.name
        _state.value = _state.value.copy(selectedAddress = device.address, message = "Selected ${device.name}")
    }

    fun setWidth(mm: Int) {
        settings.widthMm = mm
        _state.value = _state.value.copy(widthMm = mm)
    }

    fun testPrint() {
        val address = settings.deviceAddress
        if (address == null) {
            _state.value = _state.value.copy(message = "Select a printer first")
            return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(busy = true, message = "Sending test print…")
            _state.value = try {
                printerManager.printTest(address, settings.printableWidthMm, settings.charsPerLine)
                _state.value.copy(busy = false, message = "Test print sent")
            } catch (e: Exception) {
                Log.e("Whtz", "Test print failed", e)
                _state.value.copy(busy = false, message = "Print failed: ${e.message}")
            }
        }
    }
}
