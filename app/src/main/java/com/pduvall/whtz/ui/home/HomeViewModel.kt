package com.pduvall.whtz.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pduvall.whtz.data.deck.DeckRepository
import com.pduvall.whtz.data.local.GameStateFileStore
import com.pduvall.whtz.data.scryfall.BulkImporter
import com.pduvall.whtz.data.local.entity.DeckEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val deckRepository: DeckRepository,
    private val gameStateStore: GameStateFileStore,
    private val importer: BulkImporter,
) : ViewModel() {

    data class State(
        val decks: List<DeckEntity> = emptyList(),
        val hasSavedGame: Boolean = false,
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    sealed interface Update {
        data object Idle : Update
        data class Running(val phase: String, val imported: Int) : Update
        data class Done(val count: Int) : Update
        data class Failed(val message: String) : Update
    }

    private val _update = MutableStateFlow<Update>(Update.Idle)
    val update: StateFlow<Update> = _update.asStateFlow()

    /**
     * Re-runs the bulk import (cards + rulings + tokens), backfilling new fields without touching
     * saved decks. Reports the final card count on completion.
     */
    fun updateDatabase() {
        if (_update.value is Update.Running) return
        viewModelScope.launch {
            _update.value = Update.Running("cards", 0)
            var cards = 0
            try {
                importer.import().collect { p ->
                    if (p.phase == "cards") cards = p.imported
                    _update.value = Update.Running(p.phase, p.imported)
                }
                _update.value = Update.Done(cards)
                refresh()
            } catch (e: Exception) {
                _update.value = Update.Failed(e.message ?: "Update failed")
            }
        }
    }

    /** Reloads decks and whether a game is in progress. Call when the screen becomes visible. */
    fun refresh() {
        viewModelScope.launch {
            _state.value = State(
                decks = deckRepository.getDecks(),
                hasSavedGame = gameStateStore.hasSaved(),
            )
        }
    }

    fun deleteDeck(deckId: Long) {
        viewModelScope.launch {
            deckRepository.deleteDeck(deckId)
            refresh()
        }
    }
}
