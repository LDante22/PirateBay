package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Game
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDao {
    @Query("SELECT * FROM games ORDER BY addedTimestamp DESC")
    fun getAllGames(): Flow<List<Game>>

    @Query("SELECT * FROM games WHERE consoleId = :consoleId ORDER BY addedTimestamp DESC")
    fun getGamesByConsole(consoleId: String): Flow<List<Game>>

    @Query("SELECT * FROM games WHERE id = :id")
    suspend fun getGameById(id: Long): Game?

    @Query("""
        SELECT * FROM games 
        WHERE (:consoleId = 'ALL' OR consoleId = :consoleId)
        AND (:status = 'ALL' OR wishlistStatus = :status)
        AND (:genre = 'ALL' OR genre LIKE '%' || :genre || '%')
        AND (:year = 0 OR releaseYear = :year)
        AND (:minYear = 0 OR releaseYear >= :minYear)
        AND (:maxYear = 0 OR releaseYear <= :maxYear)
        AND (
            :query = '' 
            OR title LIKE '%' || :query || '%' 
            OR genre LIKE '%' || :query || '%' 
            OR emulator LIKE '%' || :query || '%'
            OR developer LIKE '%' || :query || '%'
            OR CAST(releaseYear AS TEXT) LIKE '%' || :query || '%'
        )
        ORDER BY 
            CASE WHEN :sortBy = 'TITLE' THEN title END ASC,
            CASE WHEN :sortBy = 'YEAR' THEN releaseYear END DESC,
            CASE WHEN :sortBy = 'YEAR_ASC' THEN releaseYear END ASC,
            CASE WHEN :sortBy = 'RATING' THEN userRating END DESC,
            CASE WHEN :sortBy = 'RECENT' THEN addedTimestamp END DESC
    """)
    fun searchAndFilterGames(
        query: String,
        consoleId: String,
        genre: String,
        year: Int,
        minYear: Int,
        maxYear: Int,
        status: String,
        sortBy: String
    ): Flow<List<Game>>

    @Query("SELECT DISTINCT genre FROM games ORDER BY genre ASC")
    fun getAllGenres(): Flow<List<String>>

    @Query("SELECT DISTINCT releaseYear FROM games ORDER BY releaseYear DESC")
    fun getAllReleaseYears(): Flow<List<Int>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGame(game: Game): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(games: List<Game>)

    @Update
    suspend fun updateGame(game: Game)

    @Delete
    suspend fun deleteGame(game: Game)

    @Query("DELETE FROM games WHERE id = :id")
    suspend fun deleteGameById(id: Long)

    @Query("DELETE FROM games")
    suspend fun deleteAllGames()

    @Query("SELECT COUNT(*) FROM games")
    suspend fun getGameCount(): Int

    @Query("UPDATE games SET isFavorite = NOT isFavorite WHERE id = :id")
    suspend fun toggleFavorite(id: Long)

    @Query("UPDATE games SET wishlistStatus = :newStatus WHERE id = :id")
    suspend fun updateStatus(id: Long, newStatus: String)

    @Query("UPDATE games SET userRating = :newRating WHERE id = :id")
    suspend fun updateRating(id: Long, newRating: Float)

    @Query("""
        UPDATE games 
        SET emulatorNotes = :notes, 
            recommendedBackend = :backend, 
            internalResolution = :resolution, 
            biosRequirement = :bios, 
            targetFramerate = :framerate,
            proTips = :proTips
        WHERE id = :id
    """)
    suspend fun updateEmulatorNotes(
        id: Long,
        notes: String,
        backend: String,
        resolution: String,
        bios: String,
        framerate: String,
        proTips: String
    )

    @Query("""
        UPDATE games 
        SET hltbId = :hltbId,
            hltbName = :hltbName,
            hltbMainStoryHours = :mainStory,
            hltbMainExtraHours = :mainExtra,
            hltbCompletionistHours = :completionist,
            hltbLastUpdated = :lastUpdated,
            hltbSyncStatus = :syncStatus
        WHERE id = :id
    """)
    suspend fun updateHltbData(
        id: Long,
        hltbId: Long,
        hltbName: String,
        mainStory: Float,
        mainExtra: Float,
        completionist: Float,
        lastUpdated: Long,
        syncStatus: String
    )

    @Query("""
        UPDATE games 
        SET externalProvider = :provider,
            externalGameId = :gameId,
            externalLastUpdated = :lastUpdated,
            externalSyncStatus = :syncStatus,
            bannerArtUrl = :bannerArtUrl,
            publisher = :publisher,
            releaseDate = :releaseDate,
            description = :description,
            websiteUrl = :websiteUrl,
            metacriticScore = :metacriticScore,
            supportedPlatforms = :supportedPlatforms
        WHERE id = :id
    """)
    suspend fun updateExternalMetadata(
        id: Long,
        provider: String,
        gameId: String,
        lastUpdated: Long,
        syncStatus: String,
        bannerArtUrl: String,
        publisher: String,
        releaseDate: String,
        description: String,
        websiteUrl: String,
        metacriticScore: Int,
        supportedPlatforms: String
    )
}
