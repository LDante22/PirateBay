package com.example.data.metadata

import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class GameCoverFetcher {

    private val client = OkHttpClient()

    // 1. Funcția principală care aduce HTML-ul prin ScraperAPI
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
                        extractImageUrlFromHtml(htmlBody)
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

    // 2. Metoda de căutare a imaginii în HTML cu Regex
    private fun extractImageUrlFromHtml(html: String): String? {
        val regex = Regex("""src=["']([^"']+\.(?:jpg|jpeg|png))["']""", RegexOption.IGNORE_CASE)
        val matchResult = regex.find(html)
        
        return matchResult?.groups?.get(1)?.value?.let { imgUrl ->
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
