package com.example.data.versus

import android.util.Log
import com.example.BuildConfig
import com.example.data.model.Game
import com.example.data.model.GameConsole
import com.example.data.model.WishlistStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import java.util.concurrent.TimeUnit

enum class StressLevel(
    val label: String,
    val shortLabel: String,
    val emoji: String,
    val colorHex: Long
) {
    LOW("Low / Relaxing", "Low", "🌱", 0xFF10B981),
    MEDIUM("Medium / Balanced", "Medium", "⚖️", 0xFFF59E0B),
    HIGH("High / Demanding", "High", "⚡", 0xFFEF4444),
    PUNISHING("Very High / Punishing", "Extreme", "💀", 0xFFDC2626),
    UNKNOWN("Unknown", "Unknown", "❓", 0xFF94A3B8);

    companion object {
        fun fromString(str: String): StressLevel {
            val upper = str.uppercase(Locale.US)
            return when {
                upper.contains("PUNISH") || upper.contains("EXTREME") || upper.contains("VERY HIGH") -> PUNISHING
                upper.contains("HIGH") || upper.contains("HARD") || upper.contains("DEMANDING") -> HIGH
                upper.contains("MEDIUM") || upper.contains("MODERATE") || upper.contains("BALANCED") -> MEDIUM
                upper.contains("LOW") || upper.contains("RELAX") || upper.contains("CHILL") || upper.contains("COZY") || upper.contains("EASY") -> LOW
                else -> UNKNOWN
            }
        }
    }
}

data class GameVersusAnalysis(
    val game: Game,
    val vibe: String,
    val vibeIcon: String = "🎭",
    val timeRequired: String,
    val timeHours: Float?, // null if unknown
    val stressLevel: StressLevel,
    val stressNotes: String = "",
    val standoutHighlight: String = ""
)

data class VersusResult(
    val games: List<GameVersusAnalysis>,
    val winnerGame: Game,
    val recommendationTitle: String,
    val reasoningSummary: String,
    val primaryAdvantage: String,
    val isAiGenerated: Boolean = false
)

object VersusComparisonEngine {

    private const val TAG = "VersusComparisonEngine"
    private const val GEMINI_MODEL = "gemini-3.5-flash"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    suspend fun compareGames(games: List<Game>): VersusResult = withContext(Dispatchers.IO) {
        require(games.size in 2..3) { "Versus comparison requires 2 or 3 games." }

        // Check if Gemini API key is configured and valid
        val apiKey = BuildConfig.GEMINI_API_KEY
        val hasValidApiKey = apiKey.isNotBlank() && !apiKey.equals("MY_GEMINI_API_KEY", ignoreCase = true)

        if (hasValidApiKey) {
            try {
                val aiResult = fetchVersusFromGemini(games, apiKey)
                if (aiResult != null) {
                    return@withContext aiResult
                }
            } catch (e: Exception) {
                Log.w(TAG, "Gemini Versus API failed, using intelligent offline rule engine: ${e.message}")
            }
        }

        // Deterministic intelligent heuristic evaluator
        evaluateWithOfflineEngine(games)
    }

