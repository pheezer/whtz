package com.pduvall.whtz.ui.importer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pduvall.whtz.ui.common.WhtzTitle

/**
 * First-run gate: downloads the offline card database. Shown until the DB is populated; once
 * [ImportViewModel.State.Ready], the caller swaps in the app's home content.
 */
@Composable
fun ImportScreen(
    state: ImportViewModel.State,
    onStartImport: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        WhtzTitle()
        when (state) {
            ImportViewModel.State.Checking -> {
                Spacer(Modifier.height(48.dp))
                CircularProgressIndicator()
                Spacer(Modifier.height(16.dp))
                Text("Checking card database…")
            }

            ImportViewModel.State.NeedsImport -> {
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "The app needs to download the Magic card database once " +
                        "(about 30,000 cards, plus rulings and tokens). It works offline afterwards.",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(48.dp))
                Button(onClick = onStartImport) {
                    Text("Download card database")
                }
            }

            is ImportViewModel.State.Importing -> {
                Spacer(Modifier.height(48.dp))
                CircularProgressIndicator()
                Spacer(Modifier.height(16.dp))
                Text("Importing ${state.phase}: ${state.imported}…")
            }

            is ImportViewModel.State.Ready -> {
                Spacer(Modifier.height(16.dp))
                Text("Card database ready: ${state.cardCount} cards.")
            }

            is ImportViewModel.State.Error -> {
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Import failed: ${state.message}",
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(48.dp))
                Button(onClick = onStartImport) {
                    Text("Retry")
                }
            }
        }
    }
}
