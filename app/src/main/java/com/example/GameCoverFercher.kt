package com.example.app // Pune pachetul tău real aici

import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class GameCoverFetcher {

    private val client = OkHttpClient()

    fun fetchCoverFromCoverProject(gameTitle: String): String? {
        // 1. URL-ul țintă de pe The Cover Project
        val targetUrl = "https://www.thecoverproject.net/view.php?search=" + 
            URLEncoder.encode(gameTitle, StandardCharsets.UTF_8.toString())

        // 2. Preluarea cheii secrete configurată în siguranță prin BuildConfig
        val scraperApiKey = BuildConfig.SCRAPER_API_KEY
        if (scraperApiKey.isBlank() || scraperApiKey == "YOUR_API_KEY_HERE") {
            return null // Cheia nu este setată corect
        }

        // 3. Construirea URL-ului final prin ScraperAPI pentru a ocoli blocajele
        val encodedTargetUrl = URLEncoder.encode(targetUrl, StandardCharsets.UTF_8.toString())
        val scraperUrl = "https://api.scraperapi.com/?api_key=$scraperApiKey&url=$encodedTargetUrl"

        // 4. Executarea cererii HTTP
        val request = Request.Builder()
            .url(scraperUrl)
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    // Întoarce codul HTML/conținutul paginii curățat de ScraperAPI
                    response.body?.string()
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
