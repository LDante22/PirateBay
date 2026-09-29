package com.example

import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

data class CoverResolutionResult(
    val coverUrl: String = "",
    val coverThumbUrl: String = "",
    val coverSource: String = "",
    val backCoverUrl: String = "",
    val backCoverSource: String = "",
    val completeCaseArtwork: String = ""
)

class GameCoverFetcher {

    private val client = OkHttpClient()

    companion object {
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
            val fetchedUrl = fetcher.fetchCoverFromCoverProject(title)
            
            return CoverResolutionResult(
                coverUrl = fetchedUrl ?: existingCoverUrl,
                coverSource = if (fetchedUrl != null) "TheCoverProject" else existingCoverSource,
                backCoverUrl = existingBackCoverUrl,
                completeCaseArtwork = existingCompleteCaseArtwork
            )
        }
    }

    // 1. Funcția principală care interoghează The Cover Project prin ScraperAPI
    fun fetchCoverFromCoverProject(gameTitle: String): String? {
        val targetUrl = "https://www.thecoverproject.net/view.php?search=" + 
            URLEncoder.encode(gameTitle, StandardCharsets.UTF_8.toString())

        val scraperApiKey = BuildConfig.SCRAPER_API_KEY
        if (scraperApiKey.isBlank() || scraperApiKey == "YOUR_API_KEY_HERE") {
            return null
        }

        val encodedTargetUrl = URLEncoder.encode(targetUrl, StandardCharsets.UTF_8.toString())
        val scraperUrl = "https://api.scraperapi.com/?api_key=$scraperApiKey&url=$encodedTargetUrl"

        val request = Request.Builder()
            .url(scraperUrl)
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val htmlBody = response.body?.string()
                    if (htmlBody != null) {
                        extractBestCoverUrl(htmlBody)
                    } else {
                        null
                    }
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // 2. Extrage link-ul corect al coperții din rezultatele căutării
    private fun extractBestCoverUrl(html: String): String? {
        // Căutăm tag-uri de imagini care corespund previzualizărilor de coperți de pe The Cover Project
        val regex = Regex("""src=["']([^"']*(?:uploads|covers|lores|hires)[^"']+\.(?:jpg|jpeg|png))["']""", RegexOption.IGNORE_CASE)
        val matches = regex.findAll(html)

        for (match in matches) {
            val imgUrl = match.groups[1]?.value ?: continue
            // Evităm iconițele mici sau stelele de rating
            if (!imgUrl.contains("icon") && !imgUrl.contains("star") && !imgUrl.contains("spacer")) {
                return if (imgUrl.startsWith("/")) {
                    "https://www.thecoverproject.net$imgUrl"
                } else if (!imgUrl.startsWith("http")) {
                    "https://www.thecoverproject.net/$imgUrl"
                } else {
                    imgUrl
                }
            }
        }

        // Fallback la primul link găsit dacă nu se potrivește filtrul de mai sus
        val fallbackRegex = Regex("""src=["']([^"']+\.(?:jpg|jpeg|png))["']""", RegexOption.IGNORE_CASE)
        val fallbackMatch = fallbackRegex.find(html)
        return fallbackMatch?.groups?.get(1)?.value?.let { imgUrl ->
            if (imgUrl.startsWith("/")) {
                "https://www.thecoverproject.net$imgUrl"
            } else if (!imgUrl.startsWith("http")) {
                "https://www.thecoverproject.net/$imgUrl"
            } else {
                imgUrl
            }
        }
    }
}
