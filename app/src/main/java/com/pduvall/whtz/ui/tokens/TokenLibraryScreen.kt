package com.pduvall.whtz.ui.tokens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.pduvall.whtz.data.local.entity.TokenCardEntity

@Composable
fun TokenLibraryScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    vm: TokenLibraryViewModel = hiltViewModel(),
) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val printing by vm.printing.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()
    var viewing by remember { mutableStateOf<TokenCardEntity?>(null) }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Token library",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onBack) { Text("Back") }
        }
        Spacer(Modifier.height(8.dp))

        if (ui.tokenCount == 0) {
            Text(
                "No tokens bundled yet. Go to Home → \"Update card database\" to download the token " +
                    "library (it works offline afterwards).",
                style = MaterialTheme.typography.bodyMedium,
            )
            return@Column
        }

        OutlinedTextField(
            value = ui.query,
            onValueChange = vm::onQueryChange,
            singleLine = true,
            label = { Text("Search tokens (name or type)") },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        if (printing || ui.loading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
        message?.let {
            Text(it, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(4.dp))
        }

        if (ui.results.isEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text("No tokens match.", style = MaterialTheme.typography.bodyMedium)
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(ui.results, key = { it.id }) { token ->
                    TokenRow(
                        token = token,
                        enabled = !printing,
                        onPrint = { vm.print(token) },
                        onView = { viewing = token },
                    )
                }
            }
        }
    }

    viewing?.let { token ->
        Dialog(onDismissRequest = { viewing = null }) {
            Box(
                modifier = Modifier.fillMaxWidth().clickable { viewing = null },
                contentAlignment = Alignment.Center,
            ) {
                val url = token.normalImageUrl ?: token.artCropUrl
                if (url != null) {
                    AsyncImage(
                        model = url,
                        contentDescription = token.name,
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    Text("No image available for ${token.name}.", modifier = Modifier.padding(24.dp))
                }
            }
        }
    }
}

/** A token list row (thumbnail + name/type + Print), reused by the in-game token dialog. */
@Composable
fun TokenRow(
    token: TokenCardEntity,
    enabled: Boolean,
    onPrint: () -> Unit,
    modifier: Modifier = Modifier,
    onView: () -> Unit = {},
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = token.normalImageUrl ?: token.artCropUrl,
            contentDescription = token.name,
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(48.dp).clickable { onView() },
        )
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f).clickable { onView() }) {
            Text(token.name, style = MaterialTheme.typography.bodyLarge)
            val subtitle = listOfNotNull(
                token.typeLine,
                if (!token.power.isNullOrBlank() && !token.toughness.isNullOrBlank()) {
                    "${token.power}/${token.toughness}"
                } else {
                    null
                },
            ).joinToString("  ")
            if (subtitle.isNotBlank()) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall)
            }
        }
        TextButton(onClick = onPrint, enabled = enabled) { Text("Print") }
    }
}
