package com.pduvall.whtz.ui.game

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ViewCarousel
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.pduvall.whtz.R
import com.pduvall.whtz.data.deck.CardMeta
import com.pduvall.whtz.data.deck.CardType
import com.pduvall.whtz.data.deck.PrintMode
import com.pduvall.whtz.data.local.entity.TokenCardEntity
import com.pduvall.whtz.data.printer.ManaText
import com.pduvall.whtz.ui.tokens.TokenRow
import com.pduvall.whtz.domain.engine.Command
import com.pduvall.whtz.domain.model.CardInstance
import com.pduvall.whtz.domain.model.GameState
import com.pduvall.whtz.domain.model.Zone
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun GameScreen(
    deckId: Long?,
    onExit: () -> Unit,
    privacy: Boolean = false,
    onExitPrivacy: () -> Unit = {},
    modifier: Modifier = Modifier,
    vm: GameViewModel = hiltViewModel(),
) {
    LaunchedEffect(deckId) {
        if (deckId != null) vm.startGameFromDeck(deckId) else vm.resume()
    }
    val ui by vm.ui.collectAsStateWithLifecycle()
    val printing by vm.printing.collectAsStateWithLifecycle()
    val printMessage by vm.printMessage.collectAsStateWithLifecycle()
    val viewedCard by vm.viewedCard.collectAsStateWithLifecycle()
    val tokenOptions by vm.tokenOptions.collectAsStateWithLifecycle()
    val printChooser by vm.printChooser.collectAsStateWithLifecycle()
    val state = ui.gameState

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            ui.loading -> CenteredSpinner()
            state == null -> CenteredMessage("No active game.", onExit)
            else -> GameContent(
                state = state,
                deckName = ui.deckName,
                canUndo = ui.canUndo,
                cardMeta = ui.cardMeta,
                log = ui.log,
                printing = printing,
                printMessage = printMessage,
                onCommand = vm::dispatch,
                onUndo = vm::undo,
                onRevertTo = vm::revertTo,
                onLeave = onExit,
                onEnd = { vm.endGame(); onExit() },
                onPrint = vm::requestPrint,
                onMark = vm::markOnBattlefield,
                onViewCard = vm::viewCard,
                onPrintTokens = { oracleId, _ -> vm.showTokensFor(oracleId) },
                onPrintRulings = vm::printRulings,
                onCastCommander = vm::castCommander,
                onReturnToCommand = vm::returnCommanderToCommandZone,
                onClearMessage = vm::clearPrintMessage,
                onPlayDraw = vm::playDraw,
                onPlayDrawSeven = vm::playDrawSeven,
                onPlayShuffle = vm::playShuffle,
                onPlaySwoosh = vm::playSwoosh,
                onPlayBubble = vm::playBubble,
                modifier = modifier,
            )
        }
        if (privacy && state != null) {
            GamePrivacyPanel(
                state = state,
                cardMeta = ui.cardMeta,
                onCommand = vm::dispatch,
                onPrint = vm::requestPrint,
                onMark = vm::markOnBattlefield,
                onView = vm::viewCard,
                onReturnToCommand = vm::returnCommanderToCommandZone,
                onPrintTokens = { oracleId, _ -> vm.showTokensFor(oracleId) },
                onPrintRulings = vm::printRulings,
                onExit = onExitPrivacy,
            )
        }
    }

    viewedCard?.let { card ->
        CardImageDialog(card = card, onDismiss = vm::dismissViewedCard)
    }

    tokenOptions?.let { opts ->
        TokenOptionsDialog(
            options = opts,
            printing = printing,
            onPrint = vm::printToken,
            onDismiss = vm::dismissTokenOptions,
        )
    }

    printChooser?.let { chooser ->
        PrintChooserDialog(
            chooser = chooser,
            onPick = { mode -> vm.printMode(chooser.instanceId, mode) },
            onDismiss = vm::dismissPrintChooser,
        )
    }
}

/**
 * Privacy-mode overlay shown over the game: hides the board but offers buttons to open the
 * Library / Graveyard / Exile with the full zone view, so an opponent can inspect them.
 */
@Composable
private fun GamePrivacyPanel(
    state: GameState,
    cardMeta: Map<String, CardMeta>,
    onCommand: (Command) -> Unit,
    onPrint: (String) -> Unit,
    onMark: (String) -> Unit,
    onView: (String) -> Unit,
    onReturnToCommand: (String) -> Unit,
    onPrintTokens: (String, String) -> Unit,
    onPrintRulings: (String, String) -> Unit,
    onExit: () -> Unit,
) {
    var openZone by remember { mutableStateOf<Zone?>(null) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "PRIVACY MODE",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onExit) { Text("Exit") }
        }
        Spacer(Modifier.height(12.dp))
        val zone = openZone
        if (zone == null) {
            Text(
                "Opponents may inspect these zones:",
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(12.dp))
            listOf(Zone.LIBRARY, Zone.GRAVEYARD, Zone.EXILE).forEach { z ->
                Button(
                    onClick = { openZone = z },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("${zoneEmoji(z)} ${zoneShort(z)} (${state.zone(z).size})") }
                Spacer(Modifier.height(8.dp))
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { openZone = null }) { Text("← Back") }
                Text(
                    "${zoneEmoji(zone)} ${zoneShort(zone)}",
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            key(zone) {
                ZoneCardsView(
                    zone = zone,
                    state = state,
                    cardMeta = cardMeta,
                    onCommand = onCommand,
                    onPrint = onPrint,
                    onMark = onMark,
                    onView = onView,
                    onReturnToCommand = onReturnToCommand,
                    onPrintTokens = onPrintTokens,
                    onPrintRulings = onPrintRulings,
                    onShowThicc = null,
                )
            }
        }
    }
}

private data class Confirm(val message: String, val onConfirm: () -> Unit)

private data class MechanicSession(val mechanic: LibraryMechanic, val count: Int)

/** Which top-of-library count action a number prompt is collecting for. */
private data class CountPrompt(val title: String, val mill: Boolean)

