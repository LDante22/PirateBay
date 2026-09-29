package com.example

import com.example.data.model.Game
import com.example.data.model.GameConsole
import com.example.ui.viewmodel.PlatformOption
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun testPlatformOptionConsoleMatching() {
        // PC
        assertTrue(PlatformOption.PC.supports(GameConsole.PC))
        assertFalse(PlatformOption.PC.supports(GameConsole.PS5))

        // PlayStation
        assertTrue(PlatformOption.PLAYSTATION.supports(GameConsole.PS2))
        assertTrue(PlatformOption.PLAYSTATION.supports(GameConsole.PS5))
        assertFalse(PlatformOption.PLAYSTATION.supports(GameConsole.XBOX_ONE))

        // Nintendo
        assertTrue(PlatformOption.NINTENDO.supports(GameConsole.SWITCH))
        assertTrue(PlatformOption.NINTENDO.supports(GameConsole.NINTENDO_DS))
        assertFalse(PlatformOption.NINTENDO.supports(GameConsole.PC))

        // Xbox
        assertTrue(PlatformOption.XBOX.supports(GameConsole.XBOX_360))
        assertTrue(PlatformOption.XBOX.supports(GameConsole.XBOX_ONE))
        assertFalse(PlatformOption.XBOX.supports(GameConsole.PS4))

        // All
        assertTrue(PlatformOption.ALL.supports(GameConsole.PC))
        assertTrue(PlatformOption.ALL.supports(GameConsole.SWITCH))
        assertTrue(PlatformOption.ALL.supports(GameConsole.PS5))
        assertTrue(PlatformOption.ALL.supports(GameConsole.XBOX_360))
    }

    @Test
    fun testFilterEligibleGamesForPlatform() {
        val sampleGames = listOf(
            Game(id = 1, title = "Half-Life 2", consoleId = GameConsole.PC.id, genre = "FPS"),
            Game(id = 2, title = "God of War", consoleId = GameConsole.PS4.id, genre = "Action"),
            Game(id = 3, title = "The Legend of Zelda", consoleId = GameConsole.SWITCH.id, genre = "Adventure"),
            Game(id = 4, title = "Halo 3", consoleId = GameConsole.XBOX_360.id, genre = "FPS"),
            Game(id = 5, title = "Shadow of the Colossus", consoleId = GameConsole.PS2.id, genre = "Action")
        )

        // PC only
        val pcGames = sampleGames.filter { PlatformOption.PC.supports(it.console) }
        assertEquals(1, pcGames.size)
        assertEquals("Half-Life 2", pcGames[0].title)

        // PlayStation only
        val psGames = sampleGames.filter { PlatformOption.PLAYSTATION.supports(it.console) }
        assertEquals(2, psGames.size)
        assertTrue(psGames.all { it.console == GameConsole.PS4 || it.console == GameConsole.PS2 })

        // Nintendo only
        val nintendoGames = sampleGames.filter { PlatformOption.NINTENDO.supports(it.console) }
        assertEquals(1, nintendoGames.size)
        assertEquals("The Legend of Zelda", nintendoGames[0].title)

        // Xbox only
        val xboxGames = sampleGames.filter { PlatformOption.XBOX.supports(it.console) }
        assertEquals(1, xboxGames.size)
        assertEquals("Halo 3", xboxGames[0].title)

        // All platforms
        val allGames = sampleGames.filter { PlatformOption.ALL.supports(it.console) }
        assertEquals(5, allGames.size)
    }

    @Test
    fun testHltbHourFormatting() {
        assertEquals("--", Game.formatHours(0f))
        assertEquals("--", Game.formatHours(-5f))
        assertEquals("25h", Game.formatHours(25f))
        assertEquals("38h", Game.formatHours(38f))
        assertEquals("62h", Game.formatHours(62f))
        assertEquals("12.5h", Game.formatHours(12.5f))
    }

    @Test
    fun testHltbMatcherExactMatch() {
        val game = Game(
            id = 10,
            title = "Final Fantasy VII",
            consoleId = GameConsole.PS2.id,
            genre = "RPG",
            releaseYear = 1997
        )

        val candidate = com.example.data.hltb.HltbGameEntry(
            gameId = 3524,
            gameName = "Final Fantasy VII",
            mainStoryHours = 36.5f,
            mainExtraHours = 52.0f,
            completionistHours = 85.0f,
            platforms = "PlayStation, PC, Switch",
            releaseYear = 1997
        )

        val match = com.example.data.hltb.HltbMatcher.matchGame(game, listOf(candidate))
        assertTrue(match is com.example.data.hltb.HltbMatchResult.Matched)
        val matchedEntry = (match as com.example.data.hltb.HltbMatchResult.Matched).entry
        assertEquals(3524L, matchedEntry.gameId)
        assertEquals(36.5f, matchedEntry.mainStoryHours, 0.01f)
    }

    @Test
    fun testHltbMatcherTitleNormalizationAndRomanNumerals() {
        val normalized1 = com.example.data.hltb.HltbMatcher.normalizeTitle("Grand Theft Auto IV: The Complete Edition")
        val normalized2 = com.example.data.hltb.HltbMatcher.normalizeTitle("Grand Theft Auto 4")
        assertEquals(normalized1, normalized2)

        val game = Game(
            id = 11,
            title = "Grand Theft Auto IV",
            consoleId = GameConsole.PS3.id,
            genre = "Action",
            releaseYear = 2008
        )

        val candidate = com.example.data.hltb.HltbGameEntry(
            gameId = 4056,
            gameName = "Grand Theft Auto 4",
            mainStoryHours = 27.5f,
            mainExtraHours = 40f,
            completionistHours = 76f,
            platforms = "PlayStation 3, Xbox 360, PC",
            releaseYear = 2008
        )

        val match = com.example.data.hltb.HltbMatcher.matchGame(game, listOf(candidate))
        assertTrue(match is com.example.data.hltb.HltbMatchResult.Matched)
    }
}

