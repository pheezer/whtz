package com.pduvall.whtz.ui.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pduvall.whtz.data.local.entity.DeckEntity
import com.pduvall.whtz.ui.common.WhtzTitle

@Composable
fun HomeScreen(
    cardCount: Int,
    onImport: () -> Unit,
    onResume: () -> Unit,
    onPlayDeck: (Long) -> Unit,
    onEditDeck: (Long) -> Unit,
    onPrinterSettings: () -> Unit,
    onTokens: () -> Unit,
    modifier: Modifier = Modifier,
    vm: HomeViewModel = hiltViewModel(),
) {
    LaunchedEffect(Unit) { vm.refresh() }
    val state by vm.state.collectAsStateWithLifecycle()
    val update by vm.update.collectAsStateWithLifecycle()
    var deckPendingDelete by remember { mutableStateOf<DeckEntity?>(null) }

    Column(modifier = modifier.fillMaxSize().padding(20.dp)) {
        WhtzTitle(modifier = Modifier.align(Alignment.CenterHorizontally))
        Spacer(Modifier.height(4.dp))
        Text(
            "A MTG card printer and companion app for unnecessarily large decks",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "Offline card database: $cardCount cards",
            style = MaterialTheme.typography.bodySmall,
        )
        when (val u = update) {
            is HomeViewModel.Update.Running -> {
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Text(
                    "Updating ${u.phase}… ${u.imported}",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            is HomeViewModel.Update.Done -> Text(
                "Card database updated (${u.count} cards).",
                style = MaterialTheme.typography.bodySmall,
            )
            is HomeViewModel.Update.Failed -> Text(
                "Update failed: ${u.message}",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
            HomeViewModel.Update.Idle -> {}
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = { vm.updateDatabase() },
            enabled = update !is HomeViewModel.Update.Running,
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Update card database") }
        Spacer(Modifier.height(20.dp))

        Button(onClick = onImport, modifier = Modifier.fillMaxWidth()) { Text("Import a deck") }
        if (state.hasSavedGame) {
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onResume, modifier = Modifier.fillMaxWidth()) {
                Text("Resume game in progress")
            }
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onTokens, modifier = Modifier.fillMaxWidth()) {
            Text("Token library")
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onPrinterSettings, modifier = Modifier.fillMaxWidth()) {
            Text("Printer settings")
        }

        Spacer(Modifier.height(20.dp))
        HorizontalDivider()
        Spacer(Modifier.height(12.dp))
        Text(
            "Your Decks",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
        Spacer(Modifier.height(8.dp))

        if (state.decks.isEmpty()) {
            Text("No saved decks yet — import one to start.")
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(state.decks, key = { it.id }) { deck ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(deck.name, modifier = Modifier.weight(1f))
                        TextButton(onClick = { onPlayDeck(deck.id) }) { Text("New game") }
                        TextButton(onClick = { onEditDeck(deck.id) }) { Text("Edit") }
                        TextButton(onClick = { deckPendingDelete = deck }) { Text("Delete") }
                    }
                }
            }
        }
    }

    deckPendingDelete?.let { deck ->
        AlertDialog(
            onDismissRequest = { deckPendingDelete = null },
            title = { Text("Delete deck") },
            text = { Text("Delete \"${deck.name}\"? This can't be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    vm.deleteDeck(deck.id)
                    deckPendingDelete = null
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { deckPendingDelete = null }) { Text("Cancel") }
            },
        )
    }
}
