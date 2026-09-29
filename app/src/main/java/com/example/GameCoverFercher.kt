package com.example

import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class GameCoverFetcher {

    private val client = OkHttpClient()

    companion object {
        private const val MOBY_API_KEY = "83dc81bc38c5d20304b2304d8195b8f9dcfb378c6762a072be75f0dc17ff5bbc"

        fun resolveCoverWithDetails(
            title: String,
            console: com.example.data.model.GameConsole?,
            releaseYear: Int,
            existingCoverUrl: String,
            existingCoverSource: String,
            existingBackCoverUrl: String,
            existingCompleteCaseArtwork: String,
            forceRefresh: Boolean = false
        ): CoverResolutionResult {
            val fetcher = GameCoverFetcher()
            val fetchedUrl = fetcher.fetchCoverFromMobyGames(title)
            
            return CoverResolutionResult(
                coverUrl = fetchedUrl ?: existingCoverUrl,
                coverSource = if (fetchedUrl != null) "MobyGames" else existingCoverSource,
                backCoverUrl = existingBackCoverUrl,
                completeCaseArtwork = existingCompleteCaseArtwork
            )
        }
    }

    // Căutare directă și sigură pe MobyGames API v2
    fun fetchCoverFromMobyGames(gameTitle: String): String? {
        val encodedTitle = URLEncoder.encode(gameTitle, StandardCharsets.UTF_8.toString())
        // Folosim endpoint-ul oficial MobyGames cu fuzzy matching pentru a găsi titlul corect și coperțile
        val url = "https://api.mobygames.com/v2/games?title=$encodedTitle&fuzzy=true&include=covers.original&api_key=$MOBY_API_KEY"

        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/json")
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val bodyString = response.body?.string() ?: return null
                
                val json = JSONObject(bodyString)
                val games = json.optJSONArray("games") ?: return null
                
                if (games.length() > 0) {
                    val firstGame = games.getJSONObject(0)
                    val covers = firstGame.optJSONArray("covers")
                    
                    if (covers != null && covers.length() > 0) {
                        // Căutăm preferabil o copertă de față (front cover), sau luăm prima disponibilă
                        for (i in 0 until covers.length()) {
                            val cover = covers.getJSONObject(i)
                            val coverUrl = cover.optString("image", "")
                            val scanInfo = cover.optString("cover_group_code", "").lowercase()
                            
                            if (coverUrl.isNotBlank()) {
                                // Dacă găsim una marcată ca front sau pur și simplu prima validă
                                if (scanInfo.contains("front") || i == 0) {
                                    return coverUrl
                                }
                            }
                        }
                        // Fallback la prima imagine din listă
                        return covers.getJSONObject(0).optString("image", null)
                    }
                }
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
