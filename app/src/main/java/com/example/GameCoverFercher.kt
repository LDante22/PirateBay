// 2. Extrage link-ul corect și de înaltă rezoluție al coperții
    private fun extractBestCoverUrl(html: String): String? {
        // Căutăm mai întâi link-uri sau imagini care conțin indicii de rezoluție înaltă (hires)
        val hiresRegex = Regex("""(?:src|href)=["']([^"']*(?:hires|full|download)[^"']+\.(?:jpg|jpeg|png))["']""", RegexOption.IGNORE_CASE)
        val hiresMatches = hiresRegex.findAll(html)

        for (match in hiresMatches) {
            val imgUrl = match.groups[1]?.value ?: continue
            if (!imgUrl.contains("icon") && !imgUrl.contains("star") && !imgUrl.contains("spacer") && !imgUrl.contains("thumb")) {
                return fixUrl(imgUrl)
            }
        }

        // Dacă nu găsim hires explicit, căutăm orice imagine din secțiunea de coperți, evitând lores dacă există altceva
        val regex = Regex("""src=["']([^"']*(?:uploads|covers|lores)[^"']+\.(?:jpg|jpeg|png))["']""", RegexOption.IGNORE_CASE)
        val matches = regex.findAll(html)

        var firstValidUrl: String? = null
        for (match in matches) {
            val imgUrl = match.groups[1]?.value ?: continue
            if (!imgUrl.contains("icon") && !imgUrl.contains("star") && !imgUrl.contains("spacer")) {
                val fixed = fixUrl(imgUrl)
                if (firstValidUrl == null) {
                    firstValidUrl = fixed
                }
                // Dacă găsim una care pare mai mare sau nu e specific lores, o preferăm
                if (!imgUrl.contains("lores")) {
                    return fixed
                }
            }
        }

        if (firstValidUrl != null) return firstValidUrl

        // Fallback general
        val fallbackRegex = Regex("""src=["']([^"']+\.(?:jpg|jpeg|png))["']""", RegexOption.IGNORE_CASE)
        val fallbackMatch = fallbackRegex.find(html)
        return fallbackMatch?.groups?.get(1)?.value?.let { fixUrl(it) }
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
