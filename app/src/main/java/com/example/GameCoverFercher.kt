// 1. Funcția principală care interoghează The Cover Project
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

    // 2. Extrage link-ul bazat pe formatul de pe The Cover Project (suportă cover_id)
    private fun extractBestCoverUrl(html: String): String? {
        // Căutăm orice referință de tip view.php?cover_id=XXXXX în rezultatele căutării
        val coverIdRegex = Regex("""view\.php\?cover_id=(\d+)""", RegexOption.IGNORE_CASE)
        val match = coverIdRegex.find(html)
        
        if (match != null) {
            val coverId = match.groups[1]?.value
            // Pe The Cover Project, imaginile la rezoluție mare sunt de obicei salvate în folderul uploads cu ID-ul sau pot fi accesate direct
            // Alternativ, putem returna link-ul paginii de vizualizare sau link-ul direct către imaginea din directorul de uploads
            return "https://www.thecoverproject.net/uploads/$coverId.jpg"
        }

        // Fallback dacă nu găsim un cover_id direct, căutăm imagini obișnuite
        val imgRegex = Regex("""src=["']([^"']*(?:uploads|covers|lores|hires)[^"']+\.(?:jpg|jpeg|png))["']""", RegexOption.IGNORE_CASE)
        val imgMatch = imgRegex.find(html)
        return imgMatch?.groups?.get(1)?.value?.let { fixUrl(it) }
    }

    private fun fixUrl(imgUrl: String): String {
        return if (imgUrl.startsWith("/")) {
            "https://www.thecoverproject.net$imgUrl"
        } else if (!imgUrl.startsWith("http")) {
            "https://www.thecoverproject.net/$imgUrl"
        } else {
            imgUrl
        }
    }
