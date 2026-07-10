package com.romexplorer.app.data

import android.content.Context
import android.net.Uri
import com.romexplorer.app.emulator.EmulatorLauncher
import com.romexplorer.app.network.RaHasher
import com.romexplorer.app.network.RetroAchievementsClient
import com.romexplorer.app.network.RetroExploreClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class LibraryRepository(
    private val context: Context,
    private val dao: RomDao,
    private val settings: Settings
) {
    private val retroExplore = RetroExploreClient()

    fun browse(system: String?, genre: String?, popularOnly: Boolean, query: String?, sort: String): Flow<List<RomEntry>> =
        dao.browse(system, genre, popularOnly, query?.takeIf { it.isNotBlank() }, sort)

    fun distinctSystems(): Flow<List<String>> = dao.distinctSystems()
    fun distinctGenres(): Flow<List<String>> = dao.distinctGenres()
    fun count(): Flow<Int> = dao.count()

    suspend fun rescan(scanner: com.romexplorer.app.scan.LibraryScanner, folderUri: Uri) {
        dao.clearFolder(folderUri.toString())
        val found = scanner.scan(folderUri)
        dao.insertAll(found)
        settings.setRomFolder(folderUri.toString())
    }

    /** Pulls genre/box art/description for one entry from RetroExplore, if not already cached. */
    suspend fun enrichWithRetroExplore(entry: RomEntry) {
        if (entry.reGameId != null) return
        val meta = retroExplore.search(entry.displayTitle) ?: return
        dao.update(
            entry.copy(
                reGameId = meta.gameId,
                reGenre = meta.genre,
                reBoxArtUrl = meta.boxArtUrl,
                reDescription = meta.description,
                lastMetadataFetch = System.currentTimeMillis()
            )
        )
    }

    /** Hashes the ROM and checks it against RetroAchievements, if credentials are configured. */
    suspend fun enrichWithRetroAchievements(entry: RomEntry) {
        val user = settings.raUsername.first() ?: return
        val key = settings.raApiKey.first() ?: return
        if (user.isBlank() || key.isBlank()) return
        if (entry.raGameId != null) return

        val hash = RaHasher.hash(context, Uri.parse(entry.uri), entry.system) ?: return
        val client = RetroAchievementsClient(context, user, key)
        val gameId = client.lookupByHash(hash) ?: return
        val progress = client.getProgress(gameId) ?: return

        dao.update(
            entry.copy(
                raGameId = gameId,
                raHash = hash,
                raAchievementsTotal = progress.achievementsTotal,
                raAchievementsUnlocked = progress.achievementsUnlocked,
                raIconUrl = progress.iconUrl
            )
        )
    }

    fun launch(entry: RomEntry) {
        EmulatorLauncher.launch(context, entry.system, Uri.parse(entry.uri), entry.displayTitle)
    }
}
