package com.example

import com.example.data.hltb.HltbClient
import com.example.data.hltb.HltbGameEntry
import com.example.data.hltb.HltbMatchResult
import com.example.data.hltb.HltbMatcher
import com.example.data.hltb.HltbRepository
import com.example.data.model.Game
import com.example.data.model.GameConsole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun testHltbClientLiveSearch() {
    runBlocking {
      val client = HltbClient()
      val result = client.searchGames("The Witcher 3")
      assertTrue("HLTB search should succeed", result.isSuccess)
      val items = result.getOrNull()
      assertNotNull(items)
      assertTrue("HLTB should return games for 'The Witcher 3'", items!!.isNotEmpty())

      val witcher = items.first()
      assertTrue("Main story hours should be > 0", witcher.mainStoryHours > 10f)
      println("Verified HLTB Client search: ${witcher.gameName} (Main: ${witcher.mainStoryHours}h, Extra: ${witcher.mainExtraHours}h, 100%: ${witcher.completionistHours}h)")
    }
  }

  @Test
  fun testHltbMatcherNormalizationAndScoring() {
    // Roman numeral normalization
    assertEquals("final fantasy 7", HltbMatcher.normalizeTitle("Final Fantasy VII"))
    assertEquals("final fantasy 16", HltbMatcher.normalizeTitle("Final Fantasy XVI"))
    assertEquals("grand theft auto 4", HltbMatcher.normalizeTitle("Grand Theft Auto IV"))
    assertEquals("resident evil 4", HltbMatcher.normalizeTitle("Resident Evil 4 - Game of the Year Edition"))

    // Abbreviation normalization
    assertEquals("breath of the wild", HltbMatcher.normalizeTitle("botw"))
    assertEquals("resident evil 4", HltbMatcher.normalizeTitle("re4"))

    val localGame = Game(
      id = 1L,
      title = "The Witcher 3: Wild Hunt",
      consoleId = GameConsole.PC.id,
      genre = "RPG",
      releaseYear = 2015
    )

    val candidates = listOf(
      HltbGameEntry(
        gameId = 10270L,
        gameName = "The Witcher 3: Wild Hunt",
        mainStoryHours = 51.5f,
        mainExtraHours = 103f,
        completionistHours = 174f,
        platforms = "PC, PlayStation 4, Xbox One",
        releaseYear = 2015
      ),
      HltbGameEntry(
        gameId = 99999L,
        gameName = "The Witcher 3: Wild Hunt - Blood and Wine (DLC)",
        mainStoryHours = 15f,
        mainExtraHours = 28f,
        completionistHours = 40f,
        platforms = "PC, PlayStation 4, Xbox One",
        releaseYear = 2016
      )
    )

    val matchResult = HltbMatcher.matchGame(localGame, candidates)
    assertTrue("Should match base game", matchResult is HltbMatchResult.Matched)
    val matched = (matchResult as HltbMatchResult.Matched).entry
    assertEquals(10270L, matched.gameId)
    assertEquals("The Witcher 3: Wild Hunt", matched.gameName)
  }

  @Test
  fun testHltbRepositoryResolve() {
    runBlocking {
      val repo = HltbRepository()
      val game = Game(
        id = 2L,
        title = "Super Mario Odyssey",
        consoleId = GameConsole.SWITCH.id,
        genre = "Platformer",
        releaseYear = 2017
      )

      val matchResult = repo.resolveHltbForGame(game, forceRefresh = true)
      assertTrue("Repository should successfully resolve Mario Odyssey", matchResult is HltbMatchResult.Matched)
      val entry = (matchResult as HltbMatchResult.Matched).entry
      assertTrue("Main story should be > 0", entry.mainStoryHours > 0f)
      println("Resolved Mario Odyssey: Main=${entry.mainStoryHours}h, 100%=${entry.completionistHours}h")
    }
  }

  @Test
  fun testMainBottomNavTabs() {
    val tabNames = com.example.ui.components.MainBottomNavTab.entries.map { it.name }
    assertTrue(tabNames.contains("LIBRARY"))
    assertTrue(tabNames.contains("PLATFORMS"))
    assertTrue(tabNames.contains("FRANCHISE"))
    assertTrue(tabNames.contains("ACCOUNT"))
    assertEquals("Franchise", com.example.ui.components.MainBottomNavTab.FRANCHISE.label)
  }
}





