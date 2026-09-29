package com.example

import com.example.data.model.CoverSourceType
import com.example.data.model.Game
import com.example.data.steamgriddb.SteamGridDbGame
import com.example.data.steamgriddb.SteamGridDbGrid
import com.example.data.steamgriddb.SteamGridDbResult
import com.example.data.steamgriddb.SteamGridDbServerEndpoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SteamGridDbTest {

    @Test
    fun testTitleSimplification_removesSubtitlesAndParentheses() {
        // 1. Removes text in parentheses
        assertEquals("The Witcher 3", SteamGridDbServerEndpoint.simplifyTitle("The Witcher 3 (Game of the Year Edition)"))

        // 2. Removes subtitles after colon
        assertEquals("Resident Evil 4", SteamGridDbServerEndpoint.simplifyTitle("Resident Evil 4: Remake (2023)"))

        // 3. Removes subtitles after hyphen and strips punctuation
        assertEquals("Half Life 2", SteamGridDbServerEndpoint.simplifyTitle("Half-Life 2 - Episode One"))

        // 4. Removes brackets and punctuation
        assertEquals("God of War", SteamGridDbServerEndpoint.simplifyTitle("God of War [PS4 / PC]"))

        // 5. Cleans special punctuation
        assertEquals("Metal Gear Solid 3", SteamGridDbServerEndpoint.simplifyTitle("Metal Gear Solid 3: Snake Eater!"))
    }

    @Test
    fun testGameModel_listCoverPrefersThumbnail() {
        val gameWithThumb = Game(
            title = "Final Fantasy X",
            consoleId = "ps2",
            coverArtUrl = "https://cdn2.steamgriddb.com/grid/full.png",
            coverThumbUrl = "https://cdn2.steamgriddb.com/thumb/thumb.png",
            coverSource = CoverSourceType.STEAMGRIDDB
        )
        // In lists, game.listCover should be the fast thumbnail
        assertEquals("https://cdn2.steamgriddb.com/thumb/thumb.png", gameWithThumb.listCover)
        // In detail, coverArtUrl holds the full 600x900 resolution
        assertEquals("https://cdn2.steamgriddb.com/grid/full.png", gameWithThumb.coverArtUrl)

        // If thumbnail is empty, fallback to coverArtUrl
        val gameWithoutThumb = Game(
            title = "Halo 2",
            consoleId = "xbox",
            coverArtUrl = "https://cdn2.steamgriddb.com/grid/full_halo.png",
            coverThumbUrl = ""
        )
        assertEquals("https://cdn2.steamgriddb.com/grid/full_halo.png", gameWithoutThumb.listCover)
    }

    @Test
    fun testSteamGridDbResults_typesAndHandling() {
        val candidate = SteamGridDbGame(id = 1234, name = "Silent Hill 2")
        val grid = SteamGridDbGrid(
            id = 999,
            score = 42,
            url = "https://steamgriddb.com/grid.png",
            thumb = "https://steamgriddb.com/thumb.png"
        )

        val success = SteamGridDbResult.Success(listOf(grid))
        assertTrue(success is SteamGridDbResult.Success)
        assertEquals(1, success.data.size)
        assertEquals(42, success.data.first().score)

        val error401 = SteamGridDbResult.Error(statusCode = 401, message = "Cheia API SteamGridDB nu e validă sau lipsește din Secrets", canRetry = false)
        assertEquals(401, error401.statusCode)
        assertTrue(!error401.canRetry)

        val error429 = SteamGridDbResult.Error(statusCode = 429, message = "Prea multe cereri (Rate limit). Te rugăm să aștepți.", canRetry = true)
        assertEquals(429, error429.statusCode)
        assertTrue(error429.canRetry)
    }
}
