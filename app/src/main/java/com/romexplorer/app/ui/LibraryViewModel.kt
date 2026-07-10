package com.romexplorer.app.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romexplorer.app.RomExplorerApp
import com.romexplorer.app.data.LibraryRepository
import com.romexplorer.app.data.RomEntry
import com.romexplorer.app.scan.LibraryScanner
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class LibraryUiState(
    val games: List<RomEntry> = emptyList(),
    val systems: List<String> = emptyList(),
    val genres: List<String> = emptyList(),
    val selectedSystem: String? = null,
    val selectedGenre: String? = null,
    val popularOnly: Boolean = false,
    val query: String = "",
    val sort: String = "title", // title | popular | system | achievements
    val isScanning: Boolean = false,
    val isEnriching: Boolean = false,
    val totalCount: Int = 0
)

class LibraryViewModel(private val app: RomExplorerApp) : ViewModel() {

    private val repo = LibraryRepository(app, app.database.romDao(), app.settings)
    private val scanner = LibraryScanner(app)

    private val filters = MutableStateFlow(LibraryUiState())

    val state: StateFlow<LibraryUiState> = filters
        .flatMapLatest { f ->
            combine(
                repo.browse(f.selectedSystem, f.selectedGenre, f.popularOnly, f.query, f.sort),
                repo.distinctSystems(),
                repo.distinctGenres(),
                repo.count()
            ) { games, systems, genres, total ->
                f.copy(games = games, systems = systems, genres = genres, totalCount = total)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LibraryUiState())

    fun setSystem(system: String?) = filters.update { it.copy(selectedSystem = system) }
    fun setGenre(genre: String?) = filters.update { it.copy(selectedGenre = genre) }
    fun setPopularOnly(value: Boolean) = filters.update { it.copy(popularOnly = value) }
    fun setQuery(q: String) = filters.update { it.copy(query = q) }
    fun setSort(sort: String) = filters.update { it.copy(sort = sort) }

    fun rescan(folderUri: Uri) {
    viewModelScope.launch {
        filters.update { it.copy(isScanning = true) }
        try {
            repo.rescan(scanner, folderUri)
        } catch (e: Exception) {
            android.util.Log.e("LibraryViewModel", "Rescan failed", e)
        }
        filters.update { it.copy(isScanning = false) }
    }
}

    /** Fetches RetroExplore + RetroAchievements metadata for whatever is currently visible. */
    fun enrichVisible() {
        viewModelScope.launch {
            filters.update { it.copy(isEnriching = true) }
            val snapshot = state.value.games
            for (entry in snapshot) {
                runCatching { repo.enrichWithRetroExplore(entry) }
                runCatching { repo.enrichWithRetroAchievements(entry) }
            }
            filters.update { it.copy(isEnriching = false) }
        }
    }

    fun launch(entry: RomEntry) = repo.launch(entry)
}
