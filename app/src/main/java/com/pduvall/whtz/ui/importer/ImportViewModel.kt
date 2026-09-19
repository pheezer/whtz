package com.pduvall.whtz.ui.importer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pduvall.whtz.data.scryfall.BulkImporter
import com.pduvall.whtz.data.scryfall.CardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ImportViewModel @Inject constructor(
    private val repository: CardRepository,
    private val importer: BulkImporter,
) : ViewModel() {

    sealed interface State {
        data object Checking : State
        data object NeedsImport : State
        data class Importing(val phase: String, val imported: Int) : State
        data class Ready(val cardCount: Int) : State
        data class Error(val message: String) : State
    }

    private val _state = MutableStateFlow<State>(State.Checking)
    val state: StateFlow<State> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val count = repository.cardCount()
            _state.value = if (count > 0) State.Ready(count) else State.NeedsImport
        }
    }

    fun startImport() {
        viewModelScope.launch {
            _state.value = State.Importing("cards", 0)
            try {
                importer.import().collect { progress ->
                    _state.value = State.Importing(progress.phase, progress.imported)
                }
                _state.value = State.Ready(repository.cardCount())
            } catch (e: Exception) {
                _state.value = State.Error(e.message ?: "Import failed")
            }
        }
    }
}
