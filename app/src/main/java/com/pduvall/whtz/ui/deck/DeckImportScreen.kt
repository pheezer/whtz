package com.pduvall.whtz.ui.deck

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pduvall.whtz.data.deck.DeckImportOutcome
import com.pduvall.whtz.data.deck.MAX_COPIES_PER_CARD
import com.pduvall.whtz.data.deck.isEligibleCommander

@Composable
fun DeckImportScreen(
    onBack: () -> Unit,
    onDeckSaved: (Long) -> Unit,
    modifier: Modifier = Modifier,
    deckId: Long? = null,
    vm: DeckImportViewModel = hiltViewModel(),
) {
    LaunchedEffect(deckId) { if (deckId != null) vm.startEdit(deckId) }
    val state by vm.state.collectAsStateWithLifecycle()
    val initialName by vm.initialName.collectAsStateWithLifecycle()
    val initialText by vm.initialText.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Text(
            if (deckId != null) "Edit deck" else "Import a deck",
            style = MaterialTheme.typography.headlineSmall,
        )
        Spacer(Modifier.height(12.dp))

        when (val s = state) {
            DeckImportViewModel.State.Editing, is DeckImportViewModel.State.Error -> {
                EditingForm(
                    initialName = initialName,
                    initialText = initialText,
                    errorMessage = (s as? DeckImportViewModel.State.Error)?.message,
                    onImport = vm::import,
                    onBack = onBack,
                )
            }

            DeckImportViewModel.State.Importing -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(12.dp))
                    Text("Resolving cards…")
                }
            }

            is DeckImportViewModel.State.Imported -> {
                ImportedSummary(
                    outcome = s.outcome,
                    onSave = { commanderIds ->
                        vm.save(s.outcome.deck, commanderIds) { id ->
                            if (deckId != null) onBack() else onDeckSaved(id)
                        }
                    },
                    onEditAgain = vm::backToEditing,
                )
            }
        }
    }
}

@Composable
private fun EditingForm(
    initialName: String,
    initialText: String,
    errorMessage: String?,
    onImport: (String, String) -> Unit,
    onBack: () -> Unit,
) {
    var name by rememberSaveable(initialName) { mutableStateOf(initialName) }
    var text by rememberSaveable(initialText) { mutableStateOf(initialText) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Deck name (optional)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            label = { Text("Paste decklist") },
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp),
        )
        if (errorMessage != null) {
            Spacer(Modifier.height(8.dp))
            Text(errorMessage, color = MaterialTheme.colorScheme.error)
        }
        Spacer(Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onBack) { Text("Back") }
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = { onImport(name, text) },
                enabled = text.isNotBlank(),
            ) { Text("Import") }
        }
    }
}

@Composable
private fun ImportedSummary(
    outcome: DeckImportOutcome,
    onSave: (Set<String>) -> Unit,
    onEditAgain: () -> Unit,
) {
    var selected by remember { mutableStateOf(emptySet<String>()) }
    var query by remember { mutableStateOf("") }

    val eligible = remember(outcome) { outcome.deck.cards.filter { it.card.isEligibleCommander() } }
    val candidates = remember(outcome) { eligible.ifEmpty { outcome.deck.cards } }
    val nameByOracle = remember(outcome) { outcome.deck.cards.associate { it.oracleId to it.name } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        Text(
            "Resolved ${outcome.resolvedCardCount} of ${outcome.totalCardCount} cards " +
                "(${outcome.deck.cards.size} distinct).",
            style = MaterialTheme.typography.bodyLarge,
        )
        if (outcome.cappedCount > 0) {
            Spacer(Modifier.height(8.dp))
            Text(
                "${outcome.cappedCount} entr${if (outcome.cappedCount == 1) "y was" else "ies were"} " +
                    "capped to $MAX_COPIES_PER_CARD copies.",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        if (outcome.unresolved.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text(
                "Couldn't find ${outcome.unresolved.size}:",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.titleSmall,
            )
            outcome.unresolved.forEach { entry -> Text("• ${entry.quantity} ${entry.name}") }
        }

        Spacer(Modifier.height(16.dp))
        Text("Commander (required — pick 1 or 2)", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(4.dp))
        if (selected.isEmpty()) {
            Text(
                "Select your commander below.",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        } else {
            selected.forEach { oracleId ->
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("★ ${nameByOracle[oracleId] ?: oracleId}", modifier = Modifier.weight(1f))
                    TextButton(onClick = { selected = selected - oracleId }) { Text("Remove") }
                }
            }
        }

        if (selected.size < 2) {
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = {
                    Text(
                        if (eligible.isEmpty()) "No legendary detected — search any card"
                        else "Search eligible commanders",
                    )
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            val matches = candidates.filter {
                it.oracleId !in selected && (query.isBlank() || it.name.contains(query.trim(), ignoreCase = true))
            }.take(8)
            matches.forEach { candidate ->
                Text(
                    text = candidate.name,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selected = selected + candidate.oracleId; query = "" }
                        .padding(vertical = 10.dp),
                )
            }
            if (query.isNotBlank() && matches.isEmpty()) {
                Text("No matches.", style = MaterialTheme.typography.bodySmall)
            }
        }

        Spacer(Modifier.height(20.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onEditAgain) { Text("Edit list") }
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = { onSave(selected) },
                enabled = selected.isNotEmpty() && outcome.deck.cards.isNotEmpty(),
            ) { Text("Save deck") }
        }
    }
}