@Composable
private fun GameContent(
    state: GameState,
    deckName: String,
    canUndo: Boolean,
    cardMeta: Map<String, CardMeta>,
    log: List<String>,
    printing: Boolean,
    printMessage: String?,
    onCommand: (Command) -> Unit,
    onUndo: () -> Unit,
    onRevertTo: (Int) -> Unit,
    onLeave: () -> Unit,
    onEnd: () -> Unit,
    onPrint: (String) -> Unit,
    onMark: (String) -> Unit,
    onViewCard: (String) -> Unit,
    onPrintTokens: (String, String) -> Unit,
    onPrintRulings: (String, String) -> Unit,
    onCastCommander: (String) -> Unit,
    onReturnToCommand: (String) -> Unit,
    onClearMessage: () -> Unit,
    onPlayDraw: () -> Unit,
    onPlayDrawSeven: () -> Unit,
    onPlayShuffle: () -> Unit,
    onPlaySwoosh: () -> Unit,
    onPlayBubble: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedZone by remember { mutableStateOf(Zone.HAND) }
    var confirm by remember { mutableStateOf<Confirm?>(null) }
    var showDrawDialog by remember { mutableStateOf(false) }
    var countPrompt by remember { mutableStateOf<CountPrompt?>(null) }
    var pendingMechanic by remember { mutableStateOf<LibraryMechanic?>(null) }
    var active by remember { mutableStateOf<MechanicSession?>(null) }
    var showLog by remember { mutableStateOf(false) }
    var showThicc by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize().padding(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = deckName.ifBlank { "Game" },
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onUndo, enabled = canUndo) { Text("Undo") }
            TextButton(onClick = { showLog = true }) { Text("Log") }
            TextButton(onClick = onLeave) { Text("Home") }
            TextButton(onClick = onEnd) { Text("End") }
        }

        if (printing) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp))
            Text("Printing…", style = MaterialTheme.typography.bodySmall)
        }
        printMessage?.let { msg ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(msg, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                TextButton(onClick = onClearMessage) { Text("Dismiss") }
            }
        }

        Spacer(Modifier.height(8.dp))
        ZoneCounts(state)

        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DrawButton(
                onClick = { onPlayDraw(); onCommand(Command.Draw(1)) },
                onLongClick = { showDrawDialog = true },
            )
            Spacer(Modifier.width(8.dp))
            Button(onClick = {
                confirm = Confirm("Draw 7 cards?") {
                    onPlayDrawSeven()
                    onCommand(Command.Draw(7))
                }
            }) { Text("Draw 7") }
            Spacer(Modifier.width(8.dp))
            Button(onClick = { countPrompt = CountPrompt("Mill", mill = true) }) { Text("Mill") }
            Spacer(Modifier.width(8.dp))
            Button(onClick = { countPrompt = CountPrompt("Exile top", mill = false) }) { Text("Exile top") }
            Spacer(Modifier.width(8.dp))
            Button(onClick = {
                confirm = Confirm("Shuffle your library?") {
                    onPlayShuffle()
                    onCommand(Command.Shuffle)
                }
            }) { Text("Shuffle") }
            Spacer(Modifier.width(8.dp))
            Button(onClick = {
                confirm = Confirm("Discard your hand and draw 7?") {
                    onPlayDrawSeven()
                    onCommand(Command.Wheel)
                }
            }) { Text("Wheel") }
        }

        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LibraryMechanic.entries.forEachIndexed { index, mech ->
                if (index > 0) Spacer(Modifier.width(8.dp))
                Button(onClick = {
                    val fixed = mech.fixedCount
                    if (fixed == null) pendingMechanic = mech else active = MechanicSession(mech, fixed)
                }) { Text(mech.label) }
            }
        }

        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
            Zone.entries.forEach { zone ->
                val count = state.zone(zone).size
                ZoneTab(
                    selected = zone == selectedZone,
                    count = count,
                    label = "${zoneEmoji(zone)} ${zoneShort(zone)} ($count)",
                    onClick = {
                        if (zone == Zone.LIBRARY && selectedZone != Zone.LIBRARY) {
                            confirm = Confirm("View the library? This reveals the order of your library.") {
                                selectedZone = Zone.LIBRARY
                            }
                        } else {
                            selectedZone = zone
                        }
                    },
                )
                Spacer(Modifier.width(8.dp))
            }
        }

        HorizontalDivider(Modifier.padding(vertical = 8.dp))

        if (selectedZone == Zone.COMMAND) {
            CommandersPanel(
                state = state,
                cardMeta = cardMeta,
                onCast = onCastCommander,
                onReturn = onReturnToCommand,
                onView = onViewCard,
            )
            val others = state.zone(Zone.COMMAND).filterNot { it.instanceId in state.commanderIds }
            if (others.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text("Other cards in command zone", style = MaterialTheme.typography.titleSmall)
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(others, key = { it.instanceId }) { card ->
                        CardRow(
                            zone = Zone.COMMAND,
                            instanceId = card.instanceId,
                            oracleId = card.oracleId,
                            name = card.name,
                            printed = state.hasPrinted(card.instanceId),
                            isCommander = false,
                            manaCost = cardMeta[card.oracleId]?.manaCost,
                            onView = onViewCard,
                            onPrint = onPrint,
                            onMark = onMark,
                            onReturnToCommand = onReturnToCommand,
                            createsTokens = cardMeta[card.oracleId]?.createsTokens == true,
                            hasRulings = cardMeta[card.oracleId]?.hasRulings == true,
                            onPrintTokens = { onPrintTokens(card.oracleId, card.name) },
                            onPrintRulings = { onPrintRulings(card.oracleId, card.name) },
                            targets = moveTargets(Zone.COMMAND, card.instanceId),
                            onCommand = onCommand,
                        )
                    }
                }
            }
        } else {
            key(selectedZone) {
                ZoneCardsView(
                    zone = selectedZone,
                    state = state,
                    cardMeta = cardMeta,
                    onCommand = onCommand,
                    onPrint = onPrint,
                    onMark = onMark,
                    onView = onViewCard,
                    onReturnToCommand = onReturnToCommand,
                    onPrintTokens = onPrintTokens,
                    onPrintRulings = onPrintRulings,
                    onShowThicc = { showThicc = true },
                )
            }
        }
    }

    confirm?.let { c ->
        AlertDialog(
            onDismissRequest = { confirm = null },
            title = { Text("Confirm") },
            text = { Text(c.message) },
            confirmButton = {
                TextButton(onClick = {
                    c.onConfirm()
                    confirm = null
                }) { Text("Confirm") }
            },
            dismissButton = {
                TextButton(onClick = { confirm = null }) { Text("Cancel") }
            },
        )
    }

    if (showLog) {
        AlertDialog(
            onDismissRequest = { showLog = false },
            title = { Text("Action log") },
            text = {
                if (log.size <= 1) {
                    Text("No actions yet.")
                } else {
                    Column(Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState())) {
                        log.indices.reversed().forEach { i ->
                            Text(
                                text = log[i],
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        showLog = false
                                        val label = log[i]
                                        confirm = Confirm("Revert to \"$label\"? Later actions are discarded.") {
                                            onRevertTo(i)
                                        }
                                    }
                                    .padding(vertical = 10.dp),
                            )
                            HorizontalDivider()
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showLog = false }) { Text("Close") } },
        )
    }

    if (showThicc) {
        ThiccDialog(
            cardCount = Zone.entries.sumOf { state.zone(it).size },
            onDismiss = { showThicc = false },
        )
    }

    if (showDrawDialog) {
        NumberDialog(
            title = "Draw cards",
            label = "Number of cards",
            onConfirm = { n -> onPlayDraw(); onCommand(Command.Draw(n)) },
            onDismiss = { showDrawDialog = false },
        )
    }

    countPrompt?.let { prompt ->
        NumberDialog(
            title = prompt.title,
            label = "How many from the top",
            onConfirm = { n ->
                onPlayBubble()
                onCommand(if (prompt.mill) Command.MillTop(n) else Command.ExileTop(n))
            },
            onDismiss = { countPrompt = null },
        )
    }

    pendingMechanic?.let { mech ->
        NumberDialog(
            title = mech.label,
            label = if (mech == LibraryMechanic.CONNIVE) "How many to draw (and discard)" else "How many cards (N)",
            onConfirm = { n ->
                if (mech == LibraryMechanic.CONNIVE) onCommand(Command.Draw(n))
                active = MechanicSession(mech, n)
            },
            onDismiss = { pendingMechanic = null },
        )
    }

    active?.let { session ->
        val dismiss = { active = null }
        val topN = state.zone(Zone.LIBRARY).take(session.count)
        when (session.mechanic) {
            LibraryMechanic.SCRY -> ScrySurveilDialog(
                surveil = false,
                count = session.count,
                state = state,
                onCommand = onCommand,
                onView = onViewCard,
                onPlaySwoosh = onPlaySwoosh,
                onDismiss = dismiss,
            )
            LibraryMechanic.SURVEIL -> ScrySurveilDialog(
                surveil = true,
                count = session.count,
                state = state,
                onCommand = onCommand,
                onView = onViewCard,
                onPlaySwoosh = onPlaySwoosh,
                onDismiss = dismiss,
            )
            LibraryMechanic.HIDEAWAY -> SelectableRevealDialog(
                title = "Hideaway ${session.count}",
                hint = "Choose card(s) to print or play; the rest go to the bottom.",
                cards = topN,
                dests = listOf(Dest.PRINT, Dest.BATTLEFIELD, Dest.BOTTOM),
                restDest = Dest.BOTTOM,
                onCommand = onCommand, onPrint = onPrint, onView = onViewCard,
                onPlaySwoosh = onPlaySwoosh, onDismiss = dismiss,
            )
            LibraryMechanic.MANIFEST_DREAD -> SelectableRevealDialog(
                title = "Manifest Dread",
                hint = "Choose one to print or play (a face-down 2/2); the other goes to the graveyard.",
                cards = topN,
                dests = listOf(Dest.PRINT, Dest.BATTLEFIELD, Dest.GRAVEYARD),
                restDest = Dest.GRAVEYARD,
                onCommand = onCommand, onPrint = onPrint, onView = onViewCard,
                onPlaySwoosh = onPlaySwoosh, onDismiss = dismiss,
            )
            LibraryMechanic.DISCOVER, LibraryMechanic.CASCADE -> DiscoverDialog(
                cascade = session.mechanic == LibraryMechanic.CASCADE,
                threshold = session.count,
                state = state,
                cardMeta = cardMeta,
                onCommand = onCommand, onPrint = onPrint, onView = onViewCard,
                onPlaySwoosh = onPlaySwoosh, onDismiss = dismiss,
            )
            LibraryMechanic.CONNIVE -> ConniveDialog(
                count = session.count,
                state = state,
                cardMeta = cardMeta,
                onCommand = onCommand, onView = onViewCard,
                onPlaySwoosh = onPlaySwoosh, onDismiss = dismiss,
            )
        }
    }
}

