package com.romexplorer.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.romexplorer.app.data.RomEntry

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    state: LibraryUiState,
    onPickFolder: () -> Unit,
    onOpenSettings: () -> Unit,
    onEnrich: () -> Unit,
    onSystem: (String?) -> Unit,
    onGenre: (String?) -> Unit,
    onPopularOnly: (Boolean) -> Unit,
    onQuery: (String) -> Unit,
    onSort: (String) -> Unit,
    onPlay: (RomEntry) -> Unit,
    onOpenGame: (RomEntry) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ROM Explorer", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onEnrich) { Icon(Icons.Default.Refresh, contentDescription = "Fetch metadata") }
                    IconButton(onClick = onOpenSettings) { Icon(Icons.Default.Settings, contentDescription = "Settings") }
                }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).padding(horizontal = 12.dp)) {

            OutlinedTextField(
                value = state.query,
                onValueChange = onQuery,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                placeholder = { Text("Search your library...") },
                singleLine = true
            )

            Spacer(Modifier.height(8.dp))

            if (state.games.isEmpty() && state.totalCount == 0) {
                EmptyLibrary(onPickFolder, state.isScanning)
                return@Column
            }

            FilterBar(
                systems = state.systems,
                genres = state.genres,
                selectedSystem = state.selectedSystem,
                selectedGenre = state.selectedGenre,
                popularOnly = state.popularOnly,
                sort = state.sort,
                onSystem = onSystem,
                onGenre = onGenre,
                onPopularOnly = onPopularOnly,
                onSort = onSort
            )

            Row(Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("${state.games.size} of ${state.totalCount} games", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onPickFolder) { Text("Rescan folder") }
            }

            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 120.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(state.games, key = { it.id }) { game ->
                    GameCard(game, onPlay = { onPlay(game) }, onClick = { onOpenGame(game) })
                }
            }
        }
    }
}

@Composable
private fun EmptyLibrary(onPickFolder: () -> Unit, isScanning: Boolean) {
    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("No ROMs scanned yet", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            "Pick the folder where your ROMs live. Nothing leaves your device except optional lookups against RetroExplore and RetroAchievements.",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = onPickFolder, enabled = !isScanning) {
            Text(if (isScanning) "Scanning..." else "Choose ROM folder")
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterBar(
    systems: List<String>,
    genres: List<String>,
    selectedSystem: String?,
    selectedGenre: String?,
    popularOnly: Boolean,
    sort: String,
    onSystem: (String?) -> Unit,
    onGenre: (String?) -> Unit,
    onPopularOnly: (Boolean) -> Unit,
    onSort: (String) -> Unit
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Dropdown("System", selectedSystem, systems, onSystem)
        Dropdown("Genre", selectedGenre, genres, onGenre)
        FilterChip(
            selected = popularOnly,
            onClick = { onPopularOnly(!popularOnly) },
            label = { Text("Popular only") }
        )
        Dropdown("Sort", sort, listOf("title", "popular", "system", "achievements"), { onSort(it ?: "title") })
    }
}

@Composable
private fun Dropdown(label: String, selected: String?, options: List<String>, onSelect: (String?) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        AssistChip(onClick = { expanded = true }, label = { Text(selected ?: label) })
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text("All") }, onClick = { onSelect(null); expanded = false })
            options.forEach { opt ->
                DropdownMenuItem(text = { Text(opt) }, onClick = { onSelect(opt); expanded = false })
            }
        }
    }
}

@Composable
private fun GameCard(game: RomEntry, onPlay: () -> Unit, onClick: () -> Unit) {
    ElevatedCard(onClick = onClick) {
        Box {
            AsyncImage(
                model = game.reBoxArtUrl,
                contentDescription = game.displayTitle,
                modifier = Modifier.fillMaxWidth().height(150.dp)
            )
            IconButton(
                onClick = onPlay,
                modifier = Modifier.align(Alignment.BottomEnd).padding(4.dp)
            ) { Icon(Icons.Default.PlayArrow, contentDescription = "Play") }
        }
        Column(Modifier.padding(8.dp)) {
            Text(game.displayTitle, maxLines = 1, style = MaterialTheme.typography.bodyMedium)
            Text(game.system, style = MaterialTheme.typography.labelSmall)
            if (game.raAchievementsTotal != null) {
                Text(
                    "${game.raAchievementsUnlocked ?: 0}/${game.raAchievementsTotal} achievements",
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}
