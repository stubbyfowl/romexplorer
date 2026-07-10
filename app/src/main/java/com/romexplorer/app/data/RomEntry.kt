package com.romexplorer.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One scanned file from the user's local ROM library, enriched over time with
 * metadata pulled from RetroExplore and RetroAchievements.
 */
@Entity(tableName = "roms")
data class RomEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,

    // --- from disk scan ---
    val uri: String,              // SAF content:// uri for the actual file
    val fileName: String,
    val displayTitle: String,     // cleaned-up title guessed from the filename
    val system: String,           // e.g. "SNES", "PS1", "GBA" - see SystemDetector
    val sizeBytes: Long,
    val folderUri: String,        // parent folder, used for rescans

    // --- from RetroExplore (optional, filled in lazily) ---
    val reGameId: String? = null,
    val reGenre: String? = null,
    val reBoxArtUrl: String? = null,
    val reDescription: String? = null,
    val rePopularityRank: Int? = null, // lower = more popular; null = unknown

    // --- from RetroAchievements (optional, filled in lazily) ---
    val raGameId: Int? = null,
    val raHash: String? = null,
    val raAchievementsTotal: Int? = null,
    val raAchievementsUnlocked: Int? = null,
    val raIconUrl: String? = null,

    val lastMetadataFetch: Long = 0L,
    val isFavorite: Boolean = false
)