@Composable
private fun NumberDialog(
    title: String,
    label: String,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var input by remember { mutableStateOf("") }
    val n = input.toIntOrNull() ?: 0
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = input,
                onValueChange = { new -> input = new.filter { it.isDigit() }.take(3) },
                label = { Text(label) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { if (n > 0) { onConfirm(n); onDismiss() } }),
            )
        },
        confirmButton = {
            TextButton(enabled = n > 0, onClick = { onConfirm(n); onDismiss() }) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DrawButton(onClick: () -> Unit, onLongClick: () -> Unit) {
    // Custom (not Material Button) so we can support long-press for "draw N".
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(MaterialTheme.colorScheme.primary)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 24.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "Draw",
            color = MaterialTheme.colorScheme.onPrimary,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

/** A zone tab that briefly pulses whenever its card count increases. */
@Composable
private fun ZoneTab(selected: Boolean, count: Int, label: String, onClick: () -> Unit) {
    val scale = remember { Animatable(1f) }
    var previous by remember { mutableStateOf(count) }
    LaunchedEffect(count) {
        if (count > previous) {
            scale.animateTo(1.18f, tween(120))
            scale.animateTo(1f, tween(150))
        }
        previous = count
    }
    val mod = Modifier.scale(scale.value)
    if (selected) {
        Button(onClick = onClick, modifier = mod) { Text(label) }
    } else {
        OutlinedButton(onClick = onClick, modifier = mod) { Text(label) }
    }
}

private enum class LibraryMechanic(val label: String, val fixedCount: Int?) {
    SCRY("Scry", null),
    SURVEIL("Surveil", null),
    DISCOVER("Discover", null),
    CASCADE("Cascade", null),
    CONNIVE("Connive", null),
    HIDEAWAY("Hideaway", null),
    MANIFEST_DREAD("Manifest Dread", 2),
}

private enum class Dest(val label: String) {
    TOP("To top"),
    BOTTOM("To bottom"),
    HAND("To hand"),
    GRAVEYARD("To graveyard"),
    EXILE("To exile"),
    BATTLEFIELD("To battlefield"),
    PRINT("Print"),
}

/** Applies a destination to an ordered list of instanceIds (first-picked ends on top for TOP). */
private fun applySelected(
    order: List<String>,
    dest: Dest,
    onCommand: (Command) -> Unit,
    onPrint: (String) -> Unit,
) {
    when (dest) {
        Dest.TOP -> order.reversed().forEach { onCommand(Command.MoveToTop(it)) }
        Dest.BOTTOM -> order.forEach { onCommand(Command.MoveToBottom(it)) }
        Dest.HAND -> order.forEach { onCommand(Command.MoveCard(it, Zone.HAND)) }
        Dest.GRAVEYARD -> order.forEach { onCommand(Command.MoveCard(it, Zone.GRAVEYARD)) }
        Dest.EXILE -> order.forEach { onCommand(Command.MoveCard(it, Zone.EXILE)) }
        Dest.BATTLEFIELD -> order.forEach { onCommand(Command.MoveCard(it, Zone.BATTLEFIELD)) }
        Dest.PRINT -> order.forEach { onPrint(it) }
    }
}

private fun batchDests(from: Zone): List<Dest> = buildList {
    if (from != Zone.HAND) add(Dest.HAND)
    if (from != Zone.BATTLEFIELD) add(Dest.BATTLEFIELD)
    if (from != Zone.GRAVEYARD) add(Dest.GRAVEYARD)
    if (from != Zone.EXILE) add(Dest.EXILE)
    add(Dest.TOP)
    add(Dest.BOTTOM)
    add(Dest.PRINT)
}

@Composable
private fun SelectableCardRow(
    name: String,
    orderNumber: Int?,
    checked: Boolean,
    onToggle: () -> Unit,
    onView: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = checked, onCheckedChange = { onToggle() })
        if (orderNumber != null) {
            Text(
                "#$orderNumber",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.width(6.dp))
        }
        Text(
            name,
            modifier = Modifier.weight(1f).clickable { onView() }.padding(vertical = 8.dp),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ZoneBatchBar(count: Int, dests: List<Dest>, onApply: (Dest) -> Unit, onClear: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("$count selected", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            TextButton(onClick = onClear) { Text("Clear") }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            dests.forEach { dest ->
                OutlinedButton(onClick = { onApply(dest) }) { Text(dest.label) }
            }
        }
    }
}

private enum class SlideDir { NEUTRAL, LEFT, RIGHT }

/**
 * A left/right slider: LEFT = lighter grey-blue, RIGHT = dark grey-blue, NEUTRAL = grey. When a
 * side is chosen the destination label ([leftLabel]/[rightLabel]) shows on the track.
 */
@Composable
private fun DirectionSlider(
    dir: SlideDir,
    leftLabel: String,
    rightLabel: String,
    onLeft: () -> Unit,
    onRight: () -> Unit,
) {
    val trackWidth = 132.dp
    val thumb = 26.dp
    val pad = 3.dp
    val targetColor = when (dir) {
        SlideDir.LEFT -> Color(0xFF8FA8C0)
        SlideDir.RIGHT -> Color(0xFF2E4457)
        SlideDir.NEUTRAL -> Color(0xFF3B4A55)
    }
    val color by animateColorAsState(targetColor, label = "sliderColor")
    val targetOffset = when (dir) {
        SlideDir.LEFT -> pad
        SlideDir.RIGHT -> trackWidth - thumb - pad
        SlideDir.NEUTRAL -> (trackWidth - thumb) / 2
    }
    val thumbOffset by animateDpAsState(targetOffset, label = "sliderThumb")
    Box(
        modifier = Modifier
            .width(trackWidth)
            .height(thumb + pad * 2)
            .clip(RoundedCornerShape(percent = 50))
            .background(color),
    ) {
        val label = when (dir) {
            SlideDir.LEFT -> leftLabel
            SlideDir.RIGHT -> rightLabel
            SlideDir.NEUTRAL -> ""
        }
        if (label.isNotEmpty()) {
            Text(
                text = label,
                color = Color.White,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier
                    .align(if (dir == SlideDir.LEFT) Alignment.CenterEnd else Alignment.CenterStart)
                    .padding(horizontal = 12.dp),
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(x = thumbOffset)
                .size(thumb)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.92f)),
        )
        Box(modifier = Modifier.fillMaxHeight().width(trackWidth / 2).align(Alignment.CenterStart).clickable { onLeft() })
        Box(modifier = Modifier.fillMaxHeight().width(trackWidth / 2).align(Alignment.CenterEnd).clickable { onRight() })
    }
}

@Composable
private fun ScrySurveilDialog(
    surveil: Boolean,
    count: Int,
    state: GameState,
    onCommand: (Command) -> Unit,
    onView: (String) -> Unit,
    onPlaySwoosh: () -> Unit,
    onDismiss: () -> Unit,
) {
    val revealed = remember(surveil, count) { state.zone(Zone.LIBRARY).take(count) }
    val topOrder = remember(surveil, count) { mutableStateListOf<String>() }
    val otherOrder = remember(surveil, count) { mutableStateListOf<String>() }
    val hint = if (surveil) {
        "Slide left to keep on top, right to send to the graveyard — order follows the sequence you slide."
    } else {
        "Slide left for the top, right for the bottom — order follows the sequence you slide."
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (surveil) "Surveil $count" else "Scry $count") },
        text = {
            Column {
                Text(hint, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(8.dp))
                Column(Modifier.heightIn(max = 340.dp).verticalScroll(rememberScrollState())) {
                    revealed.forEach { card ->
                        val id = card.instanceId
                        val dir = when {
                            id in topOrder -> SlideDir.LEFT
                            id in otherOrder -> SlideDir.RIGHT
                            else -> SlideDir.NEUTRAL
                        }
                        val number = when (dir) {
                            SlideDir.LEFT -> topOrder.indexOf(id) + 1
                            SlideDir.RIGHT -> otherOrder.indexOf(id) + 1
                            SlideDir.NEUTRAL -> null
                        }
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            DirectionSlider(
                                dir = dir,
                                leftLabel = "Top",
                                rightLabel = if (surveil) "Graveyard" else "Bottom",
                                onLeft = {
                                    if (id in topOrder) topOrder.remove(id)
                                    else { otherOrder.remove(id); topOrder.add(id) }
                                },
                                onRight = {
                                    if (id in otherOrder) otherOrder.remove(id)
                                    else { topOrder.remove(id); otherOrder.add(id) }
                                },
                            )
                            Spacer(Modifier.width(8.dp))
                            if (number != null) {
                                Text(
                                    "#$number",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                Spacer(Modifier.width(6.dp))
                            }
                            Text(card.name, modifier = Modifier.weight(1f).clickable { onView(card.oracleId) })
                        }
                        HorizontalDivider()
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                // First slid left ends on top → dispatch MoveToTop in reverse.
                topOrder.reversed().forEach { onCommand(Command.MoveToTop(it)) }
                // First slid right sits above later ones (bottom) / order into graveyard.
                otherOrder.forEach {
                    onCommand(if (surveil) Command.MoveCard(it, Zone.GRAVEYARD) else Command.MoveToBottom(it))
                }
                onPlaySwoosh()
                onDismiss()
            }) { Text("Done") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SelectableRevealDialog(
    title: String,
    hint: String,
    cards: List<CardInstance>,
    dests: List<Dest>,
    restDest: Dest?,
    onCommand: (Command) -> Unit,
    onPrint: (String) -> Unit,
    onView: (String) -> Unit,
    onPlaySwoosh: () -> Unit,
    onDismiss: () -> Unit,
) {
    val remaining = remember(title) { mutableStateListOf<CardInstance>().apply { addAll(cards) } }
    val selected = remember(title) { mutableStateListOf<String>() }
    val applyDest = { dest: Dest ->
        val order = selected.toList()
        applySelected(order, dest, onCommand, onPrint)
        remaining.removeAll { it.instanceId in order }
        selected.clear()
        onPlaySwoosh()
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                Text(hint, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(8.dp))
                if (remaining.isEmpty()) {
                    Text("All cards resolved.")
                } else {
                    Column(Modifier.heightIn(max = 300.dp).verticalScroll(rememberScrollState())) {
                        remaining.forEach { card ->
                            val idx = selected.indexOf(card.instanceId)
                            SelectableCardRow(
                                name = card.name,
                                orderNumber = if (idx >= 0) idx + 1 else null,
                                checked = idx >= 0,
                                onToggle = {
                                    if (idx >= 0) selected.remove(card.instanceId)
                                    else selected.add(card.instanceId)
                                },
                                onView = { onView(card.oracleId) },
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        dests.forEach { dest ->
                            OutlinedButton(enabled = selected.isNotEmpty(), onClick = { applyDest(dest) }) {
                                Text(dest.label)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
        dismissButton = {
            if (restDest != null && remaining.isNotEmpty()) {
                TextButton(onClick = {
                    applySelected(remaining.map { it.instanceId }, restDest, onCommand, onPrint)
                    remaining.clear()
                    selected.clear()
                    onPlaySwoosh()
                    onDismiss()
                }) { Text("Rest ${restDest.label.replaceFirst("To ", "to ")}") }
            }
        },
    )
}

@Composable
private fun ConniveDialog(
    count: Int,
    state: GameState,
    cardMeta: Map<String, CardMeta>,
    onCommand: (Command) -> Unit,
    onView: (String) -> Unit,
    onPlaySwoosh: () -> Unit,
    onDismiss: () -> Unit,
) {
    val hand = remember(count) { mutableStateListOf<CardInstance>().apply { addAll(state.zone(Zone.HAND)) } }
    val selected = remember(count) { mutableStateListOf<String>() }
    var nonland by remember(count) { mutableStateOf(0) }
    AlertDialog(
        onDismissRequest = { onPlaySwoosh(); onDismiss() },
        title = { Text("Connive $count") },
        text = {
            Column {
                Text(
                    "You drew $count. Select card(s) to discard. Each nonland discarded adds a " +
                        "+1/+1 counter to the conniving permanent.",
                    style = MaterialTheme.typography.bodySmall,
                )
                if (nonland > 0) {
                    Spacer(Modifier.height(4.dp))
                    Text("Add $nonland +1/+1 counter(s).", style = MaterialTheme.typography.bodyMedium)
                }
                Spacer(Modifier.height(6.dp))
                if (hand.isEmpty()) {
                    Text("Your hand is empty.")
                } else {
                    Column(Modifier.heightIn(max = 300.dp).verticalScroll(rememberScrollState())) {
                        hand.forEach { card ->
                            SelectableCardRow(
                                name = card.name,
                                orderNumber = null,
                                checked = card.instanceId in selected,
                                onToggle = {
                                    if (card.instanceId in selected) selected.remove(card.instanceId)
                                    else selected.add(card.instanceId)
                                },
                                onView = { onView(card.oracleId) },
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(enabled = selected.isNotEmpty(), onClick = {
                        val order = selected.toList()
                        order.forEach { id ->
                            onCommand(Command.MoveCard(id, Zone.GRAVEYARD))
                            val oracleId = hand.firstOrNull { it.instanceId == id }?.oracleId
                            val meta = oracleId?.let { cardMeta[it] }
                            if (meta != null && CardType.LAND !in meta.types) nonland++
                        }
                        hand.removeAll { it.instanceId in order }
                        selected.clear()
                        onPlaySwoosh()
                    }) { Text("Discard") }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onPlaySwoosh(); onDismiss() }) { Text("Done") } },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DiscoverDialog(
    cascade: Boolean,
    threshold: Int,
    state: GameState,
    cardMeta: Map<String, CardMeta>,
    onCommand: (Command) -> Unit,
    onPrint: (String) -> Unit,
    onView: (String) -> Unit,
    onPlaySwoosh: () -> Unit,
    onDismiss: () -> Unit,
) {
    val library = remember(cascade, threshold) { state.zone(Zone.LIBRARY) }
    val hitIndex = remember(cascade, threshold) {
        library.indexOfFirst { card ->
            val meta = cardMeta[card.oracleId]
            val nonland = meta != null && CardType.LAND !in meta.types
            val mv = meta?.manaValue ?: 0
            nonland && (if (cascade) mv < threshold else mv <= threshold)
        }
    }
    val passed = if (hitIndex >= 0) library.take(hitIndex) else library
    val hit = if (hitIndex >= 0) library[hitIndex] else null
    val sendPassedToBottom = { passed.forEach { onCommand(Command.MoveToBottom(it.instanceId)) } }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (cascade) "Cascade (< $threshold)" else "Discover $threshold") },
        text = {
            Column {
                Text(
                    if (cascade) "Reveal from the top until a nonland with mana value less than $threshold."
                    else "Reveal from the top until a nonland with mana value $threshold or less.",
                    style = MaterialTheme.typography.bodySmall,
                )
                if (passed.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "To the bottom: ${passed.joinToString { it.name }}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(8.dp))
                if (hit == null) {
                    Text("No qualifying card found.")
                } else {
                    Text("Found: ${hit.name}", modifier = Modifier.clickable { onView(hit.oracleId) })
                    Spacer(Modifier.height(6.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(onClick = {
                            onPrint(hit.instanceId); sendPassedToBottom(); onPlaySwoosh(); onDismiss()
                        }) { Text("Print") }
                        OutlinedButton(onClick = {
                            onCommand(Command.MoveCard(hit.instanceId, Zone.BATTLEFIELD))
                            sendPassedToBottom(); onPlaySwoosh(); onDismiss()
                        }) { Text("Play") }
                        if (cascade) {
                            OutlinedButton(onClick = {
                                onCommand(Command.MoveToBottom(hit.instanceId))
                                sendPassedToBottom(); onPlaySwoosh(); onDismiss()
                            }) { Text("To bottom") }
                        } else {
                            OutlinedButton(onClick = {
                                onCommand(Command.MoveCard(hit.instanceId, Zone.HAND))
                                sendPassedToBottom(); onPlaySwoosh(); onDismiss()
                            }) { Text("To hand") }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (hit == null) {
                TextButton(onClick = { sendPassedToBottom(); onPlaySwoosh(); onDismiss() }) { Text("All to bottom") }
            } else {
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        },
    )
}

@Composable
private fun CardRow(
    zone: Zone,
    instanceId: String,
    oracleId: String,
    name: String,
    printed: Boolean,
    isCommander: Boolean,
    manaCost: String? = null,
    onView: (String) -> Unit,
    onPrint: (String) -> Unit,
    onMark: (String) -> Unit,
    onReturnToCommand: (String) -> Unit,
    createsTokens: Boolean = false,
    hasRulings: Boolean = false,
    onPrintTokens: () -> Unit = {},
    onPrintRulings: () -> Unit = {},
    targets: List<Pair<String, Command>>,
    onCommand: (Command) -> Unit,
    selectable: Boolean = false,
    checked: Boolean = false,
    orderNumber: Int? = null,
    onToggle: () -> Unit = {},
) {
    var expanded by remember { mutableStateOf(false) }
    val displayName = buildString {
        if (isCommander) append("★ ")
        append(name)
        if (printed) append("  ✓printed")
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (selectable) {
            Checkbox(checked = checked, onCheckedChange = { onToggle() })
            if (orderNumber != null) {
                Text(
                    "#$orderNumber",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.width(4.dp))
            }
        }
        Text(
            text = displayName,
            modifier = Modifier
                .weight(1f)
                .clickable { onView(oracleId) }
                .padding(vertical = 8.dp),
        )
        if (!manaCost.isNullOrBlank()) {
            Text(
                text = ManaText.toAscii(manaCost),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp),
            )
        }
        Box {
            TextButton(onClick = { expanded = true }) { Text("Actions") }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                CardActionsItems(
                    zone = zone,
                    instanceId = instanceId,
                    oracleId = oracleId,
                    printed = printed,
                    isCommander = isCommander,
                    createsTokens = createsTokens,
                    hasRulings = hasRulings,
                    targets = targets,
                    onView = onView,
                    onPrint = onPrint,
                    onMark = onMark,
                    onReturnToCommand = onReturnToCommand,
                    onPrintTokens = onPrintTokens,
                    onPrintRulings = onPrintRulings,
                    onCommand = onCommand,
                    close = { expanded = false },
                )
            }
        }
    }
}

/** The card action menu items, shared by the list rows and the visual view. Render inside a menu. */
@Composable
private fun CardActionsItems(
    zone: Zone,
    instanceId: String,
    oracleId: String,
    printed: Boolean,
    isCommander: Boolean,
    createsTokens: Boolean,
    hasRulings: Boolean,
    targets: List<Pair<String, Command>>,
    onView: (String) -> Unit,
    onPrint: (String) -> Unit,
    onMark: (String) -> Unit,
    onReturnToCommand: (String) -> Unit,
    onPrintTokens: () -> Unit,
    onPrintRulings: () -> Unit,
    onCommand: (Command) -> Unit,
    close: () -> Unit,
) {
    if (isCommander && zone != Zone.COMMAND) {
        DropdownMenuItem(
            text = { Text("Return to command zone") },
            onClick = { onReturnToCommand(instanceId); close() },
        )
    }
    if (zone != Zone.BATTLEFIELD) {
        if (printed) {
            // A physical copy already exists — place this duplicate instead of reprinting.
            DropdownMenuItem(
                text = { Text("Place (already printed)") },
                onClick = { onMark(instanceId); close() },
            )
            DropdownMenuItem(
                text = { Text("Print anyway…") },
                onClick = { onPrint(instanceId); close() },
            )
        } else {
            DropdownMenuItem(
                text = { Text("Print…") },
                onClick = { onPrint(instanceId); close() },
            )
        }
    }
    DropdownMenuItem(
        text = { Text("View card image") },
        onClick = { onView(oracleId); close() },
    )
    if (createsTokens) {
        DropdownMenuItem(
            text = { Text("Print tokens…") },
            onClick = { onPrintTokens(); close() },
        )
    }
    if (hasRulings) {
        DropdownMenuItem(
            text = { Text("Print rulings") },
            onClick = { onPrintRulings(); close() },
        )
    }
    targets.forEach { (label, command) ->
        DropdownMenuItem(
            text = { Text(label) },
            onClick = { onCommand(command); close() },
        )
    }
}

@Composable
private fun CommandersPanel(
    state: GameState,
    cardMeta: Map<String, CardMeta>,
    onCast: (String) -> Unit,
    onReturn: (String) -> Unit,
    onView: (String) -> Unit,
) {
    Text("Commanders", style = MaterialTheme.typography.titleMedium)
    Spacer(Modifier.height(4.dp))
    state.commanderIds.forEach { id ->
        val located = state.locate(id) ?: return@forEach
        val (zone, instance) = located
        val tax = state.commanderTax(id)
        val cost = cardMeta[instance.oracleId]?.manaCost
            ?.takeIf { it.isNotBlank() }
            ?.let { "  ${ManaText.toAscii(it)}" }
            .orEmpty()
        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
            Text(
                text = "★ ${instance.name}$cost",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.clickable { onView(instance.oracleId) },
            )
            Text(
                text = "In ${zoneLabel(zone)}" + if (tax > 0) "  ·  tax +$tax" else "",
                style = MaterialTheme.typography.bodySmall,
            )
            Spacer(Modifier.height(4.dp))
            if (zone == Zone.COMMAND) {
                Button(onClick = { onCast(id) }) {
                    Text(if (tax > 0) "Cast to battlefield (+$tax)" else "Cast to battlefield")
                }
            } else {
                OutlinedButton(onClick = { onReturn(id) }) { Text("Return to command zone") }
            }
        }
        HorizontalDivider()
    }
}

/**
 * The full contents of one non-command zone: the Search / Filter / Visual (/ 🍑) toggles, the
 * optional search + filter panels, the batch-select bar, and the card list or visual carousel.
 * Owns its own per-zone UI state; wrap the call in `key(zone)` to reset it when the zone changes.
 * Reused by the normal tab view and the privacy-mode zone viewer ([onShowThicc] null hides 🍑).
 */
@Composable
private fun ZoneCardsView(
    zone: Zone,
    state: GameState,
    cardMeta: Map<String, CardMeta>,
    onCommand: (Command) -> Unit,
    onPrint: (String) -> Unit,
    onMark: (String) -> Unit,
    onView: (String) -> Unit,
    onReturnToCommand: (String) -> Unit,
    onPrintTokens: (String, String) -> Unit,
    onPrintRulings: (String, String) -> Unit,
    onShowThicc: (() -> Unit)?,
) {
    var showSearch by remember { mutableStateOf(false) }
    var showFilters by remember { mutableStateOf(false) }
    var showVisual by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedTypes by remember { mutableStateOf(emptySet<CardType>()) }
    var selectedRarities by remember { mutableStateOf(emptySet<String>()) }
    val zoneSelected = remember { mutableStateListOf<String>() }

    val cards = state.zone(zone)
    if (cards.isNotEmpty()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { showSearch = !showSearch }) {
                Icon(
                    Icons.Filled.Search,
                    contentDescription = "Search",
                    tint = if (showSearch) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = { showFilters = !showFilters }) {
                Icon(
                    Icons.Filled.FilterList,
                    contentDescription = "Filters",
                    tint = if (showFilters) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = { showVisual = !showVisual }) {
                Icon(
                    Icons.Filled.ViewCarousel,
                    contentDescription = "Visual card view",
                    tint = if (showVisual) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (onShowThicc != null && zone == Zone.LIBRARY) {
                IconButton(onClick = onShowThicc) {
                    Text("🍑", style = MaterialTheme.typography.titleLarge)
                }
            }
        }
        if (showSearch) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search ${zoneShort(zone)}") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
        }
        if (showFilters) {
            ZoneFilters(
                cards = cards,
                cardMeta = cardMeta,
                selectedTypes = selectedTypes,
                onToggleType = { selectedTypes = selectedTypes.toggle(it) },
                selectedRarities = selectedRarities,
                onToggleRarity = { selectedRarities = selectedRarities.toggle(it) },
            )
            Spacer(Modifier.height(8.dp))
        }
    }

    val filtered = remember(cards, searchQuery, selectedTypes, selectedRarities, cardMeta) {
        cards.filter { card ->
            val meta = cardMeta[card.oracleId]
            val matchesSearch = searchQuery.isBlank() ||
                card.name.contains(searchQuery.trim(), ignoreCase = true)
            val matchesType = selectedTypes.isEmpty() ||
                (meta != null && meta.types.any { it in selectedTypes })
            val matchesRarity = selectedRarities.isEmpty() ||
                (meta?.rarity != null && meta.rarity in selectedRarities)
            matchesSearch && matchesType && matchesRarity
        }
    }

    if (zoneSelected.isNotEmpty()) {
        ZoneBatchBar(
            count = zoneSelected.size,
            dests = batchDests(zone),
            onApply = { dest ->
                applySelected(zoneSelected.toList(), dest, onCommand, onPrint)
                zoneSelected.clear()
            },
            onClear = { zoneSelected.clear() },
        )
        Spacer(Modifier.height(8.dp))
    }

    when {
        cards.isEmpty() -> Text("No cards in ${zoneShort(zone)}.")
        filtered.isEmpty() -> Text("No cards match the current filters.")
        showVisual -> VisualCardScroll(
            zone = zone,
            cards = filtered,
            state = state,
            cardMeta = cardMeta,
            onCommand = onCommand,
            onPrint = onPrint,
            onMark = onMark,
            onView = onView,
            onReturnToCommand = onReturnToCommand,
            onPrintTokens = onPrintTokens,
            onPrintRulings = onPrintRulings,
        )
        else -> {
            val allSelected = filtered.isNotEmpty() &&
                filtered.all { it.instanceId in zoneSelected }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = allSelected,
                    onCheckedChange = { checkAll ->
                        if (checkAll) {
                            filtered.forEach {
                                if (it.instanceId !in zoneSelected) zoneSelected.add(it.instanceId)
                            }
                        } else {
                            zoneSelected.clear()
                        }
                    },
                )
                Text("Select all (${filtered.size})", style = MaterialTheme.typography.bodyMedium)
            }
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(filtered, key = { it.instanceId }) { card ->
                    val idx = zoneSelected.indexOf(card.instanceId)
                    CardRow(
                        zone = zone,
                        instanceId = card.instanceId,
                        oracleId = card.oracleId,
                        name = card.name,
                        printed = state.hasPrinted(card.instanceId),
                        isCommander = state.isCommander(card.instanceId),
                        manaCost = cardMeta[card.oracleId]?.manaCost,
                        onView = onView,
                        onPrint = onPrint,
                        onMark = onMark,
                        onReturnToCommand = onReturnToCommand,
                        createsTokens = cardMeta[card.oracleId]?.createsTokens == true,
                        hasRulings = cardMeta[card.oracleId]?.hasRulings == true,
                        onPrintTokens = { onPrintTokens(card.oracleId, card.name) },
                        onPrintRulings = { onPrintRulings(card.oracleId, card.name) },
                        targets = moveTargets(zone, card.instanceId),
                        onCommand = onCommand,
                        selectable = true,
                        checked = idx >= 0,
                        orderNumber = if (idx >= 0) idx + 1 else null,
                        onToggle = {
                            if (idx >= 0) zoneSelected.remove(card.instanceId)
                            else zoneSelected.add(card.instanceId)
                        },
                    )
                }
            }
        }
    }
}

/**
 * A swipeable carousel of card images for the current zone. Uses a pager so only the focused card
 * and its neighbors are composed, and Coil loads/caches each image on demand — so a large zone
 * never loads every image at once. Tapping a card opens its Actions menu.
 */
@Composable
private fun VisualCardScroll(
    zone: Zone,
    cards: List<CardInstance>,
    state: GameState,
    cardMeta: Map<String, CardMeta>,
    onCommand: (Command) -> Unit,
    onPrint: (String) -> Unit,
    onMark: (String) -> Unit,
    onView: (String) -> Unit,
    onReturnToCommand: (String) -> Unit,
    onPrintTokens: (String, String) -> Unit,
    onPrintRulings: (String, String) -> Unit,
) {
    val pagerState = rememberPagerState { cards.size }
    val scope = rememberCoroutineScope()
    var actionsFor by remember { mutableStateOf<CardInstance?>(null) }
    Column(modifier = Modifier.fillMaxSize()) {
        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = 48.dp),
            pageSpacing = 12.dp,
            modifier = Modifier.weight(1f),
        ) { page ->
            val card = cards[page]
            val imageUrl = cardMeta[card.oracleId]?.imageUrl
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 8.dp)
                    .clickable { actionsFor = card },
                contentAlignment = Alignment.Center,
            ) {
                if (imageUrl != null) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = card.name,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Text(
                        card.name,
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(16.dp),
                    )
                }
                DropdownMenu(
                    expanded = actionsFor?.instanceId == card.instanceId,
                    onDismissRequest = { actionsFor = null },
                ) {
                    CardActionsItems(
                        zone = zone,
                        instanceId = card.instanceId,
                        oracleId = card.oracleId,
                        printed = state.hasPrinted(card.instanceId),
                        isCommander = state.isCommander(card.instanceId),
                        createsTokens = cardMeta[card.oracleId]?.createsTokens == true,
                        hasRulings = cardMeta[card.oracleId]?.hasRulings == true,
                        targets = moveTargets(zone, card.instanceId),
                        onView = onView,
                        onPrint = onPrint,
                        onMark = onMark,
                        onReturnToCommand = onReturnToCommand,
                        onPrintTokens = { onPrintTokens(card.oracleId, card.name) },
                        onPrintRulings = { onPrintRulings(card.oracleId, card.name) },
                        onCommand = onCommand,
                        close = { actionsFor = null },
                    )
                }
            }
        }
        Text(
            text = "${pagerState.currentPage + 1} / ${cards.size}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.CenterHorizontally).padding(8.dp),
        )
        if (cards.size > 1) {
            Slider(
                value = pagerState.currentPage.toFloat(),
                onValueChange = { scope.launch { pagerState.scrollToPage(it.roundToInt()) } },
                valueRange = 0f..(cards.size - 1).toFloat(),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            )
        }
    }
}

@Composable
private fun CardImageDialog(card: ViewedCard, onDismiss: () -> Unit) {
    val faces = card.faces
    var index by remember(card) { mutableStateOf(0) }
    val face = faces.getOrNull(index) ?: faces.firstOrNull()
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        if (faces.size > 1) index = (index + 1) % faces.size else onDismiss()
                    },
                contentAlignment = Alignment.Center,
            ) {
                if (face?.imageUrl != null) {
                    AsyncImage(
                        model = face.imageUrl,
                        contentDescription = face.name,
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    Text(
                        "No image available for ${face?.name ?: "card"}.",
                        modifier = Modifier.padding(24.dp),
                    )
                }
            }
            if (faces.size > 1) {
                Text(
                    "${face?.name.orEmpty()} — tap to flip",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(8.dp),
                )
            }
        }
    }
}

@Composable
private fun PrintChooserDialog(
    chooser: PrintChooser,
    onPick: (PrintMode) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Print — ${chooser.sourceName}") },
        text = {
            Column {
                chooser.modes.forEach { mode ->
                    TextButton(
                        onClick = { onPick(mode) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            "${mode.label}  →  ${destinationWord(mode.destination)}",
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

private fun destinationWord(zone: Zone): String = when (zone) {
    Zone.BATTLEFIELD -> "battlefield"
    Zone.GRAVEYARD -> "graveyard"
    Zone.EXILE -> "exile"
    Zone.HAND -> "hand"
    Zone.LIBRARY -> "library"
    Zone.COMMAND -> "command zone"
}

@Composable
private fun TokenOptionsDialog(
    options: TokenOptions,
    printing: Boolean,
    onPrint: (TokenCardEntity) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (options.sourceName.isBlank()) "Tokens" else "Tokens: ${options.sourceName}")
        },
        text = {
            when {
                options.loading -> Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Loading tokens…")
                }

                options.error != null -> Text("Couldn't load tokens: ${options.error}")
                options.tokens.isEmpty() -> Text("This card doesn't create any tokens.")
                else -> LazyColumn {
                    items(options.tokens, key = { it.id }) { token ->
                        TokenRow(token = token, enabled = !printing, onPrint = { onPrint(token) })
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}

@Composable
private fun ZoneCounts(state: GameState) {
    Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
        Zone.entries.forEach { zone ->
            Text(
                text = "${zoneShort(zone)} ${state.zone(zone).size}",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(end = 12.dp),
            )
        }
    }
}

@Composable
private fun CenteredSpinner() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) { CircularProgressIndicator() }
}

@Composable
private fun CenteredMessage(message: String, onExit: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(message)
        Spacer(Modifier.height(8.dp))
        Button(onClick = onExit) { Text("Back") }
    }
}

private fun zoneShort(zone: Zone): String = when (zone) {
    Zone.LIBRARY -> "LIB"
    Zone.HAND -> "HAND"
    Zone.GRAVEYARD -> "GY"
    Zone.EXILE -> "EXILE"
    Zone.BATTLEFIELD -> "BF"
    Zone.COMMAND -> "CMD"
}

private fun zoneEmoji(zone: Zone): String = when (zone) {
    Zone.LIBRARY -> "📚"
    Zone.HAND -> "✋"
    Zone.GRAVEYARD -> "🪦"
    Zone.EXILE -> "💀"
    Zone.BATTLEFIELD -> "⚔️"
    Zone.COMMAND -> "🎖️"
}

private fun zoneLabel(zone: Zone): String = when (zone) {
    Zone.LIBRARY -> "library"
    Zone.HAND -> "hand"
    Zone.GRAVEYARD -> "graveyard"
    Zone.EXILE -> "exile"
    Zone.BATTLEFIELD -> "battlefield"
    Zone.COMMAND -> "command zone"
}

private fun moveTargets(from: Zone, instanceId: String): List<Pair<String, Command>> {
    val targets = mutableListOf<Pair<String, Command>>()
    if (from != Zone.HAND) targets += "To hand" to Command.MoveCard(instanceId, Zone.HAND)
    if (from != Zone.BATTLEFIELD) targets += "To battlefield (no print)" to Command.MoveCard(instanceId, Zone.BATTLEFIELD)
    if (from != Zone.GRAVEYARD) targets += "To graveyard" to Command.MoveCard(instanceId, Zone.GRAVEYARD)
    if (from != Zone.EXILE) targets += "To exile" to Command.MoveCard(instanceId, Zone.EXILE)
    targets += "Library (top)" to Command.MoveToTop(instanceId)
    targets += "Library (bottom)" to Command.MoveToBottom(instanceId)
    return targets
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ZoneFilters(
    cards: List<CardInstance>,
    cardMeta: Map<String, CardMeta>,
    selectedTypes: Set<CardType>,
    onToggleType: (CardType) -> Unit,
    selectedRarities: Set<String>,
    onToggleRarity: (String) -> Unit,
) {
    val metas = cards.mapNotNull { cardMeta[it.oracleId] }
    val types = CardType.entries.filter { type -> metas.any { type in it.types } }
    val rarities = RARITY_ORDER.filter { rarity -> metas.any { it.rarity == rarity } }
    if (types.isNotEmpty()) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            types.forEach { type ->
                FilterChip(
                    selected = type in selectedTypes,
                    onClick = { onToggleType(type) },
                    label = { Text(type.label) },
                )
            }
        }
    }
    if (rarities.isNotEmpty()) {
        Spacer(Modifier.height(4.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            rarities.forEach { rarity ->
                FilterChip(
                    selected = rarity in selectedRarities,
                    onClick = { onToggleRarity(rarity) },
                    label = { Text(rarity.replaceFirstChar { it.uppercase() }) },
                )
            }
        }
    } else if (metas.isNotEmpty() && metas.all { it.rarity == null }) {
        Text(
            "Update the card database (Home) to filter by rarity.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private val RARITY_ORDER = listOf("common", "uncommon", "rare", "mythic", "special", "bonus")

private fun <T> Set<T>.toggle(item: T): Set<T> = if (item in this) this - item else this + item

@Composable
private fun ThiccDialog(cardCount: Int, onDismiss: () -> Unit) {
    // Average MTG card is ~0.31 mm thick; 304.8 mm per foot.
    val feet = cardCount * 0.31 / 304.8
    val feetText = "%.2f".format(feet)
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Nice") } },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Image(
                    painter = painterResource(R.drawable.thicc),
                    contentDescription = "Deck thiccness",
                    modifier = Modifier.size(200.dp),
                )
                Spacer(Modifier.height(12.dp))
                WobblyText("This deck is $feetText feet thick.")
                if (feet > 1.0) {
                    Spacer(Modifier.height(8.dp))
                    RainbowWobblyText("EXTRA THICC")
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    "$cardCount cards × ~0.31 mm each",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
    )
}

@Composable
private fun WobblyText(text: String) {
    val transition = rememberInfiniteTransition(label = "wobble")
    val rotation by transition.animateFloat(
        initialValue = -4f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "rotation",
    )
    val pulse by transition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "pulse",
    )
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.graphicsLayer {
            rotationZ = rotation
            scaleX = pulse
            scaleY = pulse
        },
    )
}

@Composable
private fun RainbowWobblyText(text: String) {
    val transition = rememberInfiniteTransition(label = "extraThicc")
    val hue by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(600, easing = LinearEasing), RepeatMode.Restart),
        label = "hue",
    )
    val rotation by transition.animateFloat(
        initialValue = -9f,
        targetValue = 9f,
        animationSpec = infiniteRepeatable(tween(200), RepeatMode.Reverse),
        label = "rotation",
    )
    val pulse by transition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.14f,
        animationSpec = infiniteRepeatable(tween(230), RepeatMode.Reverse),
        label = "pulse",
    )
    Text(
        text = text,
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        color = Color.hsv(hue, 1f, 1f),
        modifier = Modifier.graphicsLayer {
            rotationZ = rotation
            scaleX = pulse
            scaleY = pulse
        },
    )
}
