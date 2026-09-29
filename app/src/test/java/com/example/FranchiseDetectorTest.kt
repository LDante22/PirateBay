package com.example

import com.example.data.franchise.FranchiseDetector
import com.example.data.model.Game
import com.example.data.model.GameConsole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FranchiseDetectorTest {

    @Test
    fun testCrossPlatformGroupingAssassinsCreed() {
        val games = listOf(
            Game(id = 1, title = "Assassin's Creed", consoleId = GameConsole.PS2.id, releaseYear = 2007),
            Game(id = 2, title = "Assassin's Creed II", consoleId = GameConsole.PS3.id, releaseYear = 2009),
            Game(id = 3, title = "Assassin's Creed Brotherhood", consoleId = GameConsole.PS3.id, releaseYear = 2010),
            Game(id = 4, title = "Assassin's Creed Origins", consoleId = GameConsole.PS4.id, releaseYear = 2017),
            Game(id = 5, title = "Assassin's Creed Odyssey", consoleId = GameConsole.PS4.id, releaseYear = 2018),
            Game(id = 6, title = "Assassin's Creed Valhalla", consoleId = GameConsole.PC.id, releaseYear = 2020)
        )

        val franchises = FranchiseDetector.detectFranchises(games)
        assertEquals(1, franchises.size)

        val ac = franchises.first()
        assertEquals("Assassin's Creed", ac.name)
        assertEquals(6, ac.gameCount)

        // Verify all 4 platforms are represented in metadata
        val platformIds = ac.platforms.map { it.id }.toSet()
        assertTrue(platformIds.contains(GameConsole.PS2.id))
        assertTrue(platformIds.contains(GameConsole.PS3.id))
        assertTrue(platformIds.contains(GameConsole.PS4.id))
        assertTrue(platformIds.contains(GameConsole.PC.id))

        // Verify chronological ordering
        val years = ac.games.map { it.releaseYear }
        assertEquals(listOf(2007, 2009, 2010, 2017, 2018, 2020), years)
    }

    @Test
    fun testFranchiseDetectionWithSubtitlesAndNumbering() {
        val gameGodOfWar = Game(id = 10, title = "God of War: Ragnarök", consoleId = GameConsole.PS5.id, releaseYear = 2022)
        assertEquals("God of War", FranchiseDetector.detectFranchiseForGame(gameGodOfWar))

        val gameWitcher = Game(id = 11, title = "The Witcher 3: Wild Hunt", consoleId = GameConsole.PC.id, releaseYear = 2015)
        assertEquals("The Witcher", FranchiseDetector.detectFranchiseForGame(gameWitcher))

        val gameGta = Game(id = 12, title = "Grand Theft Auto V", consoleId = GameConsole.PS4.id, releaseYear = 2013)
        assertEquals("Grand Theft Auto", FranchiseDetector.detectFranchiseForGame(gameGta))

        val gameFf = Game(id = 13, title = "Final Fantasy VII Remake", consoleId = GameConsole.PS5.id, releaseYear = 2020)
        assertEquals("Final Fantasy", FranchiseDetector.detectFranchiseForGame(gameFf))

        val gameRe = Game(id = 14, title = "Resident Evil Village", consoleId = GameConsole.PS5.id, releaseYear = 2021)
        assertEquals("Resident Evil", FranchiseDetector.detectFranchiseForGame(gameRe))

        val gameZelda = Game(id = 15, title = "The Legend of Zelda: Tears of the Kingdom", consoleId = GameConsole.SWITCH.id, releaseYear = 2023)
        assertEquals("The Legend of Zelda", FranchiseDetector.detectFranchiseForGame(gameZelda))
    }

    @Test
    fun testAvoidIncorrectGroupingOfUnrelatedGames() {
        // Words like "Super", "The", "Chronicles" alone should not group standalone games
        val standaloneGame1 = Game(id = 20, title = "Celeste", consoleId = GameConsole.SWITCH.id)
        val standaloneGame2 = Game(id = 21, title = "Tetris Effect", consoleId = GameConsole.PS4.id)
        val standaloneGame3 = Game(id = 22, title = "Disco Elysium", consoleId = GameConsole.PC.id)

        assertNull(FranchiseDetector.detectFranchiseForGame(standaloneGame1))
        assertNull(FranchiseDetector.detectFranchiseForGame(standaloneGame2))
        assertNull(FranchiseDetector.detectFranchiseForGame(standaloneGame3))

        val list = listOf(standaloneGame1, standaloneGame2, standaloneGame3)
        val franchises = FranchiseDetector.detectFranchises(list)
        assertTrue("No incorrect franchises should be generated for standalone games", franchises.isEmpty())
    }

    @Test
    fun testPlatformFilterInsideFranchise() {
        val games = listOf(
            Game(id = 1, title = "Metal Gear Solid 2: Sons of Liberty", consoleId = GameConsole.PS2.id, releaseYear = 2001),
            Game(id = 2, title = "Metal Gear Solid 3: Snake Eater", consoleId = GameConsole.PS2.id, releaseYear = 2004),
            Game(id = 3, title = "Metal Gear Solid 4: Guns of the Patriots", consoleId = GameConsole.PS3.id, releaseYear = 2008),
            Game(id = 4, title = "Metal Gear Solid V: The Phantom Pain", consoleId = GameConsole.PS4.id, releaseYear = 2015),
            Game(id = 5, title = "Metal Gear Solid V: The Phantom Pain", consoleId = GameConsole.PC.id, releaseYear = 2015)
        )

        val franchises = FranchiseDetector.detectFranchises(games)
        assertEquals(1, franchises.size)
        val mgs = franchises.first()
        assertEquals("Metal Gear", mgs.name)

        // All platforms: 5 games
        val allGames = FranchiseDetector.filterFranchiseGames(mgs.games, "ALL")
        assertEquals(5, allGames.size)

        // PS2: 2 games
        val ps2Games = FranchiseDetector.filterFranchiseGames(mgs.games, GameConsole.PS2.id)
        assertEquals(2, ps2Games.size)

        // PS3: 1 game
        val ps3Games = FranchiseDetector.filterFranchiseGames(mgs.games, GameConsole.PS3.id)
        assertEquals(1, ps3Games.size)

        // PC: 1 game
        val pcGames = FranchiseDetector.filterFranchiseGames(mgs.games, GameConsole.PC.id)
        assertEquals(1, pcGames.size)
    }

    @Test
    fun testSingleGameNeverCreatesFranchise() {
        // A single game must NEVER create a franchise
        val singleAc = listOf(
            Game(id = 1, title = "Assassin's Creed II", consoleId = GameConsole.PS3.id)
        )
        val franchisesAc = FranchiseDetector.detectFranchises(singleAc)
        assertTrue("Single game Assassin's Creed II should not create a franchise", franchisesAc.isEmpty())

        val singleGow = listOf(
            Game(id = 2, title = "God of War", consoleId = GameConsole.PS2.id)
        )
        val franchisesGow = FranchiseDetector.detectFranchises(singleGow)
        assertTrue("Single game God of War should not create a franchise", franchisesGow.isEmpty())
    }

    @Test
    fun testSameGameOnMultiplePlatformsDoesNotCreateFranchise() {
        // Assassin's Creed II on PS3 and PC is only ONE unique game
        val acCrossPlatform = listOf(
            Game(id = 1, title = "Assassin's Creed II", consoleId = GameConsole.PS3.id),
            Game(id = 2, title = "Assassin's Creed II", consoleId = GameConsole.PC.id)
        )
        val acFranchises = FranchiseDetector.detectFranchises(acCrossPlatform)
        assertTrue("Same game on multiple platforms must NOT create a franchise", acFranchises.isEmpty())

        // Resident Evil 4 — PS2 and Resident Evil 4 — GameCube represent the same game
        val reCopies = listOf(
            Game(id = 3, title = "Resident Evil 4 — PS2", consoleId = GameConsole.PS2.id),
            Game(id = 4, title = "Resident Evil 4 — GameCube", consoleId = GameConsole.GAMECUBE.id)
        )
        val reFranchises = FranchiseDetector.detectFranchises(reCopies)
        assertTrue("Resident Evil 4 across PS2 and GameCube must NOT create a franchise", reFranchises.isEmpty())
    }

    @Test
    fun testTwoDifferentGamesCreateFranchise() {
        // Two different games belonging to Assassin's Creed
        val acGames = listOf(
            Game(id = 1, title = "Assassin's Creed II", consoleId = GameConsole.PS3.id),
            Game(id = 2, title = "Assassin's Creed Brotherhood", consoleId = GameConsole.PS3.id)
        )
        val acFranchises = FranchiseDetector.detectFranchises(acGames)
        assertEquals(1, acFranchises.size)
        assertEquals("Assassin's Creed", acFranchises.first().name)
        assertEquals(2, acFranchises.first().uniqueGameCount)

        // God of War + God of War II
        val gowGames = listOf(
            Game(id = 3, title = "God of War", consoleId = GameConsole.PS2.id),
            Game(id = 4, title = "God of War II", consoleId = GameConsole.PS2.id)
        )
        val gowFranchises = FranchiseDetector.detectFranchises(gowGames)
        assertEquals(1, gowFranchises.size)
        assertEquals("God of War", gowFranchises.first().name)
        assertEquals(2, gowFranchises.first().uniqueGameCount)

        // Call of Duty three games
        val codGames = listOf(
            Game(id = 5, title = "Call of Duty 4: Modern Warfare", consoleId = GameConsole.PS3.id),
            Game(id = 6, title = "Call of Duty: Black Ops", consoleId = GameConsole.PS3.id),
            Game(id = 7, title = "Call of Duty: Modern Warfare 2", consoleId = GameConsole.PS3.id)
        )
        val codFranchises = FranchiseDetector.detectFranchises(codGames)
        assertEquals(1, codFranchises.size)
        assertEquals("Call of Duty", codFranchises.first().name)
        assertEquals(3, codFranchises.first().uniqueGameCount)
    }

    @Test
    fun testDynamicFranchiseCreationAndRemoval() {
        // Start with 1 game -> 0 franchises
        val initialList = mutableListOf(
            Game(id = 1, title = "Resident Evil 4", consoleId = GameConsole.PS2.id)
        )
        assertEquals(0, FranchiseDetector.detectFranchises(initialList).size)

        // Add 2nd platform copy of the same game -> still 0 franchises
        initialList.add(Game(id = 2, title = "Resident Evil 4", consoleId = GameConsole.GAMECUBE.id))
        assertEquals(0, FranchiseDetector.detectFranchises(initialList).size)

        // Add a genuinely different game from the franchise -> franchise created!
        val re2 = Game(id = 3, title = "Resident Evil 2", consoleId = GameConsole.N64.id)
        initialList.add(re2)
        val franchises = FranchiseDetector.detectFranchises(initialList)
        assertEquals(1, franchises.size)
        assertEquals("Resident Evil", franchises.first().name)
        assertEquals(2, franchises.first().uniqueGameCount)
        assertEquals(3, franchises.first().gameCount) // 3 total library entries

        // Remove RE2 -> drops below 2 unique games -> franchise automatically removed
        initialList.remove(re2)
        assertEquals(0, FranchiseDetector.detectFranchises(initialList).size)
    }
}