    private fun fetchVersusFromGemini(games: List<Game>, apiKey: String): VersusResult? {
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$GEMINI_MODEL:generateContent?key=$apiKey"

        val gamesDescription = games.mapIndexed { index, g ->
            """
            Game ${index + 1}:
            - ID: ${g.id}
            - Title: "${g.title}"
            - Console: ${g.console.displayName}
            - Genre: "${g.genre}"
            - Release Year: ${g.releaseYear}
            - Developer/Publisher: "${g.developer} / ${g.publisher}"
            - User Rating: ${g.userRating} / 5.0
            - Status in Wishlist: ${g.status.label}
            - HLTB Main Story Hours: ${if (g.hltbMainStoryHours > 0f) "${g.hltbMainStoryHours} hours" else "Unknown"}
            - Description/Notes: "${g.description.take(200)} ${g.notes.take(100)}"
            """.trimIndent()
        }.joinToString("\n\n")

        val prompt = """
            You are a definitive video game advisor. The user is undecided between ${games.size} games from their personal library.
            Your job is to compare them strictly across 3 core criteria and give ONE FIRM, DECISIVE RECOMMENDATION (never say "it depends" or "both are great").

            Selected Games:
            $gamesDescription

            Strict Rules:
            1. For each game, provide:
               - "vibe": A concise 2-4 word descriptor of the atmosphere, mood, and feeling (e.g. "Dark & Oppressive Survival", "Fast, Colorful & Chaotic", "Cozy & Story-Rich", "Epic Cinematic Fantasy", "High-Stakes Tactical", "Psychological Horror").
               - "timeRequired": Approximate time to beat the main story (e.g. "~12 hours", "~35 hours"). If genuinely unknown from the provided data or general knowledge, output "Unknown".
               - "stressLevel": EXACTLY one of ["LOW", "MEDIUM", "HIGH", "PUNISHING", "UNKNOWN"].
               - "standoutHighlight": One punchy sentence highlighting what makes this game uniquely appealing.
            2. Choose ONE single winner game ("winnerGameId") from the provided game IDs.
            3. "reasoningSummary": A crisp, persuasive explanation of 1-3 sentences stating why the user should play the chosen winner right now compared to the other options. Mention specific contrasts in time commitment, stress level, or atmosphere.
            4. "primaryAdvantage": A 3-6 word summary of why it won (e.g. "Lower stress with unmatched pacing", "Immediate thrills with concise length").

            Return ONLY a valid JSON object matching this schema:
            {
              "analyses": [
                {
                  "gameId": ${games[0].id},
                  "vibe": "Dark / Atmospheric",
                  "timeRequired": "~12 hours",
                  "stressLevel": "MEDIUM",
                  "standoutHighlight": "Iconic psychological narrative with haunting sound design."
                }
              ],
              "winnerGameId": ${games[0].id},
              "recommendationTitle": "Play ${games[0].title}",
              "reasoningSummary": "Go with ${games[0].title}. At ~12 hours and a focused, atmospheric vibe, it delivers an immediately gripping story without the exhausting 60-hour grind and high stress of the other options.",
              "primaryAdvantage": "Tighter pacing and immediate narrative payoff"
            }
        """.trimIndent()

        val jsonBody = JSONObject().apply {
            put("contents", JSONArray().put(
                JSONObject().put("parts", JSONArray().put(
                    JSONObject().put("text", prompt)
                ))
            ))
            put("generationConfig", JSONObject().apply {
                put("response_mime_type", "application/json")
                put("temperature", 0.3)
            })
        }

        val request = Request.Builder()
            .url(url)
            .post(jsonBody.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()

        val response = httpClient.newCall(request).execute()
        if (!response.isSuccessful) {
            Log.w(TAG, "Gemini API error code: ${response.code}")
            return null
        }

        val responseString = response.body?.string() ?: return null
        val responseJson = JSONObject(responseString)
        val candidates = responseJson.optJSONArray("candidates") ?: return null
        if (candidates.length() == 0) return null

        val candidate = candidates.getJSONObject(0)
        val content = candidate.optJSONObject("content") ?: return null
        val parts = content.optJSONArray("parts") ?: return null
        if (parts.length() == 0) return null

        val text = parts.getJSONObject(0).optString("text")
        if (text.isBlank()) return null

        val resultJson = JSONObject(text)
        val analysesJson = resultJson.optJSONArray("analyses") ?: JSONArray()
        val winnerId = resultJson.optLong("winnerGameId", games.first().id)

        val analyses = games.map { g ->
            var vibe = deduceVibe(g)
            var timeReq = deduceTimeRequired(g)
            var stress = deduceStressLevel(g)
            var highlight = ""

            for (i in 0 until analysesJson.length()) {
                val item = analysesJson.optJSONObject(i) ?: continue
                if (item.optLong("gameId") == g.id) {
                    if (item.has("vibe")) vibe = item.optString("vibe")
                    if (item.has("timeRequired")) timeReq = item.optString("timeRequired")
                    if (item.has("stressLevel")) stress = StressLevel.fromString(item.optString("stressLevel"))
                    if (item.has("standoutHighlight")) highlight = item.optString("standoutHighlight")
                    break
                }
            }

            val hours = extractHours(timeReq, g)

            GameVersusAnalysis(
                game = g,
                vibe = vibe,
                vibeIcon = chooseVibeIcon(vibe, g.genre),
                timeRequired = timeReq,
                timeHours = hours,
                stressLevel = stress,
                standoutHighlight = highlight.ifBlank { "${g.genre} on ${g.console.displayName}" }
            )
        }

        val winnerGame = games.find { it.id == winnerId } ?: games.first()
        val recTitle = resultJson.optString("recommendationTitle", "My Pick: ${winnerGame.title}")
        val summary = resultJson.optString(
            "reasoningSummary",
            "I'd go with ${winnerGame.title}. It offers the best balance of atmosphere and manageable time commitment for your next session."
        )
        val advantage = resultJson.optString("primaryAdvantage", "Best immediate experience")

        return VersusResult(
            games = analyses,
            winnerGame = winnerGame,
            recommendationTitle = recTitle,
            reasoningSummary = summary,
            primaryAdvantage = advantage,
            isAiGenerated = true
        )
    }

    /**
     * Highly intelligent offline rule-based evaluator that produces accurate, deterministic,
     * and insightful versus recommendations when Gemini API is unavailable or offline.
     */
    fun evaluateWithOfflineEngine(games: List<Game>): VersusResult {
        val analyses = games.map { g ->
            val vibe = deduceVibe(g)
            val timeReq = deduceTimeRequired(g)
            val timeHours = extractHours(timeReq, g)
            val stress = deduceStressLevel(g)
            val highlight = generateStandoutHighlight(g, vibe, stress)

            GameVersusAnalysis(
                game = g,
                vibe = vibe,
                vibeIcon = chooseVibeIcon(vibe, g.genre),
                timeRequired = timeReq,
                timeHours = timeHours,
                stressLevel = stress,
                standoutHighlight = highlight
            )
        }

        // Scoring algorithm to pick a firm, defensible winner
        val scores = analyses.map { analysis ->
            var score = 0.0

            // 1. User rating weight (0 to 25 points)
            score += analysis.game.userRating * 5.0

            // 2. Wishlist priority status (0 to 20 points)
            when (analysis.game.status) {
                WishlistStatus.TOP_PRIORITY -> score += 20.0
                WishlistStatus.PLAYING -> score += 18.0
                WishlistStatus.WANT_TO_PLAY -> score += 12.0
                WishlistStatus.ACQUIRED -> score += 10.0
                WishlistStatus.PREORDER -> score += 6.0
                WishlistStatus.COMPLETED -> score += 2.0
            }

            // 3. Favorite boost (10 points)
            if (analysis.game.isFavorite) {
                score += 10.0
            }

            // 4. Accessibility / Time-to-Finish sweet spot (8 to 25 hours is ideal sweet spot)
            val hours = analysis.timeHours
            if (hours != null) {
                when {
                    hours in 6.0..18.0 -> score += 15.0 // Sweet spot: high completion likelihood
                    hours in 18.1..35.0 -> score += 10.0
                    hours in 35.1..60.0 -> score += 6.0
                    hours > 60.0 -> score += 2.0 // Heavy commitment
                    hours < 6.0 -> score += 12.0 // Crisp short experience
                }
            } else {
                score += 8.0
            }

            // 5. Stress balance penalty for extreme stress unless rated exceptionally high
            when (analysis.stressLevel) {
                StressLevel.LOW -> score += 8.0
                StressLevel.MEDIUM -> score += 10.0 // Balanced friction
                StressLevel.HIGH -> score += 5.0
                StressLevel.PUNISHING -> score += 2.0
                StressLevel.UNKNOWN -> score += 6.0
            }

            analysis to score
        }

        val sorted = scores.sortedByDescending { it.second }
        val winnerAnalysis = sorted.first().first
        val otherAnalyses = analyses.filter { it.game.id != winnerAnalysis.game.id }

        val reasoning = generatePersuasiveReasoning(winnerAnalysis, otherAnalyses)
        val advantage = generatePrimaryAdvantage(winnerAnalysis, otherAnalyses)

        return VersusResult(
            games = analyses,
            winnerGame = winnerAnalysis.game,
            recommendationTitle = "My Pick: ${winnerAnalysis.game.title}",
            reasoningSummary = reasoning,
            primaryAdvantage = advantage,
            isAiGenerated = false
        )
    }

    private fun generatePersuasiveReasoning(
        winner: GameVersusAnalysis,
        others: List<GameVersusAnalysis>
    ): String {
        val otherNames = others.joinToString(" and ") { "\"${it.game.title}\"" }
        val winTime = winner.timeRequired
        val winVibe = winner.vibe.lowercase(Locale.US)
        val winStress = winner.stressLevel.shortLabel.lowercase(Locale.US)

        val firstOther = others.firstOrNull()
        val otherStress = firstOther?.stressLevel?.shortLabel?.lowercase(Locale.US) ?: "higher"
        val otherTime = firstOther?.timeRequired ?: "longer"

        return when {
            winner.timeHours != null && firstOther?.timeHours != null && winner.timeHours < firstOther.timeHours -> {
                "I'd go with \"${winner.game.title}\". At $winTime with a $winVibe experience, it delivers a focused, rewarding adventure right now without the heavier $otherTime commitment of $otherNames."
            }
            winner.stressLevel == StressLevel.LOW || winner.stressLevel == StressLevel.MEDIUM && firstOther?.stressLevel == StressLevel.HIGH -> {
                "I'd choose \"${winner.game.title}\". It offers an engaging $winVibe atmosphere with manageable $winStress stress, making it much easier to pick up and enjoy immediately over the high-intensity demands of $otherNames."
            }
            winner.game.status == WishlistStatus.TOP_PRIORITY -> {
                "\"${winner.game.title}\" is the clear pick. As a top-priority title with a compelling $winVibe vibe and $winTime play time, it stands out as the most urgent and satisfying next playthrough."
            }
            else -> {
                "Go with \"${winner.game.title}\". Its distinctive $winVibe atmosphere and well-balanced $winTime duration make it the most immediately captivating choice among your options."
            }
        }
    }

    private fun generatePrimaryAdvantage(
        winner: GameVersusAnalysis,
        others: List<GameVersusAnalysis>
    ): String {
        val firstOther = others.firstOrNull()
        return when {
            winner.timeHours != null && firstOther?.timeHours != null && winner.timeHours < firstOther.timeHours ->
                "Tighter, more digestible completion time"
            winner.stressLevel in listOf(StressLevel.LOW, StressLevel.MEDIUM) && firstOther?.stressLevel in listOf(StressLevel.HIGH, StressLevel.PUNISHING) ->
                "Lower friction with high immersion"
            winner.game.userRating >= 4.8f ->
                "Masterpiece-tier acclaim & execution"
            else ->
                "Most immediate and rewarding atmosphere"
        }
    }

    private fun generateStandoutHighlight(game: Game, vibe: String, stress: StressLevel): String {
        val titleLower = game.title.lowercase(Locale.US)
        val genreLower = game.genre.lowercase(Locale.US)

        return when {
            titleLower.contains("silent hill") || titleLower.contains("resident evil") ->
                "Atmospheric survival horror classic with intense tension and psychological depth."
            titleLower.contains("persona") ->
                "Stylish JRPG masterpiece blending dungeon crawling with deep social simulation."
            titleLower.contains("elden ring") || titleLower.contains("dark souls") || titleLower.contains("bloodborne") ->
                "Unrivaled world-building and punishing, high-skill combat mastery."
            titleLower.contains("mario") || titleLower.contains("zelda") ->
                "Pure gaming craftsmanship with creative discovery and polished mechanics."
            titleLower.contains("halo") || titleLower.contains("killzone") || titleLower.contains("gears") ->
                "High-octane campaign shooter with iconic soundtrack and kinetic combat."
            genreLower.contains("horror") ->
                "Tense psychological atmosphere and resource management."
            genreLower.contains("rpg") || genreLower.contains("jrpg") ->
                "Deep progression systems, character builds, and expansive lore."
            genreLower.contains("action") || genreLower.contains("hack") ->
                "Fluid kinetic combat and stylish encounter pacing."
            genreLower.contains("platformer") ->
                "Precision movement challenges and energetic level design."
            genreLower.contains("cozy") || genreLower.contains("farming") || genreLower.contains("novel") ->
                "Relaxed, low-stress storytelling and charming world design."
            else ->
                "Engaging $vibe on ${game.console.displayName}."
        }
    }

    private fun deduceVibe(game: Game): String {
        val t = game.title.lowercase(Locale.US)
        val g = game.genre.lowercase(Locale.US)
        val d = game.description.lowercase(Locale.US)

        return when {
            t.contains("silent hill") || g.contains("psychological horror") -> "Dark / Psychological Horror"
            t.contains("resident evil") || g.contains("survival horror") -> "Tense / Survival Atmosphere"
            t.contains("elden ring") || t.contains("dark souls") || t.contains("bloodborne") -> "Grimdark / High Tension"
            t.contains("persona") -> "Stylish / Anime Urban"
            t.contains("mario") || g.contains("3d platformer") -> "Joyful / Playful Exploration"
            t.contains("zelda") || g.contains("action-adventure") -> "Epic / Sense of Wonder"
            t.contains("metroid") -> "Isolated / Sci-Fi Mood"
            t.contains("halo") || t.contains("gears") -> "Cinematic / Sci-Fi Warfare"
            t.contains("god of war") || t.contains("devil may cry") -> "Brutal / High Adrenaline"
            t.contains("pokémon") || t.contains("pokemon") -> "Nostalgic / Cozy Adventure"
            t.contains("cyberpunk") || d.contains("cyberpunk") -> "Gritty / Neon Dystopia"
            t.contains("fallout") || t.contains("stalker") -> "Post-Apocalyptic / Bleak"
            g.contains("horror") -> "Dark / Atmospheric"
            g.contains("racing") -> "Fast / Adrenaline High"
            g.contains("fighting") -> "Competitive / Fast-Paced"
            g.contains("jrpg") || g.contains("rpg") -> "Story-Rich / Immersive"
            g.contains("puzzle") -> "Cerebral / Thoughtful"
            g.contains("stealth") -> "Methodical / High Tension"
            g.contains("shooter") || g.contains("fps") -> "Fast / Action-Packed"
            g.contains("visual novel") || g.contains("cozy") -> "Relaxing / Narrative-Driven"
            g.contains("strategy") || g.contains("tactics") -> "Calculated / Tactical"
            else -> if (game.genre.isNotBlank()) game.genre else "Atmospheric / Engaging"
        }
    }

    private fun chooseVibeIcon(vibe: String, genre: String): String {
        val lower = "$vibe $genre".lowercase(Locale.US)
        return when {
            lower.contains("horror") || lower.contains("grim") || lower.contains("dark") -> "🕯️"
            lower.contains("fast") || lower.contains("adrenaline") || lower.contains("action") -> "⚡"
            lower.contains("cozy") || lower.contains("relax") || lower.contains("joy") -> "🌱"
            lower.contains("epic") || lower.contains("wonder") || lower.contains("fantasy") -> "🗡️"
            lower.contains("sci-fi") || lower.contains("cyber") -> "🚀"
            lower.contains("stylish") || lower.contains("anime") -> "✨"
            lower.contains("tactical") || lower.contains("cerebral") -> "♟️"
            else -> "🎭"
        }
    }

    private fun deduceTimeRequired(game: Game): String {
        return when {
            game.hltbMainStoryHours > 0f -> {
                val h = game.hltbMainStoryHours
                if (h % 1f == 0f) "~${h.toInt()} hours" else "~${String.format(Locale.US, "%.1f", h)} hours"
            }
            game.hltbMainExtraHours > 0f -> "~${game.hltbMainExtraHours.toInt()} hours"
            else -> deduceCatalogTime(game)
        }
    }

    private fun deduceCatalogTime(game: Game): String {
        val t = game.title.lowercase(Locale.US)
        val g = game.genre.lowercase(Locale.US)

        return when {
            t.contains("silent hill 2") -> "~9 hours"
            t.contains("silent hill 3") -> "~7 hours"
            t.contains("resident evil 4") -> "~16 hours"
            t.contains("resident evil 2") -> "~9 hours"
            t.contains("persona 4") -> "~75 hours"
            t.contains("persona 3") -> "~65 hours"
            t.contains("persona 5") -> "~100 hours"
            t.contains("elden ring") -> "~55 hours"
            t.contains("bloodborne") -> "~35 hours"
            t.contains("dark souls") -> "~42 hours"
            t.contains("super mario odyssey") -> "~12 hours"
            t.contains("super mario 64") -> "~12 hours"
            t.contains("zelda: breath of the wild") -> "~50 hours"
            t.contains("zelda: ocarina of time") -> "~28 hours"
            t.contains("halo: combat evolved") -> "~10 hours"
            t.contains("halo 3") -> "~9 hours"
            t.contains("god of war") -> "~12 hours"
            t.contains("metal gear solid 3") -> "~15 hours"
            t.contains("shadow of the colossus") -> "~7 hours"
            t.contains("kingdom hearts 2") -> "~32 hours"
            t.contains("final fantasy x") -> "~48 hours"
            t.contains("portal") -> "~3 hours"
            t.contains("portal 2") -> "~8 hours"
            t.contains("hollow knight") -> "~27 hours"
            g.contains("jrpg") -> "~45 hours"
            g.contains("rpg") -> "~35 hours"
            g.contains("horror") -> "~10 hours"
            g.contains("platformer") -> "~12 hours"
            g.contains("shooter") || g.contains("fps") -> "~9 hours"
            g.contains("fighting") -> "~4 hours"
            g.contains("racing") -> "~14 hours"
            else -> "Unknown"
        }
    }

    private fun extractHours(timeStr: String, game: Game): Float? {
        if (game.hltbMainStoryHours > 0f) return game.hltbMainStoryHours
        val match = Regex("""(\d+(\.\d+)?)""").find(timeStr)
        return match?.groupValues?.get(1)?.toFloatOrNull()
    }

    private fun deduceStressLevel(game: Game): StressLevel {
        val t = game.title.lowercase(Locale.US)
        val g = game.genre.lowercase(Locale.US)

        return when {
            t.contains("elden ring") || t.contains("dark souls") || t.contains("bloodborne") || t.contains("sekiro") || t.contains("demon's souls") ->
                StressLevel.PUNISHING
            t.contains("silent hill") || t.contains("resident evil") || t.contains("dead space") || t.contains("outlast") || t.contains("alien: isolation") ->
                StressLevel.HIGH
            t.contains("ninja gaiden") || t.contains("devil may cry") || t.contains("cuphead") || t.contains("hollow knight") || t.contains("celeste") ->
                StressLevel.HIGH
            t.contains("persona") || t.contains("final fantasy") || t.contains("kingdom hearts") || t.contains("dragon quest") ->
                StressLevel.MEDIUM
            t.contains("zelda") || t.contains("god of war") || t.contains("halo") || t.contains("uncharted") ->
                StressLevel.MEDIUM
            t.contains("mario") || t.contains("kirby") || t.contains("pokémon") || t.contains("pokemon") || t.contains("animal crossing") ->
                StressLevel.LOW
            g.contains("soulslike") || g.contains("roguelike") ->
                StressLevel.HIGH
            g.contains("survival horror") || g.contains("psychological horror") || g.contains("horror") ->
                StressLevel.HIGH
            g.contains("cozy") || g.contains("visual novel") || g.contains("casual") || g.contains("farming") ->
                StressLevel.LOW
            g.contains("jrpg") || g.contains("rpg") || g.contains("action-adventure") || g.contains("platformer") ->
                StressLevel.MEDIUM
            g.contains("fighting") || g.contains("fps") || g.contains("shooter") ->
                StressLevel.MEDIUM
            g.contains("puzzle") || g.contains("strategy") ->
                StressLevel.MEDIUM
            else ->
                StressLevel.MEDIUM
        }
    }
}
