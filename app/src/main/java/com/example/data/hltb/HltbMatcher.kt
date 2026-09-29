package com.example.data.hltb

import com.example.data.model.Game
import com.example.data.model.GameConsole
import kotlin.math.max
import kotlin.math.min

/**
 * Intelligent matcher for HowLongToBeat search results.
 * Compares candidate titles, platforms, and release years with the local game,
 * scoring confidence and preventing ambiguous or incorrect matches.
 */
object HltbMatcher {

    private const val MIN_ACCEPTABLE_CONFIDENCE = 0.65f
    private const val AMBIGUITY_THRESHOLD_DELTA = 0.08f

    /**
     * Normalizes a game title for robust matching:
     * - Converts to lower case
     * - Normalizes Roman numerals (e.g. III -> 3, II -> 2, IV -> 4, etc.)
     * - Replaces '&' with 'and'
     * - Strips common edition/remaster noise phrases
     * - Removes punctuation and collapses whitespace
     */
    fun normalizeTitle(title: String): String {
        if (title.isBlank()) return ""
        var norm = title.lowercase().trim()

        // Replace '&' with 'and'
        norm = norm.replace("&", " and ")

        // Expand common game abbreviations when present as full tokens
        val abbreviations = mapOf(
            "botw" to "breath of the wild",
            "totk" to "tears of the kingdom",
            "re4" to "resident evil 4",
            "re3" to "resident evil 3",
            "re2" to "resident evil 2",
            "re1" to "resident evil 1",
            "ff7" to "final fantasy 7",
            "ff8" to "final fantasy 8",
            "ff9" to "final fantasy 9",
            "ff10" to "final fantasy 10",
            "ff12" to "final fantasy 12",
            "ff13" to "final fantasy 13",
            "ff14" to "final fantasy 14",
            "ff15" to "final fantasy 15",
            "ff16" to "final fantasy 16",
            "pkmn" to "pokemon",
            "gow" to "god of war",
            "mgs" to "metal gear solid",
            "gta" to "grand theft auto"
        )
        for ((abbr, full) in abbreviations) {
            norm = norm.replace(Regex("\\b$abbr\\b"), full)
        }

        // Replace Roman numerals when surrounded by word boundaries (in descending length)
        val romanMap = listOf(
            "xviii" to "18", "xvii" to "17", "xvi" to "16", "xv" to "15", "xiv" to "14",
            "xiii" to "13", "xii" to "12", "xi" to "11", "xix" to "19", "xx" to "20",
            "viii" to "8", "vii" to "7", "vi" to "6", "iv" to "4", "v" to "5",
            "iii" to "3", "ii" to "2", "ix" to "9", "x" to "10", "i" to "1"
        )
        for ((rom, num) in romanMap) {
            norm = norm.replace(Regex("\\b$rom\\b"), " $num ")
        }

        // Strip common edition and remaster noise phrases
        val noisePhrases = listOf(
            "the complete edition",
            "complete edition",
            "game of the year edition",
            "game of the year",
            "goty edition",
            "goty",
            "definitive edition",
            "enhanced edition",
            "special edition",
            "collectors edition",
            "directors cut",
            "remastered",
            "remaster"
        )
        for (phrase in noisePhrases) {
            norm = norm.replace(phrase, " ")
        }

        // Remove subtitles separators like " - " or ": " if desired, but keep text
        norm = norm.replace(Regex("[:\\-_/•|]"), " ")

        // Remove special symbols
        norm = norm.replace(Regex("[^a-z0-9\\s]"), "")

        // Collapse multi-spaces
        norm = norm.replace(Regex("\\s+"), " ").trim()

        return norm
    }

    /**
     * Checks whether the game's console matches the platform string returned by HLTB.
     */
    fun isPlatformCompatible(console: GameConsole, hltbPlatforms: String): Boolean {
        if (hltbPlatforms.isBlank() || console == GameConsole.ALL) return true
        val lower = hltbPlatforms.lowercase()
        return when (console) {
            GameConsole.PC -> lower.contains("pc") || lower.contains("windows") || lower.contains("steam") || lower.contains("dos")
            GameConsole.PS2 -> lower.contains("playstation 2") || lower.contains("ps2")
            GameConsole.PSP -> lower.contains("playstation portable") || lower.contains("psp")
            GameConsole.PS3 -> lower.contains("playstation 3") || lower.contains("ps3")
            GameConsole.PS_VITA -> lower.contains("playstation vita") || lower.contains("ps vita") || lower.contains("vita")
            GameConsole.PS4 -> lower.contains("playstation 4") || lower.contains("ps4")
            GameConsole.PS5 -> lower.contains("playstation 5") || lower.contains("ps5")
            GameConsole.GBA -> lower.contains("game boy advance") || lower.contains("gba")
            GameConsole.N64 -> lower.contains("nintendo 64") || lower.contains("n64")
            GameConsole.NINTENDO_DS -> lower.contains("nintendo ds") || lower.contains("ds")
            GameConsole.NINTENDO_3DS -> lower.contains("nintendo 3ds") || lower.contains("3ds")
            GameConsole.GAMECUBE -> lower.contains("gamecube") || lower.contains("nintendo gamecube")
            GameConsole.WII -> lower.contains("wii") && !lower.contains("wii u")
            GameConsole.WII_U -> lower.contains("wii u")
            GameConsole.SWITCH -> lower.contains("nintendo switch") || lower.contains("switch")
            GameConsole.XBOX -> lower.contains("xbox") && !lower.contains("xbox 360") && !lower.contains("xbox one") && !lower.contains("xbox series")
            GameConsole.XBOX_360 -> lower.contains("xbox 360") || lower.contains("360")
            GameConsole.XBOX_ONE -> lower.contains("xbox one") || lower.contains("xbox series")
            GameConsole.ALL -> true
        }
    }

