package com.pduvall.whtz.ui.tokens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pduvall.whtz.data.local.entity.TokenCardEntity
import com.pduvall.whtz.data.printer.CardJobPrinter
import com.pduvall.whtz.data.printer.CardRasterizer
import com.pduvall.whtz.data.printer.PrinterSettingsStore
import com.pduvall.whtz.data.tokens.TokenRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TokenLibraryViewModel @Inject constructor(
    private val tokenRepository: TokenRepository,
    private val cardRasterizer: CardRasterizer,
    private val cardJobPrinter: CardJobPrinter,
    private val printerSettings: PrinterSettingsStore,
) : ViewModel() {

    data class UiState(
        val query: String = "",
        val loading: Boolean = false,
        val tokenCount: Int = 0,
        val results: List<TokenCardEntity> = emptyList(),
    )

    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui.asStateFlow()

    private val _printing = MutableStateFlow(false)
    val printing: StateFlow<Boolean> = _printing.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private var searchJob: Job? = null

    init {
        viewModelScope.launch { refreshCountAndSearch() }
    }

    fun onQueryChange(query: String) {
        _ui.value = _ui.value.copy(query = query)
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(300) // debounce typing
            search()
        }
    }

    fun refresh() {
        viewModelScope.launch { refreshCountAndSearch() }
    }

    private suspend fun refreshCountAndSearch() {
        val count = tokenRepository.tokenCount()
        _ui.value = _ui.value.copy(tokenCount = count)
        if (count > 0) search()
    }

    private suspend fun search() {
        _ui.value = _ui.value.copy(loading = true)
        val results = tokenRepository.search(_ui.value.query)
        _ui.value = _ui.value.copy(loading = false, results = results)
    }

    fun print(token: TokenCardEntity) {
        if (!cardJobPrinter.hasPrinter()) {
            _message.value = "No printer selected — open Printer settings first."
            return
        }
        viewModelScope.launch {
            _printing.value = true
            try {
                val job = cardRasterizer.build(
                    token,
                    printerSettings.widthDots,
                    printerSettings.brightness,
                )
                cardJobPrinter.print(job)
                _message.value = "Printed ${token.name}"
            } catch (e: Exception) {
                _message.value = "Print failed: ${e.message}"
            } finally {
                _printing.value = false
            }
        }
    }

    fun clearMessage() {
        _message.value = null
    }
}
