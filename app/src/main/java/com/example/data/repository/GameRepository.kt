package com.example.data.repository

import com.example.data.db.GameDao
import com.example.data.model.Game
import kotlinx.coroutines.flow.Flow

class GameRepository(private val gameDao: GameDao) {

    fun searchAndFilterGames(
        query: String,
        consoleId: String,
        genre: String = "ALL",
        year: Int = 0,
        minYear: Int = 0,
        maxYear: Int = 0,
        status: String = "ALL",
        sortBy: String = "RECENT"
    ): Flow<List<Game>> {
        return gameDao.searchAndFilterGames(
            query = query,
            consoleId = consoleId,
            genre = genre,
            year = year,
            minYear = minYear,
            maxYear = maxYear,
            status = status,
            sortBy = sortBy
        )
    }

    fun getAllGenres(): Flow<List<String>> = gameDao.getAllGenres()

    fun getAllReleaseYears(): Flow<List<Int>> = gameDao.getAllReleaseYears()

    fun getAllGames(): Flow<List<Game>> = gameDao.getAllGames()

    suspend fun getGameById(id: Long): Game? = gameDao.getGameById(id)

    suspend fun insertGame(game: Game): Long = gameDao.insertGame(game)

    suspend fun insertAll(games: List<Game>) = gameDao.insertAll(games)

    suspend fun updateGame(game: Game) = gameDao.updateGame(game)

    suspend fun deleteGame(game: Game) = gameDao.deleteGame(game)

    suspend fun deleteGameById(id: Long) = gameDao.deleteGameById(id)

    suspend fun deleteAllGames() = gameDao.deleteAllGames()

    suspend fun toggleFavorite(id: Long) = gameDao.toggleFavorite(id)

    suspend fun updateStatus(id: Long, newStatus: String) = gameDao.updateStatus(id, newStatus)

    suspend fun updateRating(id: Long, newRating: Float) = gameDao.updateRating(id, newRating)

    suspend fun updateEmulatorNotes(
        id: Long,
        notes: String,
        backend: String,
        resolution: String,
        bios: String,
        framerate: String,
        proTips: String
    ) = gameDao.updateEmulatorNotes(id, notes, backend, resolution, bios, framerate, proTips)

    suspend fun updateHltbData(
        id: Long,
        hltbId: Long,
        hltbName: String,
        mainStory: Float,
        mainExtra: Float,
        completionist: Float,
        lastUpdated: Long,
        syncStatus: String
    ) = gameDao.updateHltbData(
        id, hltbId, hltbName, mainStory, mainExtra, completionist, lastUpdated, syncStatus
    )

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
    ) = gameDao.updateExternalMetadata(
        id, provider, gameId, lastUpdated, syncStatus, bannerArtUrl,
        publisher, releaseDate, description, websiteUrl, metacriticScore, supportedPlatforms
    )

    suspend fun getGameCount(): Int = gameDao.getGameCount()
}
