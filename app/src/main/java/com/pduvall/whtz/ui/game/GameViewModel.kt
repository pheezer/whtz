package com.pduvall.whtz.ui.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pduvall.whtz.data.deck.CardMeta
import com.pduvall.whtz.data.deck.DeckRepository
import com.pduvall.whtz.data.deck.PrintMode
import com.pduvall.whtz.data.deck.cardTypesOf
import com.pduvall.whtz.data.deck.manaValueOf
import com.pduvall.whtz.data.deck.printModes
import kotlinx.coroutines.delay
import com.pduvall.whtz.data.local.GameStateFileStore
import com.pduvall.whtz.data.local.dao.OracleCardDao
import com.pduvall.whtz.data.local.entity.TokenCardEntity
import com.pduvall.whtz.data.printer.CardJobPrinter
import com.pduvall.whtz.data.printer.CardRasterizer
import com.pduvall.whtz.data.printer.PrinterSettingsStore
import com.pduvall.whtz.data.rulings.RulingRepository
import com.pduvall.whtz.data.scryfall.CardFaceDto
import com.pduvall.whtz.data.tokens.TokenRepository
import com.pduvall.whtz.data.sound.Sfx
import com.pduvall.whtz.data.sound.SoundManager
import com.pduvall.whtz.domain.engine.Command
import com.pduvall.whtz.domain.engine.GameSession
import com.pduvall.whtz.domain.model.GameState
import com.pduvall.whtz.domain.model.Zone
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.serialization.json.Json
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Matches an all_parts entry with component "token" (cheap gate for the "Print tokens" action). */
private val TOKEN_PART = Regex("\"component\"\\s*:\\s*\"token\"")

data class GameUiState(
    val loading: Boolean = true,
    val gameState: GameState? = null,
    val deckName: String = "",
    val canUndo: Boolean = false,
    val cardMeta: Map<String, CardMeta> = emptyMap(),
    val log: List<String> = emptyList(),
)

/** One face of a viewed card (double-faced cards have two; tap to flip between them). */
data class ViewedFace(val name: String, val imageUrl: String?)

/** A card the user tapped to view full-size (one or more faces). */
data class ViewedCard(val faces: List<ViewedFace>)

/** State of the "what to print" dialog for a multi-mode card (adventure / DFC / split). */
data class PrintChooser(
    val instanceId: String,
    val sourceName: String,
    val modes: List<PrintMode>,
)

/** State of the "Print tokens" dialog: the tokens a given card creates. */
data class TokenOptions(
    val sourceName: String,
    val loading: Boolean = false,
    val tokens: List<TokenCardEntity> = emptyList(),
    val error: String? = null,
)

