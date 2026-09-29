package com.example.data.hltb

import com.example.data.model.Game
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

/**
 * Repository coordinating HowLongToBeat searches, local caching, and request deduplication.
 */
class HltbRepository(
    private val client: HltbClient = HltbClient()
) {

    companion object {
        // Cache valid for 30 days
        const val CACHE_VALIDITY_MS = 30L * 24 * 60 * 60 * 1000L
    }

    private val inFlightMutex = Mutex()
    private val inFlightRequests = ConcurrentHashMap<Long, kotlinx.coroutines.Deferred<HltbMatchResult>>()

    /**
     * Resolves HLTB data for a game:
     * 1. Checks if existing cached data is valid.
     * 2. If valid and not force-refreshing, returns the cached result.
     * 3. Queries HLTB and matches using title, console, and release year.
     */
    suspend fun resolveHltbForGame(
        game: Game,
        forceRefresh: Boolean = false
    ): HltbMatchResult {
        // 1. Check local cache
        val currentTime = System.currentTimeMillis()
        val hasRecentCache = game.hltbLastUpdated > 0L && (currentTime - game.hltbLastUpdated < CACHE_VALIDITY_MS)

        if (!forceRefresh && hasRecentCache && game.hltbSyncStatus == "SYNCED" && game.hasHltbData) {
            return HltbMatchResult.Matched(
                entry = HltbGameEntry(
                    gameId = game.hltbId,
                    gameName = game.hltbName.ifBlank { game.title },
                    mainStoryHours = game.hltbMainStoryHours,
                    mainExtraHours = game.hltbMainExtraHours,
                    completionistHours = game.hltbCompletionistHours,
                    platforms = game.console.displayName,
                    releaseYear = game.releaseYear
                ),
                confidenceScore = 1.0f
            )
        }

        // 2. Perform search & match
        val searchResult = client.searchGames(game.title)
        if (searchResult.isFailure) {
            val ex = searchResult.exceptionOrNull()
            return HltbMatchResult.Error(ex?.localizedMessage ?: "Failed to connect to HowLongToBeat")
        }

        val candidates = searchResult.getOrDefault(emptyList())
        if (candidates.isEmpty()) {
            return HltbMatchResult.NotFound
        }

        return HltbMatcher.matchGame(game, candidates)
    }
}
