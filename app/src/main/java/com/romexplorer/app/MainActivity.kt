package com.romexplorer.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.romexplorer.app.ui.GameDetailScreen
import com.romexplorer.app.ui.LibraryScreen
import com.romexplorer.app.ui.LibraryViewModel
import com.romexplorer.app.ui.SettingsScreen

class MainActivity : ComponentActivity() {

    private val app get() = application as RomExplorerApp

    private val viewModel: LibraryViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                LibraryViewModel(app) as T
        }
    }

    private val pickFolder = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri: Uri? ->
        if (uri != null) {
            contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
            )
            viewModel.rescan(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppRoot()
        }
    }

    @Composable
    private fun AppRoot() {
        val nav = rememberNavController()
        MaterialTheme {
            Surface(modifier = Modifier) {
                NavHost(navController = nav, startDestination = "library") {
                    composable("library") {
                        val state by viewModel.state.collectAsState()
                        LibraryScreen(
                            state = state,
                            onPickFolder = { pickFolder.launch(null) },
                            onOpenSettings = { nav.navigate("settings") },
                            onEnrich = { viewModel.enrichVisible() },
                            onSystem = viewModel::setSystem,
                            onGenre = viewModel::setGenre,
                            onPopularOnly = viewModel::setPopularOnly,
                            onQuery = viewModel::setQuery,
                            onSort = viewModel::setSort,
                            onPlay = { viewModel.launch(it) },
                            onOpenGame = { game -> nav.navigate("game/${game.id}") }
                        )
                    }
                    composable("settings") {
                        SettingsScreen(settings = app.settings, onBack = { nav.popBackStack() })
                    }
                    composable("game/{id}") { backStackEntry ->
                        val id = backStackEntry.arguments?.getString("id")?.toLongOrNull()
                        val state by viewModel.state.collectAsState()
                        val game = state.games.firstOrNull { it.id == id }
                        if (game != null) {
                            GameDetailScreen(
                                game = game,
                                onBack = { nav.popBackStack() },
                                onPlay = { viewModel.launch(game) }
                            )
                        }
                    }
                }
            }
        }
    }
}
