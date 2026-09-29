// 1. Funcția principală care interoghează The Cover Project
 // 1. Caută jocul cu fallback inteligent pe cuvinte cheie dacă nu găsește din prima
    fun fetchCoverFromCoverProject(gameTitle: String): String? {
        val scraperApiKey = BuildConfig.SCRAPER_API_KEY
        if (scraperApiKey.isBlank() || scraperApiKey == "YOUR_API_KEY_HERE") {
            return null
        }

        // Curățăm titlul de caractere speciale sau ediții care ar putea bloca căutarea pe site
        val cleanTitle = gameTitle.replace(Regex("[:\\-,._]"), " ").trim()
        
        // Încercare 1: Căutare cu titlul curățat
        val result = searchAndExtractCover(cleanTitle, scraperApiKey)
        if (result != null) return result

        // Încercare 2: Dacă e un titlu lung, încercăm doar primele 2 cuvinte principale
        val words = cleanTitle.split("\\s+".toRegex())
        if (words.size > 2) {
            val shortTitle = "${words[0]} ${words[1]}"
            return searchAndExtractCover(shortTitle, scraperApiKey)
        }

        return null
    }

   private fun searchAndExtractCover(query: String, apiKey: String): String? {
        val searchUrl = "https://www.thecoverproject.net/view.php?search=" + 
            URLEncoder.encode(query, StandardCharsets.UTF_8.toString())

        val htmlBody = executeScraper(searchUrl, apiKey) ?: return null
        
        // Căutăm în HTML-ul rezultatelor un bloc sau un link care conține titlul căutat sau un cover_id valid asociat
        // Extragem toate perechile de cover_id și textul din jur pentru a găsi potrivirea corectă
        val regex = Regex("""href=["']view\.php\?cover_id=(\d+)["'][^>]*>(.*?)</a>""", RegexOption.IGNORE_CASE)
        val matches = regex.findAll(htmlBody)

        var matchedCoverId: String? = null
        val queryLower = query.lowercase()

        for (match in matches) {
            val coverId = match.groups[1]?.value
            val linkText = match.groups[2]?.value?.lowercase() ?: ""

            // Verificăm dacă textul link-ului sau contextul conține cuvintele principale din căutare
            if (linkText.contains(queryLower) || queryLower.split(" ").all { linkText.contains(it) }) {
                matchedCoverId = coverId
                break
            }
        }

        // Fallback la primul ID găsit doar dacă nu avem altceva, dar preferabil să verificăm
        val finalCoverId = matchedCoverId ?: Regex("""view\.php\?cover_id=(\d+)""", RegexOption.IGNORE_CASE).find(htmlBody)?.groups?.get(1)?.value

        if (finalCoverId != null) {
            val detailUrl = "https://www.thecoverproject.net/view.php?cover_id=$finalCoverId"
            val detailHtml = executeScraper(detailUrl, apiKey)
            if (detailHtml != null) {
                val imageUrl = extractImageFromDetail(detailHtml)
                if (imageUrl != null) return imageUrl
            }
        }

        return extractBestCoverUrl(htmlBody)
    }
    private fun executeScraper(targetUrl: String, apiKey: String): String? {
        val encodedTargetUrl = URLEncoder.encode(targetUrl, StandardCharsets.UTF_8.toString())
        val scraperUrl = "https://api.scraperapi.com/?api_key=$apiKey&url=$encodedTargetUrl"

        val request = Request.Builder().url(scraperUrl).build()
        return try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) response.body?.string() else null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun extractImageFromDetail(html: String): String? {
        val regex = Regex("""src=["']([^"']*(?:uploads|covers|hires|full)[^"']+\.(?:jpg|jpeg|png))["']""", RegexOption.IGNORE_CASE)
        val matches = regex.findAll(html)

        for (match in matches) {
            val imgUrl = match.groups[1]?.value ?: continue
            if (!imgUrl.contains("icon") && !imgUrl.contains("star") && !imgUrl.contains("spacer") && !imgUrl.contains("thumb")) {
                return fixUrl(imgUrl)
            }
        }
        return null
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