@HiltViewModel
class GameViewModel @Inject constructor(
    private val deckRepository: DeckRepository,
    private val gameStateStore: GameStateFileStore,
    private val oracleCardDao: OracleCardDao,
    private val cardRasterizer: CardRasterizer,
    private val printerSettings: PrinterSettingsStore,
    private val soundManager: SoundManager,
    private val tokenRepository: TokenRepository,
    private val rulingRepository: RulingRepository,
    private val cardJobPrinter: CardJobPrinter,
    private val json: Json,
) : ViewModel() {

    fun playDraw() = soundManager.play(Sfx.DRAW)
    fun playShuffle() = soundManager.play(Sfx.SHUFFLE)
    fun playSwoosh() = soundManager.play(Sfx.SWOOSH)
    fun playBubble() = soundManager.play(Sfx.BUBBLE)

    /** Draw sound fired rapidly seven times (for Draw 7 and Wheel). */
    fun playDrawSeven() {
        viewModelScope.launch {
            repeat(7) {
                soundManager.play(Sfx.DRAW)
                delay(70)
            }
        }
    }

    private var session: GameSession? = null
    private var deckId: Long? = null
    private var deckName: String = ""
    private var cardMeta: Map<String, CardMeta> = emptyMap()

    private val _ui = MutableStateFlow(GameUiState())
    val ui: StateFlow<GameUiState> = _ui.asStateFlow()

    private val _printing = MutableStateFlow(false)
    val printing: StateFlow<Boolean> = _printing.asStateFlow()

    private val _printMessage = MutableStateFlow<String?>(null)
    val printMessage: StateFlow<String?> = _printMessage.asStateFlow()

    fun clearPrintMessage() {
        _printMessage.value = null
    }

    private val _viewedCard = MutableStateFlow<ViewedCard?>(null)
    val viewedCard: StateFlow<ViewedCard?> = _viewedCard.asStateFlow()

    /** The tokens a tapped card creates, for the "Print tokens" dialog (null = closed). */
    private val _tokenOptions = MutableStateFlow<TokenOptions?>(null)
    val tokenOptions: StateFlow<TokenOptions?> = _tokenOptions.asStateFlow()

    /** "What to print" dialog for multi-mode cards (null = closed). */
    private val _printChooser = MutableStateFlow<PrintChooser?>(null)
    val printChooser: StateFlow<PrintChooser?> = _printChooser.asStateFlow()

    /** Load a card's image(s) to show as an overlay. Double-faced cards expose both sides. */
    fun viewCard(oracleId: String) {
        viewModelScope.launch {
            val card = oracleCardDao.getByOracleId(oracleId)
            val faces = card?.cardFacesJson?.let {
                runCatching { json.decodeFromString<List<CardFaceDto>>(it) }.getOrNull()
            }.orEmpty()
            val viewFaces = if (faces.count { it.imageUris?.normal != null } >= 2) {
                faces.map {
                    ViewedFace(it.name ?: card?.name.orEmpty(), it.imageUris?.normal ?: it.imageUris?.artCrop)
                }
            } else {
                listOf(ViewedFace(card?.name.orEmpty(), card?.normalImageUrl ?: card?.artCropUrl))
            }
            _viewedCard.value = ViewedCard(viewFaces)
        }
    }

    fun dismissViewedCard() {
        _viewedCard.value = null
    }

    fun dismissPrintChooser() {
        _printChooser.value = null
    }

    /**
     * For a card already printed this game: place a copy without reprinting (the "print one, mark
     * the rest" rule — e.g. 4x Templar Knight). Routes by type, so a duplicate instant/sorcery
     * lands in the graveyard rather than the battlefield.
     */
    fun markOnBattlefield(instanceId: String) {
        val instance = session?.state?.locate(instanceId)?.second ?: return
        viewModelScope.launch {
            val card = oracleCardDao.getByOracleId(instance.oracleId)
            val dest = card?.printModes(json)?.firstOrNull()?.destination ?: Zone.BATTLEFIELD
            dispatch(Command.MoveCard(instanceId, dest))
            _printMessage.value = "Placed ${instance.name} (already printed)"
        }
    }

    /** Cast a commander from the command zone to the battlefield, adding +{2} commander tax. */
    fun castCommander(instanceId: String) {
        dispatch(Command.CastCommander(instanceId))
    }

    /** Return a commander to the command zone (e.g. after it dies), where it can be recast. */
    fun returnCommanderToCommandZone(instanceId: String) {
        val instance = session?.state?.locate(instanceId)?.second ?: return
        dispatch(Command.MoveCard(instanceId, Zone.COMMAND))
        _printMessage.value = "${instance.name} returned to the command zone"
    }

    /**
     * Begins printing a card. Single-mode cards print immediately; multi-mode cards (adventure,
     * double-faced, split) open a chooser so the player picks which face/spell to print.
     */
    fun requestPrint(instanceId: String) {
        val instance = session?.state?.locate(instanceId)?.second ?: return
        if (!cardJobPrinter.hasPrinter()) {
            _printMessage.value = "No printer selected — open Printer settings first."
            return
        }
        viewModelScope.launch {
            val card = oracleCardDao.getByOracleId(instance.oracleId)
            if (card == null) {
                _printMessage.value = "Card data not found"
                return@launch
            }
            val modes = card.printModes(json)
            if (modes.size <= 1) {
                printMode(instanceId, modes.firstOrNull() ?: PrintMode(instance.name, null, Zone.BATTLEFIELD))
            } else {
                _printChooser.value = PrintChooser(instanceId, card.name, modes)
            }
        }
    }

    /**
     * Prints one [mode] of a card, then routes the instance to that mode's destination and records
     * it as printed this game. Printing a commander from the command zone counts as casting it.
     */
    fun printMode(instanceId: String, mode: PrintMode) {
        _printChooser.value = null
        val located = session?.state?.locate(instanceId) ?: return
        val (fromZone, instance) = located
        if (!cardJobPrinter.hasPrinter()) {
            _printMessage.value = "No printer selected — open Printer settings first."
            return
        }
        viewModelScope.launch {
            _printing.value = true
            try {
                val card = oracleCardDao.getByOracleId(instance.oracleId)
                    ?: error("Card data not found")
                val job = cardRasterizer.build(
                    card,
                    printerSettings.widthDots,
                    printerSettings.brightness,
                    mode.faceIndex,
                )
                cardJobPrinter.print(job)
                val castsCommander = session?.state?.isCommander(instanceId) == true &&
                    fromZone == Zone.COMMAND &&
                    mode.destination == Zone.BATTLEFIELD
                if (castsCommander) {
                    dispatch(Command.CastCommander(instanceId))
                } else {
                    dispatch(Command.MoveCard(instanceId, mode.destination))
                }
                dispatch(Command.MarkPrinted(instanceId))
                _printMessage.value = "Printed ${mode.label}"
            } catch (e: Exception) {
                _printMessage.value = "Print failed: ${e.message}"
            } finally {
                _printing.value = false
            }
        }
    }

    /** Open the "Print tokens" dialog, loading the tokens the given card creates. */
    fun showTokensFor(oracleId: String) {
        _tokenOptions.value = TokenOptions(sourceName = "", loading = true)
        viewModelScope.launch {
            try {
                val name = oracleCardDao.getByOracleId(oracleId)?.name.orEmpty()
                val tokens = tokenRepository.tokensCreatedBy(oracleId)
                _tokenOptions.value = TokenOptions(sourceName = name, tokens = tokens)
            } catch (e: Exception) {
                _tokenOptions.value =
                    TokenOptions(sourceName = "", error = e.message ?: "Failed to load tokens")
            }
        }
    }

    fun dismissTokenOptions() {
        _tokenOptions.value = null
    }

    /** Print a single token (from the token dialog). Tokens aren't tracked in game zones. */
    fun printToken(token: TokenCardEntity) {
        if (!cardJobPrinter.hasPrinter()) {
            _printMessage.value = "No printer selected — open Printer settings first."
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
                _printMessage.value = "Printed ${token.name}"
            } catch (e: Exception) {
                _printMessage.value = "Print failed: ${e.message}"
            } finally {
                _printing.value = false
            }
        }
    }

    /** Print a card's rulings as a bordered text sheet. */
    fun printRulings(oracleId: String, cardName: String) {
        if (!cardJobPrinter.hasPrinter()) {
            _printMessage.value = "No printer selected — open Printer settings first."
            return
        }
        viewModelScope.launch {
            _printing.value = true
            try {
                val lines = rulingRepository.printableLines(oracleId)
                val job = cardRasterizer.buildRulings(cardName, lines, printerSettings.widthDots)
                cardJobPrinter.print(job)
                _printMessage.value = "Printed rulings for $cardName"
            } catch (e: Exception) {
                _printMessage.value = "Print failed: ${e.message}"
            } finally {
                _printing.value = false
            }
        }
    }

    /** Resume the persisted game, if any. */
    fun resume() {
        viewModelScope.launch {
            val saved = gameStateStore.load()
            if (saved != null) {
                session = GameSession(saved.state)
                deckId = saved.deckId
                deckName = saved.deckName
                loadCardMeta(saved.state)
            }
            publish(loading = false)
        }
    }

    fun startGameFromDeck(id: Long) {
        viewModelScope.launch {
            _ui.value = _ui.value.copy(loading = true)
            val deck = deckRepository.loadDeck(id)
            if (deck == null) {
                publish(loading = false)
                return@launch
            }
            session = GameSession(GameFactory.newGame(deck))
            deckId = id
            deckName = deck.name
            persist()
            loadCardMeta(session!!.state)
            publish(loading = false)
        }
    }

    fun dispatch(command: Command) {
        val s = session ?: return
        s.apply(command, describe(command, s.state))
        persist()
        publish(loading = false)
    }

    /** Revert the game to a past point in the action log (discards later actions). */
    fun revertTo(index: Int) {
        val s = session ?: return
        s.revertTo(index)
        persist()
        publish(loading = false)
    }

    private fun describe(command: Command, pre: GameState): String = when (command) {
        is Command.Draw -> "Draw ${command.count}"
        is Command.MillTop -> "Mill ${command.count}"
        is Command.ExileTop -> "Exile ${command.count}"
        Command.Shuffle -> "Shuffle"
        Command.Wheel -> "Wheel (discard hand, draw 7)"
        is Command.MoveToTop -> "${nameOf(pre, command.instanceId)} → library top"
        is Command.MoveToBottom -> "${nameOf(pre, command.instanceId)} → library bottom"
        is Command.MoveCard -> "${nameOf(pre, command.instanceId)} → ${zoneWord(command.to)}"
        is Command.ReorderTop -> "Reorder top"
        is Command.MarkPrinted -> "Marked printed"
        is Command.CastCommander -> "Cast ${nameOf(pre, command.instanceId)}"
    }

    private fun nameOf(state: GameState, instanceId: String): String =
        state.locate(instanceId)?.second?.name ?: "card"

    private fun zoneWord(zone: Zone): String = when (zone) {
        Zone.LIBRARY -> "library"
        Zone.HAND -> "hand"
        Zone.GRAVEYARD -> "graveyard"
        Zone.EXILE -> "exile"
        Zone.BATTLEFIELD -> "battlefield"
        Zone.COMMAND -> "command zone"
    }

    fun undo() {
        val s = session ?: return
        s.undo()
        persist()
        publish(loading = false)
    }

    fun endGame() {
        session = null
        deckId = null
        deckName = ""
        cardMeta = emptyMap()
        viewModelScope.launch { gameStateStore.clear() }
        publish(loading = false)
    }

    /** Loads type/rarity metadata (+ token/ruling gates) for the deck's distinct cards. */
    private suspend fun loadCardMeta(state: GameState) {
        val ids = state.zones.values.flatten().map { it.oracleId }.distinct()
        val rows = ids.chunked(900).flatMap { oracleCardDao.getMeta(it) }
        val withRulings = ids.chunked(900).flatMap { rulingRepository.oracleIdsWithRulings(it) }.toSet()
        cardMeta = rows.associate { row ->
            row.oracleId to CardMeta(
                types = cardTypesOf(row.typeLine),
                rarity = row.rarity,
                manaValue = manaValueOf(row.manaCost),
                manaCost = row.manaCost,
                imageUrl = row.normalImageUrl,
                createsTokens = row.allPartsJson?.let { TOKEN_PART.containsMatchIn(it) } == true,
                hasRulings = row.oracleId in withRulings,
            )
        }
    }

    private fun persist() {
        val snapshot = session?.state ?: return
        val id = deckId
        val name = deckName
        viewModelScope.launch {
            gameStateStore.save(snapshot, id, name)
        }
    }

    private fun publish(loading: Boolean) {
        _ui.value = GameUiState(
            loading = loading,
            gameState = session?.state,
            deckName = deckName,
            canUndo = session?.canUndo() == true,
            cardMeta = cardMeta,
            log = session?.log ?: emptyList(),
        )
    }
}