    /**
     * Calculates string similarity between 0.0 and 1.0 using a blend of Levenshtein and Token Overlap.
     */
    fun calculateSimilarity(s1: String, s2: String): Float {
        val n1 = normalizeTitle(s1)
        val n2 = normalizeTitle(s2)
        if (n1 == n2) return 1.0f
        if (n1.isEmpty() || n2.isEmpty()) return 0.0f

        // Token Overlap (Jaccard / Token-Set)
        val tokens1 = n1.split(" ").filter { it.isNotBlank() }.toSet()
        val tokens2 = n2.split(" ").filter { it.isNotBlank() }.toSet()
        val intersection = tokens1.intersect(tokens2).size
        val union = tokens1.union(tokens2).size
        val jaccard = if (union > 0) intersection.toFloat() / union.toFloat() else 0.0f

        // Levenshtein similarity
        val maxLen = max(n1.length, n2.length)
        val distance = levenshteinDistance(n1, n2)
        val levSim = 1.0f - (distance.toFloat() / maxLen.toFloat())

        // Substring bonus: if one title contains the other exactly
        val substringBonus = if (n1.contains(n2) || n2.contains(n1)) 0.15f else 0.0f

        return min(1.0f, (levSim * 0.5f) + (jaccard * 0.5f) + substringBonus)
    }

    private fun levenshteinDistance(s1: String, s2: String): Int {
        val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }
        for (i in 0..s1.length) dp[i][0] = i
        for (j in 0..s2.length) dp[0][j] = j

        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,      // deletion
                    dp[i][j - 1] + 1,      // insertion
                    dp[i - 1][j - 1] + cost // substitution
                )
            }
        }
        return dp[s1.length][s2.length]
    }

    /**
     * Evaluates a candidate HLTB game against the user's game.
     * Returns a composite score from 0.0 to 1.0.
     */
    fun scoreCandidate(game: Game, candidate: HltbGameEntry): Float {
        // Direct ID match gets top priority
        if (game.hltbId > 0L && game.hltbId == candidate.gameId) {
            return 1.0f
        }

        var baseSim = calculateSimilarity(game.title, candidate.gameName)

        // Exact normalized match
        val normGame = normalizeTitle(game.title)
        val normCandidate = normalizeTitle(candidate.gameName)
        if (normGame == normCandidate) {
            baseSim = 0.95f
        }

        // Platform compatibility check
        val platformMatches = isPlatformCompatible(game.console, candidate.platforms)
        if (platformMatches) {
            baseSim += 0.15f
        } else if (candidate.platforms.isNotBlank()) {
            baseSim -= 0.20f
        }

        // Year alignment check (if releaseYear is valid)
        if (game.releaseYear > 1980 && candidate.releaseYear > 1980) {
            val yearDiff = kotlin.math.abs(game.releaseYear - candidate.releaseYear)
            if (yearDiff == 0) {
                baseSim += 0.10f
            } else if (yearDiff <= 2) {
                baseSim += 0.05f
            } else if (yearDiff > 10) {
                baseSim -= 0.15f
            }
        }

        // Noise penalty (e.g. expansion / soundtrack / DLC when user title doesn't specify it)
        val lowerCandidate = candidate.gameName.lowercase()
        val lowerGame = game.title.lowercase()
        val noiseWords = listOf("dlc", "expansion", "soundtrack", "ost", "collector's edition", "artbook", "guide", "demo", "vr")
        for (noise in noiseWords) {
            if (lowerCandidate.contains(noise) && !lowerGame.contains(noise)) {
                baseSim -= 0.35f
            }
        }

        return max(0.0f, min(1.0f, baseSim))
    }

    /**
     * Matches a game against a list of candidates from HLTB search results.
     */
    fun matchGame(game: Game, candidates: List<HltbGameEntry>): HltbMatchResult {
        if (candidates.isEmpty()) {
            return HltbMatchResult.NotFound
        }

        // 1. If an exact HLTB ID was previously stored and exists in candidates, return it immediately
        if (game.hltbId > 0L) {
            val exactIdMatch = candidates.find { it.gameId == game.hltbId }
            if (exactIdMatch != null) {
                return HltbMatchResult.Matched(exactIdMatch, 1.0f)
            }
        }

        // 2. Score all candidates
        val scoredList = candidates.map { candidate ->
            val score = scoreCandidate(game, candidate)
            candidate.copy(confidenceScore = score) to score
        }.sortedByDescending { it.second }

        val best = scoredList.firstOrNull() ?: return HltbMatchResult.NotFound

        // 3. Threshold checks
        if (best.second < MIN_ACCEPTABLE_CONFIDENCE) {
            return HltbMatchResult.NotFound
        }

        // 4. Ambiguity check: if there is a second candidate with a very close score and different gameId
        if (scoredList.size > 1) {
            val second = scoredList[1]
            val scoreDelta = best.second - second.second

            // If the top score is not overwhelmingly high and second candidate is within ambiguity threshold
            if (best.second < 0.90f && scoreDelta < AMBIGUITY_THRESHOLD_DELTA) {
                val candidateList = listOf(best.first, second.first)
                return HltbMatchResult.Unresolved(
                    candidates = candidateList,
                    reason = "Ambiguous matches between '${best.first.gameName}' and '${second.first.gameName}'"
                )
            }
        }

        return HltbMatchResult.Matched(
            entry = best.first,
            confidenceScore = best.second
        )
    }
}
