package com.pduvall.whtz.ui.deck

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pduvall.whtz.data.deck.DeckImportOutcome
import com.pduvall.whtz.data.deck.DeckRepository
import com.pduvall.whtz.data.deck.DeckSection
import com.pduvall.whtz.data.deck.ResolvedDeck
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DeckImportViewModel @Inject constructor(
    private val deckRepository: DeckRepository,
) : ViewModel() {

    sealed interface State {
        data object Editing : State
        data object Importing : State
        data class Imported(val outcome: DeckImportOutcome) : State
        data class Error(val message: String) : State
    }

    private val _state = MutableStateFlow<State>(State.Editing)
    val state: StateFlow<State> = _state.asStateFlow()

    private var editDeckId: Long? = null

    private val _initialName = MutableStateFlow("")
    val initialName: StateFlow<String> = _initialName.asStateFlow()

    private val _initialText = MutableStateFlow("")
    val initialText: StateFlow<String> = _initialText.asStateFlow()

    /** Loads an existing deck's name + decklist text to edit; saving will replace that deck. */
    fun startEdit(deckId: Long) {
        if (editDeckId == deckId) return
        editDeckId = deckId
        viewModelScope.launch {
            deckRepository.loadForEdit(deckId)?.let {
                _initialName.value = it.name
                _initialText.value = it.text
            }
        }
    }

    fun import(name: String, text: String) {
        viewModelScope.launch {
            _state.value = State.Importing
            _state.value = try {
                State.Imported(deckRepository.importFromText(name, text))
            } catch (e: Exception) {
                State.Error(e.message ?: "Import failed")
            }
        }
    }

    /** Saves the deck, marking the chosen cards as commander(s) (command zone, 1 copy each). */
    fun save(deck: ResolvedDeck, commanderOracleIds: Set<String>, onSaved: (Long) -> Unit) {
        viewModelScope.launch {
            val adjusted = deck.copy(
                cards = deck.cards.map { card ->
                    if (card.oracleId in commanderOracleIds) {
                        card.copy(section = DeckSection.COMMANDER, quantity = 1)
                    } else {
                        card.copy(section = DeckSection.MAINBOARD)
                    }
                },
            )
            val editing = editDeckId
            val id = if (editing != null) {
                deckRepository.updateDeck(editing, adjusted)
            } else {
                deckRepository.saveDeck(adjusted)
            }
            onSaved(id)
        }
    }

    fun backToEditing() {
        _state.value = State.Editing
    }
}
