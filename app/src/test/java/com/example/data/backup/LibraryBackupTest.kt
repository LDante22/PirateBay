package com.example.data.backup

import com.example.data.model.Game
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class LibraryBackupTest {

    private val sampleGame1 = Game(
        id = 101L,
        title = "Super Mario Odyssey",
        consoleId = "SWITCH",
        releaseYear = 2017,
        genre = "Platformer",
        userRating = 5.0f,
        notes = "Masterpiece on Nintendo Switch",
        wishlistStatus = "COMPLETED",
        isFavorite = true,
        userEmail = "gamer@example.com",
        hltbId = 42833L,
        hltbName = "Super Mario Odyssey",
        hltbMainStoryHours = 12.5f,
        hltbMainExtraHours = 26.0f,
        hltbCompletionistHours = 61.5f,
        externalProvider = "IGDB",
        externalGameId = "26758",
        youtubeVideoId = "wGQHQc_3ycE"
    )

    private val sampleGame2 = Game(
        id = 102L,
        title = "The Legend of Zelda: Tears of the Kingdom",
        consoleId = "SWITCH",
        releaseYear = 2023,
        genre = "Action-Adventure",
        userRating = 5.0f,
        notes = "Incredible physics and building mechanics",
        wishlistStatus = "PLAYING",
        isFavorite = true,
        userEmail = "gamer@example.com",
        hltbId = 72012L,
        hltbName = "The Legend of Zelda: Tears of the Kingdom",
        hltbMainStoryHours = 59.0f,
        hltbMainExtraHours = 110.0f,
        hltbCompletionistHours = 228.0f
    )

    @Test
    fun testJsonExportAndAnalysis() = runBlocking {
        val games = listOf(sampleGame1, sampleGame2)
        val jsonOutput = LibraryBackupService.generateJsonExport(
            games = games,
            appName = "The Tavern Game Vault",
            appVersion = "1.0"
        )

        assertNotNull(jsonOutput)
        assertTrue(jsonOutput.contains("The Tavern Game Vault"))
        assertTrue(jsonOutput.contains("Super Mario Odyssey"))
        assertTrue(jsonOutput.contains("Tears of the Kingdom"))
        assertFalse(jsonOutput.contains("gamer@example.com")) // Email should be sanitized for privacy

        // Verify JSON parsing and duplicate detection
        val localGames = listOf(
            sampleGame1.copy(userRating = 4.0f, notes = "Old note")
        )

        val analysis = LibraryBackupService.analyzeBackupJson(
            jsonString = jsonOutput,
            currentLocalGames = localGames
        )

        assertTrue(analysis.isValid)
        assertEquals(2, analysis.totalRecordsInBackup)
        assertEquals(1, analysis.newGames.size) // Tears of the Kingdom is new
        assertEquals(1, analysis.existingMatches.size) // Super Mario Odyssey matches local

        // Test Smart Merge
        val match = analysis.existingMatches.first()
        val mergedGame = LibraryBackupService.resolveGameMerge(match.localGame, match.backupGame)
        assertEquals(4.0f, mergedGame.userRating, 0.01f) // Local user rating preserved
        assertEquals("Old note", mergedGame.notes) // Local custom note preserved
        assertEquals(42833L, mergedGame.hltbId) // HLTB data imported
        assertEquals(12.5f, mergedGame.hltbMainStoryHours, 0.01f)
    }

    @Test
    fun testCsvExportFormat() = runBlocking {
        val games = listOf(sampleGame1, sampleGame2)
        val csvOutput = LibraryBackupService.generateCsvExport(games)

        assertNotNull(csvOutput)
        assertTrue(csvOutput.startsWith("\uFEFF")) // UTF-8 BOM
        assertTrue(csvOutput.contains("Platform / Console"))
        assertTrue(csvOutput.contains("Super Mario Odyssey"))
        assertTrue(csvOutput.contains("The Legend of Zelda: Tears of the Kingdom"))
    }

    @Test
    fun testInvalidJsonHandling() = runBlocking {
        val invalidAnalysis = LibraryBackupService.analyzeBackupJson(
            jsonString = "{ \"broken\": [",
            currentLocalGames = emptyList()
        )
        assertFalse(invalidAnalysis.isValid)
        assertNotNull(invalidAnalysis.errorMessage)
    }
}
