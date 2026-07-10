package com.romexplorer.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface RomDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(entries: List<RomEntry>): List<Long>

    @Update
    suspend fun update(entry: RomEntry)

    @Query("SELECT * FROM roms WHERE uri = :uri LIMIT 1")
    suspend fun findByUri(uri: String): RomEntry?

    @Query("DELETE FROM roms WHERE folderUri = :folderUri")
    suspend fun clearFolder(folderUri: String)

    @Query("SELECT DISTINCT system FROM roms ORDER BY system ASC")
    fun distinctSystems(): Flow<List<String>>

    @Query("SELECT DISTINCT reGenre FROM roms WHERE reGenre IS NOT NULL ORDER BY reGenre ASC")
    fun distinctGenres(): Flow<List<String>>

    /**
     * Mirrors the filter bar on retroexplore.com: system + genre + "popular only",
     * plus a free-text search box.
     */
    @Query(
        """
        SELECT * FROM roms
        WHERE (:system IS NULL OR system = :system)
          AND (:genre IS NULL OR reGenre = :genre)
          AND (:popularOnly = 0 OR rePopularityRank IS NOT NULL)
          AND (:query IS NULL OR displayTitle LIKE '%' || :query || '%')
        ORDER BY
          CASE WHEN :sort = 'popular' THEN rePopularityRank END ASC,
          CASE WHEN :sort = 'title' THEN displayTitle END ASC,
          CASE WHEN :sort = 'system' THEN system END ASC,
          CASE WHEN :sort = 'achievements' THEN raAchievementsTotal END DESC,
          displayTitle ASC
        """
    )
    fun browse(
        system: String?,
        genre: String?,
        popularOnly: Boolean,
        query: String?,
        sort: String
    ): Flow<List<RomEntry>>

    @Query("SELECT * FROM roms WHERE id = :id")
    fun observe(id: Long): Flow<RomEntry?>

    @Query("SELECT COUNT(*) FROM roms")
    fun count(): Flow<Int>
}
