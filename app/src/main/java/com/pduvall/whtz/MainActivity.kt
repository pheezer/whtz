package com.pduvall.whtz

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.pduvall.whtz.ui.deck.DeckImportScreen
import com.pduvall.whtz.ui.game.GameScreen
import com.pduvall.whtz.ui.home.HomeScreen
import com.pduvall.whtz.ui.printer.PrinterSettingsScreen
import com.pduvall.whtz.ui.tokens.TokenLibraryScreen
import com.pduvall.whtz.ui.importer.ImportScreen
import com.pduvall.whtz.ui.importer.ImportViewModel
import com.pduvall.whtz.ui.common.PrivacyOverlay
import com.pduvall.whtz.ui.theme.WhtzTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Dark theme → light system-bar icons (the Surface paints the dark background behind them).
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }
        setContent {
            WhtzTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    var privacy by remember { mutableStateOf(false) }
                    val importVm: ImportViewModel = hiltViewModel()
                    val importState by importVm.state.collectAsStateWithLifecycle()
                    val ready = importState is ImportViewModel.State.Ready
                    Box(modifier = Modifier.fillMaxSize()) {
                        Column(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
                            // Eye icon at the top opens privacy mode (hidden while privacy is on).
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                            ) {
                                if (!privacy) {
                                    IconButton(onClick = { privacy = true }) {
                                        Icon(Icons.Filled.Visibility, contentDescription = "Privacy mode")
                                    }
                                }
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                when (val s = importState) {
                                    is ImportViewModel.State.Ready -> AppNav(
                                        cardCount = s.cardCount,
                                        privacy = privacy,
                                        onExitPrivacy = { privacy = false },
                                    )
                                    else -> ImportScreen(state = importState, onStartImport = importVm::startImport)
                                }
                            }
                        }
                        // Before the app is Ready, AppNav isn't shown — cover the import screen here.
                        if (privacy && !ready) PrivacyOverlay(onDismiss = { privacy = false })
                    }
                }
            }
        }
    }
}

@Composable
private fun AppNav(cardCount: Int, privacy: Boolean, onExitPrivacy: () -> Unit) {
    val nav = rememberNavController()
    val route = nav.currentBackStackEntryAsState().value?.destination?.route
    val onGame = route?.startsWith("game") == true
    Box(modifier = Modifier.fillMaxSize()) {
    NavHost(navController = nav, startDestination = "home") {
        composable("home") {
            HomeScreen(
                cardCount = cardCount,
                onImport = { nav.navigate("import") },
                onResume = { nav.navigate("game") },
                onPlayDeck = { id -> nav.navigate("game?deckId=$id") },
                onEditDeck = { id -> nav.navigate("import?deckId=$id") },
                onPrinterSettings = { nav.navigate("printer") },
                onTokens = { nav.navigate("tokens") },
            )
        }
        composable("printer") {
            PrinterSettingsScreen(onBack = { nav.popBackStack() })
        }
        composable("tokens") {
            TokenLibraryScreen(onBack = { nav.popBackStack() })
        }
        composable(
            route = "import?deckId={deckId}",
            arguments = listOf(
                navArgument("deckId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
            ),
        ) { entry ->
            val editDeckId = entry.arguments?.getString("deckId")?.toLongOrNull()
            DeckImportScreen(
                deckId = editDeckId,
                onBack = { nav.popBackStack() },
                onDeckSaved = { id ->
                    nav.navigate("game?deckId=$id") { popUpTo("home") }
                },
            )
        }
        composable(
            route = "game?deckId={deckId}",
            arguments = listOf(
                navArgument("deckId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
            ),
        ) { entry ->
            val deckId = entry.arguments?.getString("deckId")?.toLongOrNull()
            GameScreen(
                deckId = deckId,
                onExit = { nav.popBackStack("home", inclusive = false) },
                privacy = privacy,
                onExitPrivacy = onExitPrivacy,
            )
        }
    }
        // The game screen shows its own privacy panel (with zone views); cover the rest here.
        if (privacy && !onGame) {
            PrivacyOverlay(onDismiss = onExitPrivacy)
        }
    }
}
