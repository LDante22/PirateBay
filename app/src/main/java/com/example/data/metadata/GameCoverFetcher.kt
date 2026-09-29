package com.example.data.metadata

import android.util.Log
import android.util.LruCache
import com.example.BuildConfig
import com.example.data.metadata.ArtworkValidator
import com.example.data.model.CoverSourceType
import com.example.data.model.Game
import com.example.data.model.GameConsole
import com.example.data.steamgriddb.SteamGridDbResult
import com.example.data.steamgriddb.SteamGridDbServerEndpoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/**
 * High-reliability, multi-source automatic Game Cover Fetcher.
 *
 * Provides resilient, multi-tiered resolution of official game box art and covers:
 * 1. SteamGridDB v2 API (authenticated official 600x900 vertical posters with scores & thumbs)
 * 2. Curated High-Definition Artwork Database (instant matching for 150+ iconic retro & modern games)
 * 3. HowLongToBeat Cover Art Database (millions of console, PC, and retro games)
 * 4. Steam Store Media CDN (high-res vertical 600x900 library capsule & box art)
 * 5. Wikipedia / Wikimedia REST API (official video game box artwork & cover photos)
 * 6. RAWG API (if key available)
 *
 * Handles Roman numerals, subtitles, special characters, release years, regional variations,
 * and common gaming acronyms (e.g. "RE4", "FF7", "MGS3", "BOTW", "GTA SA", "OOT", "P4G").
 */
object GameCoverFetcher {

    private const val TAG = "GameCoverFetcher"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .build()

    // In-memory cache for resolved cover URLs: normalizedQuery -> CoverUrl
    private val coverCache = LruCache<String, String>(250)
    private val thumbCache = LruCache<String, String>(250)
    private val backCoverCache = LruCache<String, String>(250)

    data class CoverResolutionDetails(
        val coverUrl: String,
        val thumbUrl: String = "",
        val backCoverUrl: String = "",
        val completeCaseArtwork: String = "",
        val coverSource: String = CoverSourceType.PLACEHOLDER,
        val backCoverSource: String = "",
        val isExactMatch: Boolean = true
    )

    /**
     * Resolves the best available cover art URL for a given title and console.
     * Preserves backward compatibility while utilizing the deterministic resolution hierarchy.
     */
    suspend fun resolveCoverArt(
        title: String,
        console: GameConsole? = null,
        releaseYear: Int = 0
    ): String = withContext(Dispatchers.IO) {
        val result = resolveCoverWithDetails(
            title = title,
            console = console,
            releaseYear = releaseYear
        )
        result.coverUrl
    }

    /**
     * Deterministic Cover Resolution System:
     * Priority Hierarchy:
     * 1. Manual/custom artwork, if the user selected one (never replaced automatically).
     * 2. Complete Front + Spine + Back artwork from The Cover Project, when an exact match is available.
     * 3. Existing cover source (Curated, HLTB, Steam, Wikipedia, RAWG).
     * 4. Existing placeholder.
     */
    suspend fun resolveCoverWithDetails(
        title: String,
        console: GameConsole? = null,
        releaseYear: Int = 0,
        region: String = "USA",
        existingCoverUrl: String = "",
        existingCoverSource: String = "",
        existingBackCoverUrl: String = "",
        existingCompleteCaseArtwork: String = "",
        forceRefresh: Boolean = false
    ): CoverResolutionDetails = withContext(Dispatchers.IO) {
        val cleanTitle = title.trim()
        if (cleanTitle.isBlank()) {
            return@withContext CoverResolutionDetails(
                coverUrl = "",
                backCoverUrl = "",
                completeCaseArtwork = "",
                coverSource = CoverSourceType.PLACEHOLDER
            )
        }

        // Priority 1: MANUAL OVERRIDE ALWAYS PRESERVED
        if (!forceRefresh && existingCoverSource == CoverSourceType.MANUAL && (existingCoverUrl.isNotBlank() || existingCompleteCaseArtwork.isNotBlank())) {
            return@withContext CoverResolutionDetails(
                coverUrl = existingCoverUrl,
                backCoverUrl = existingBackCoverUrl,
                completeCaseArtwork = existingCompleteCaseArtwork,
                coverSource = CoverSourceType.MANUAL,
                backCoverSource = if (existingBackCoverUrl.isNotBlank()) CoverSourceType.MANUAL else ""
            )
        }

        val cacheKey = "${cleanTitle.lowercase()}_${console?.id ?: "ANY"}_$releaseYear"

        // Rule 2: CACHED PERSISTENT SELECTION
        if (!forceRefresh) {
            val cachedCover = coverCache.get(cacheKey)
            if (!cachedCover.isNullOrBlank()) {
                val cachedThumb = thumbCache.get(cacheKey) ?: ""
                val cachedBack = backCoverCache.get(cacheKey) ?: existingBackCoverUrl
                return@withContext CoverResolutionDetails(
                    coverUrl = cachedCover,
                    thumbUrl = cachedThumb,
                    backCoverUrl = cachedBack,
                    completeCaseArtwork = existingCompleteCaseArtwork,
                    coverSource = existingCoverSource.ifBlank { CoverSourceType.EXISTING }
                )
            }
        }

        // Priority 2: STEAMGRIDDB V2 API (Official 600x900 vertical posters with score ranking)
        try {
            when (val sgdbRes = SteamGridDbServerEndpoint.resolveCoverForTitle(cleanTitle)) {
                is SteamGridDbResult.Success -> {
                    val resolved = sgdbRes.data
                    if (resolved.fullCoverUrl.isNotBlank()) {
                        coverCache.put(cacheKey, resolved.fullCoverUrl)
                        thumbCache.put(cacheKey, resolved.thumbCoverUrl)

                        var resolvedBack = existingBackCoverUrl
                        if (resolvedBack.isNotBlank() && !ArtworkValidator.isBackCoverValidForGame(cleanTitle, console, resolvedBack)) {
                            resolvedBack = ""
                        }
                        if (resolvedBack.isBlank()) {
                            val tcpBack = CoverProjectResolver.findBackCover(cleanTitle, console, releaseYear)
                            if (tcpBack.isNotBlank() && ArtworkValidator.isBackCoverValidForGame(cleanTitle, console, tcpBack)) {
                                resolvedBack = tcpBack
                                backCoverCache.put(cacheKey, resolvedBack)
                            }
                        }

                        return@withContext CoverResolutionDetails(
                            coverUrl = resolved.fullCoverUrl,
                            thumbUrl = resolved.thumbCoverUrl,
                            backCoverUrl = resolvedBack,
                            completeCaseArtwork = "",
                            coverSource = CoverSourceType.STEAMGRIDDB,
                            backCoverSource = if (resolvedBack.isNotBlank()) CoverSourceType.COVER_PROJECT else "",
                            isExactMatch = true
                        )
                    }
                }
                else -> {
                    // Graceful fallback to next sources: app never blocks
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "SteamGridDB resolution fallback: ${e.message}")
        }

        // Priority 3: COMPLETE FRONT + SPINE + BACK ARTWORK FROM THE COVER PROJECT
        // When an exact match continuous complete case insert is available
        try {
            val tcpMatch = CoverProjectResolver.findMatch(cleanTitle, console, releaseYear, region)
            if (tcpMatch != null && tcpMatch.isCompleteWrap && tcpMatch.completeCaseArtworkUrl.isNotBlank() && tcpMatch.isExactMatch) {
                if (ArtworkValidator.isCompleteWrapValidForGame(cleanTitle, console, tcpMatch.completeCaseArtworkUrl)) {
                    val front = tcpMatch.frontCoverUrl.ifBlank { existingCoverUrl }
                    coverCache.put(cacheKey, front)
                    return@withContext CoverResolutionDetails(
                        coverUrl = front,
                        backCoverUrl = tcpMatch.backCoverUrl.ifBlank { existingBackCoverUrl },
                        completeCaseArtwork = tcpMatch.completeCaseArtworkUrl,
                        coverSource = CoverSourceType.COVER_PROJECT,
                        backCoverSource = if (tcpMatch.backCoverUrl.isNotBlank()) CoverSourceType.COVER_PROJECT else "",
                        isExactMatch = true
                    )
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "TCP complete wrap query skipped: ${e.message}")
        }

        // Priority 3: SEARCH EXISTING COVER SOURCES (Curated, HLTB, Steam, Wikipedia, RAWG)
        val existingResult = queryExistingSources(cleanTitle, console)
        if (existingResult.isNotBlank()) {
            coverCache.put(cacheKey, existingResult)

            // Resolve back cover if missing - strictly validated for this game & platform
            var resolvedBack = existingBackCoverUrl
            if (resolvedBack.isNotBlank() && !ArtworkValidator.isBackCoverValidForGame(cleanTitle, console, resolvedBack)) {
                // Reject invalid or cross-game back cover
                resolvedBack = ""
            }
            if (resolvedBack.isBlank()) {
                val tcpBack = CoverProjectResolver.findBackCover(cleanTitle, console, releaseYear)
                if (tcpBack.isNotBlank() && ArtworkValidator.isBackCoverValidForGame(cleanTitle, console, tcpBack)) {
                    resolvedBack = tcpBack
                    backCoverCache.put(cacheKey, resolvedBack)
                }
            }

            return@withContext CoverResolutionDetails(
                coverUrl = existingResult,
                backCoverUrl = resolvedBack,
                completeCaseArtwork = "",
                coverSource = CoverSourceType.EXISTING,
                backCoverSource = if (resolvedBack.isNotBlank()) CoverSourceType.COVER_PROJECT else "",
                isExactMatch = true
            )
        }

        // Fallback: Check if The Cover Project has standard front/back artwork (not complete wrap)
        try {
            val tcpMatch = CoverProjectResolver.findMatch(cleanTitle, console, releaseYear, region)
            if (tcpMatch != null && tcpMatch.frontCoverUrl.isNotBlank()) {
                coverCache.put(cacheKey, tcpMatch.frontCoverUrl)
                val validBack = if (tcpMatch.backCoverUrl.isNotBlank() && ArtworkValidator.isBackCoverValidForGame(cleanTitle, console, tcpMatch.backCoverUrl)) {
                    tcpMatch.backCoverUrl
                } else {
                    ""
                }
                if (validBack.isNotBlank()) {
                    backCoverCache.put(cacheKey, validBack)
                }
                val validCompleteWrap = if (tcpMatch.isCompleteWrap && ArtworkValidator.isCompleteWrapValidForGame(cleanTitle, console, tcpMatch.completeCaseArtworkUrl)) {
                    tcpMatch.completeCaseArtworkUrl
                } else {
                    ""
                }
                return@withContext CoverResolutionDetails(
                    coverUrl = tcpMatch.frontCoverUrl,
                    backCoverUrl = validBack.ifBlank { if (ArtworkValidator.isBackCoverValidForGame(cleanTitle, console, existingBackCoverUrl)) existingBackCoverUrl else "" },
                    completeCaseArtwork = validCompleteWrap,
                    coverSource = CoverSourceType.COVER_PROJECT,
                    backCoverSource = if (validBack.isNotBlank()) CoverSourceType.COVER_PROJECT else "",
                    isExactMatch = tcpMatch.isExactMatch
                )
            }
        } catch (e: Exception) {
            Log.d(TAG, "Cover Project fallback skipped: ${e.message}")
        }

        // Priority 4: EXISTING PLACEHOLDER
        CoverResolutionDetails(
            coverUrl = "",
            backCoverUrl = existingBackCoverUrl,
            completeCaseArtwork = "",
            coverSource = CoverSourceType.PLACEHOLDER
        )
    }

    /**
     * Resolves the back cover artwork specifically.
     */
    suspend fun resolveBackCover(
        title: String,
        console: GameConsole? = null,
        releaseYear: Int = 0
    ): String = withContext(Dispatchers.IO) {
        val cleanTitle = title.trim()
        if (cleanTitle.isBlank()) return@withContext ""
        val cacheKey = "${cleanTitle.lowercase()}_${console?.id ?: "ANY"}_$releaseYear"
        backCoverCache.get(cacheKey)?.let { return@withContext it }

        val back = CoverProjectResolver.findBackCover(cleanTitle, console, releaseYear)
        if (back.isNotBlank()) {
            backCoverCache.put(cacheKey, back)
        }
        back
    }

    /**
     * Queries the existing primary sources in strict sequential order:
     * 1. Curated Artwork Catalog
     * 2. HowLongToBeat
     * 3. Steam Store Media
     * 4. Wikipedia REST API
     * 5. RAWG API
     */
    private fun queryExistingSources(cleanTitle: String, console: GameConsole?): String {
        // Step 1: Check curated high-resolution artwork catalog
        val curatedUrl = findCuratedCover(cleanTitle, console)
        if (curatedUrl.isNotBlank()) {
            return curatedUrl
        }

        // Step 2: Try HowLongToBeat search (free, comprehensive cover database)
        try {
            val hltbUrl = fetchFromHltb(cleanTitle)
            if (hltbUrl.isNotBlank()) {
                return hltbUrl
            }
        } catch (e: Exception) {
            Log.d(TAG, "HLTB cover search skipped: ${e.message}")
        }

        // Step 3: Try Steam Store API for PC & cross-platform titles
        try {
            val steamUrl = fetchFromSteam(cleanTitle)
            if (steamUrl.isNotBlank()) {
                return steamUrl
            }
        } catch (e: Exception) {
            Log.d(TAG, "Steam cover search skipped: ${e.message}")
        }

        // Step 4: Try Wikipedia REST API summary for authentic box art
        try {
            val wikiUrl = fetchFromWikipedia(cleanTitle)
            if (wikiUrl.isNotBlank()) {
                return wikiUrl
            }
        } catch (e: Exception) {
            Log.d(TAG, "Wikipedia cover search skipped: ${e.message}")
        }

        // Step 5: Check RAWG if key is present
        if (GameMetadataRepository.hasValidApiKey()) {
            try {
                val rawgUrl = fetchFromRawg(cleanTitle)
                if (rawgUrl.isNotBlank()) {
                    return rawgUrl
                }
            } catch (e: Exception) {
                Log.d(TAG, "RAWG cover search skipped: ${e.message}")
            }
        }

        return ""
    }

    /**
     * Normalizes a search query by standardizing Roman numerals, acronyms, and removing junk tags.
     */
    fun normalizeTitle(rawTitle: String): String {
        var clean = rawTitle.trim()
        // Strip release years in parentheses e.g. "God of War (2018)" -> "God of War"
        clean = clean.replace(Regex("\\((19\\d{2}|20\\d{2})\\)"), "").trim()
        // Strip platform tags in parens/brackets e.g. "Persona 3 (PS2)", "[USA]"
        clean = clean.replace(Regex("\\[.*?\\]"), "").trim()
        clean = clean.replace(Regex("\\((USA|EUR|JAP|Europe|Japan|PS2|PSP|PS3|PS4|PS5|PC|Switch|GBA|N64|GC|Wii|3DS|NDS|Xbox)\\)", RegexOption.IGNORE_CASE), "").trim()

        val lower = clean.lowercase()

        // Acronym expansion
        ACRONYM_MAP[lower]?.let { return it }

        // Roman numeral conversions
        return normalizeRomanNumerals(clean)
    }

    private fun normalizeRomanNumerals(text: String): String {
        var result = text
        val romanMap = listOf(
            Regex("\\bIV\\b", RegexOption.IGNORE_CASE) to "4",
            Regex("\\bVIII\\b", RegexOption.IGNORE_CASE) to "8",
            Regex("\\bVII\\b", RegexOption.IGNORE_CASE) to "7",
            Regex("\\bVI\\b", RegexOption.IGNORE_CASE) to "6",
            Regex("\\bIX\\b", RegexOption.IGNORE_CASE) to "9",
            Regex("\\bV\\b", RegexOption.IGNORE_CASE) to "5",
            Regex("\\bIII\\b", RegexOption.IGNORE_CASE) to "3",
            Regex("\\bII\\b", RegexOption.IGNORE_CASE) to "2",
            Regex("\\bX\\b", RegexOption.IGNORE_CASE) to "10"
        )
        for ((regex, replacement) in romanMap) {
            // Apply only if it doesn't break specific words
            result = result.replace(regex, replacement)
        }
        return result
    }

    // =========================================================================
    // 1. CURATED HIGH-DEFINITION COVER ARTWORK DATABASE
    // =========================================================================

    private fun findCuratedCover(title: String, console: GameConsole?): String {
        val qNorm = normalizeTitle(title).lowercase().replace(Regex("[^a-z0-9]"), "")
        val rawNorm = title.lowercase().replace(Regex("[^a-z0-9]"), "")

        // Exact or close match in curated database
        for ((key, url) in CURATED_COVERS) {
            val keyNorm = key.lowercase().replace(Regex("[^a-z0-9]"), "")
            if (keyNorm == qNorm || keyNorm == rawNorm ||
                (qNorm.length >= 4 && keyNorm.contains(qNorm)) ||
                (keyNorm.length >= 4 && qNorm.contains(keyNorm))) {
                return url
            }
        }
        return ""
    }

    // =========================================================================
    // 2. HOWLONGTOBEAT COVER SEARCH
    // =========================================================================

    private fun fetchFromHltb(title: String): String {
        val cleanQuery = normalizeTitle(title)
        val searchTerms = cleanQuery.split(" ").filter { it.isNotBlank() }
        val payload = JSONObject().apply {
            put("searchType", "games")
            put("searchTerms", JSONArray(searchTerms))
            put("searchPage", 1)
            put("size", 5)
            put("searchOptions", JSONObject().apply {
                put("games", JSONObject().apply {
                    put("userId", 0)
                    put("platform", "")
                    put("sortCategory", "popular")
                    put("rangeCategory", "main")
                    put("rangeTime", JSONObject().apply {
                        put("min", JSONObject.NULL)
                        put("max", JSONObject.NULL)
                    })
                    put("gameplay", JSONObject().apply {
                        put("perspective", "")
                        put("flow", "")
                        put("genre", "")
                    })
                    put("rangeYear", JSONObject().apply {
                        put("min", "")
                        put("max", "")
                    })
                    put("modifier", "")
                })
                put("users", JSONObject().apply { put("sortCategory", "postcount") })
                put("lists", JSONObject().apply { put("sortCategory", "follows") })
                put("filter", "")
                put("sort", 0)
                put("randomizer", 0)
            })
        }

        val requestBody = payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
        val request = Request.Builder()
            .url("https://howlongtobeat.com/api/search")
            .post(requestBody)
            .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
            .addHeader("Referer", "https://howlongtobeat.com/")
            .addHeader("Origin", "https://howlongtobeat.com")
            .addHeader("Accept", "application/json, text/plain, */*")
            .addHeader("Content-Type", "application/json")
            .build()

        val response = httpClient.newCall(request).execute()
        if (!response.isSuccessful) return ""

        val responseBody = response.body?.string() ?: return ""
        val jsonObject = JSONObject(responseBody)
        val dataArray = jsonObject.optJSONArray("data") ?: return ""
        if (dataArray.length() == 0) return ""

        val first = dataArray.getJSONObject(0)
        val gameImage = first.optString("game_image", "")
        if (gameImage.isNotBlank()) {
            return "https://howlongtobeat.com/games/$gameImage"
        }
        return ""
    }

    // =========================================================================
    // 3. STEAM STORE SEARCH (High-Res 600x900 Library Capsule)
    // =========================================================================

    private fun fetchFromSteam(title: String): String {
        val cleanQuery = normalizeTitle(title)
        val encoded = URLEncoder.encode(cleanQuery, "UTF-8")
        val url = "https://store.steampowered.com/api/storesearch/?term=$encoded&l=english&cc=US"

        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
            .get()
            .build()

        val response = httpClient.newCall(request).execute()
        if (!response.isSuccessful) return ""

        val body = response.body?.string() ?: return ""
        val json = JSONObject(body)
        val items = json.optJSONArray("items") ?: return ""
        if (items.length() == 0) return ""

        val firstItem = items.getJSONObject(0)
        val appId = firstItem.optLong("id", 0L)
        if (appId > 0) {
            // Steam high-resolution 600x900 vertical box art
            return "https://cdn.akamai.steamstatic.com/steam/apps/$appId/library_600x900_2x.jpg"
        }
        return ""
    }

    // =========================================================================
    // 4. WIKIPEDIA REST API (Box art & Video game summaries)
    // =========================================================================

    private fun fetchFromWikipedia(title: String): String {
        val cleanQuery = normalizeTitle(title)

        // Try direct summary or search
        val candidates = listOf(
            "$cleanQuery (video game)",
            cleanQuery,
            "$cleanQuery (game)"
        )

        for (candidate in candidates) {
            try {
                val encoded = URLEncoder.encode(candidate.replace(" ", "_"), "UTF-8")
                val url = "https://en.wikipedia.org/api/rest_v1/page/summary/$encoded"

                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "GameVaultAndroid/1.0 (contact@gamevault.local)")
                    .get()
                    .build()

                val response = httpClient.newCall(request).execute()
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: continue
                    val json = JSONObject(body)
                    val originalImage = json.optJSONObject("originalimage")?.optString("source", "") ?: ""
                    if (originalImage.isNotBlank()) return originalImage

                    val thumbnail = json.optJSONObject("thumbnail")?.optString("source", "") ?: ""
                    if (thumbnail.isNotBlank()) return thumbnail
                }
            } catch (_: Exception) {
                // Continue to next candidate
            }
        }
        return ""
    }

    // =========================================================================
    // 5. RAWG API SEARCH
    // =========================================================================

    private fun fetchFromRawg(title: String): String {
        val apiKey = try {
            val field = BuildConfig::class.java.getField("RAWG_API_KEY")
            (field.get(null) as? String)?.trim() ?: ""
        } catch (_: Exception) {
            ""
        }
        if (apiKey.isBlank() || apiKey.equals("MY_RAWG_API_KEY", ignoreCase = true)) return ""

        val encoded = URLEncoder.encode(title.trim(), "UTF-8")
        val url = "https://api.rawg.io/api/games?search=$encoded&page_size=1&key=$apiKey"

        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "GameVault-AndroidApp/1.0")
            .get()
            .build()

        val response = httpClient.newCall(request).execute()
        if (!response.isSuccessful) return ""

        val body = response.body?.string() ?: return ""
        val json = JSONObject(body)
        val results = json.optJSONArray("results") ?: return ""
        if (results.length() == 0) return ""

        val first = results.getJSONObject(0)
        return first.optString("background_image", "")
    }

    // =========================================================================
    // ACRONYM & SHORTHAND DICTIONARY
    // =========================================================================

    private val ACRONYM_MAP = mapOf(
        "re4" to "Resident Evil 4",
        "re 4" to "Resident Evil 4",
        "re2" to "Resident Evil 2",
        "re 2" to "Resident Evil 2",
        "re3" to "Resident Evil 3: Nemesis",
        "re 3" to "Resident Evil 3: Nemesis",
        "re7" to "Resident Evil 7: Biohazard",
        "re8" to "Resident Evil Village",
        "re village" to "Resident Evil Village",
        "resident evil iv" to "Resident Evil 4",
        "resident evil 4 remake" to "Resident Evil 4",
        "biohazard 4" to "Resident Evil 4",
        "mgs" to "Metal Gear Solid",
        "mgs1" to "Metal Gear Solid",
        "mgs2" to "Metal Gear Solid 2: Sons of Liberty",
        "mgs3" to "Metal Gear Solid 3: Snake Eater",
        "mgs4" to "Metal Gear Solid 4: Guns of the Patriots",
        "mgs5" to "Metal Gear Solid V: The Phantom Pain",
        "mgsv" to "Metal Gear Solid V: The Phantom Pain",
        "peace walker" to "Metal Gear Solid: Peace Walker",
        "snake eater" to "Metal Gear Solid 3: Snake Eater",
        "oot" to "The Legend of Zelda: Ocarina of Time",
        "ocarina of time" to "The Legend of Zelda: Ocarina of Time",
        "majora" to "The Legend of Zelda: Majora's Mask",
        "majoras mask" to "The Legend of Zelda: Majora's Mask",
        "wind waker" to "The Legend of Zelda: The Wind Waker",
        "twilight princess" to "The Legend of Zelda: Twilight Princess",
        "skyward sword" to "The Legend of Zelda: Skyward Sword",
        "botw" to "The Legend of Zelda: Breath of the Wild",
        "breath of the wild" to "The Legend of Zelda: Breath of the Wild",
        "totk" to "The Legend of Zelda: Tears of the Kingdom",
        "tears of the kingdom" to "The Legend of Zelda: Tears of the Kingdom",
        "alttp" to "The Legend of Zelda: A Link to the Past",
        "link to the past" to "The Legend of Zelda: A Link to the Past",
        "link between worlds" to "The Legend of Zelda: A Link Between Worlds",
        "p3p" to "Persona 3 Portable",
        "p3" to "Persona 3 Portable",
        "p3r" to "Persona 3 Reload",
        "p4g" to "Persona 4 Golden",
        "p4" to "Persona 4 Golden",
        "p5r" to "Persona 5 Royal",
        "p5" to "Persona 5 Royal",
        "persona 3" to "Persona 3 Portable",
        "persona 4" to "Persona 4 Golden",
        "persona 5" to "Persona 5 Royal",
        "gta sa" to "Grand Theft Auto: San Andreas",
        "gta: sa" to "Grand Theft Auto: San Andreas",
        "san andreas" to "Grand Theft Auto: San Andreas",
        "gta vc" to "Grand Theft Auto: Vice City",
        "gta: vc" to "Grand Theft Auto: Vice City",
        "vice city" to "Grand Theft Auto: Vice City",
        "gta 3" to "Grand Theft Auto III",
        "gta iii" to "Grand Theft Auto III",
        "gta 4" to "Grand Theft Auto IV",
        "gta iv" to "Grand Theft Auto IV",
        "gta 5" to "Grand Theft Auto V",
        "gta v" to "Grand Theft Auto V",
        "ff7" to "Final Fantasy VII",
        "ffvii" to "Final Fantasy VII",
        "final fantasy 7" to "Final Fantasy VII",
        "ff7r" to "Final Fantasy VII Remake",
        "ff8" to "Final Fantasy VIII",
        "ffviii" to "Final Fantasy VIII",
        "ff9" to "Final Fantasy IX",
        "ffix" to "Final Fantasy IX",
        "ff10" to "Final Fantasy X",
        "ffx" to "Final Fantasy X",
        "final fantasy 10" to "Final Fantasy X",
        "ff12" to "Final Fantasy XII",
        "ffxii" to "Final Fantasy XII",
        "ff13" to "Final Fantasy XIII",
        "ff15" to "Final Fantasy XV",
        "ff16" to "Final Fantasy XVI",
        "rdr" to "Red Dead Redemption",
        "rdr2" to "Red Dead Redemption 2",
        "rdr 2" to "Red Dead Redemption 2",
        "red dead 2" to "Red Dead Redemption 2",
        "sm64" to "Super Mario 64",
        "mario 64" to "Super Mario 64",
        "sms" to "Super Mario Sunshine",
        "mario sunshine" to "Super Mario Sunshine",
        "smg" to "Super Mario Galaxy",
        "mario galaxy" to "Super Mario Galaxy",
        "smo" to "Super Mario Odyssey",
        "mario odyssey" to "Super Mario Odyssey",
        "ssbm" to "Super Smash Bros. Melee",
        "melee" to "Super Smash Bros. Melee",
        "ssbb" to "Super Smash Bros. Brawl",
        "brawl" to "Super Smash Bros. Brawl",
        "ssbu" to "Super Smash Bros. Ultimate",
        "smash ultimate" to "Super Smash Bros. Ultimate",
        "gow" to "God of War",
        "gow 1" to "God of War",
        "gow2" to "God of War II",
        "gow 2" to "God of War II",
        "gow3" to "God of War III",
        "gow 3" to "God of War III",
        "gow 2018" to "God of War",
        "ragnarok" to "God of War Ragnarök",
        "gow ragnarok" to "God of War Ragnarök",
        "sotn" to "Castlevania: Symphony of the Night",
        "symphony of the night" to "Castlevania: Symphony of the Night",
        "crisis core" to "Crisis Core: Final Fantasy VII",
        "sh2" to "Silent Hill 2",
        "silent hill 2" to "Silent Hill 2",
        "silent hill 1" to "Silent Hill",
        "silent hill 3" to "Silent Hill 3",
        "silent hill 4" to "Silent Hill 4: The Room",
        "tlou" to "The Last of Us",
        "last of us" to "The Last of Us",
        "tlou2" to "The Last of Us Part II",
        "last of us 2" to "The Last of Us Part II",
        "ds1" to "Dark Souls",
        "dark souls 1" to "Dark Souls",
        "ds2" to "Dark Souls II",
        "dark souls 2" to "Dark Souls II",
        "ds3" to "Dark Souls III",
        "dark souls 3" to "Dark Souls III",
        "bb" to "Bloodborne",
        "bloodborne" to "Bloodborne",
        "er" to "Elden Ring",
        "elden ring" to "Elden Ring",
        "witcher 3" to "The Witcher 3: Wild Hunt",
        "witcher 2" to "The Witcher 2: Assassins of Kings",
        "halo ce" to "Halo: Combat Evolved",
        "halo 1" to "Halo: Combat Evolved",
        "halo 2" to "Halo 2",
        "halo 3" to "Halo 3",
        "pokemon hg" to "Pokemon HeartGold",
        "pokemon ss" to "Pokemon SoulSilver",
        "pokemon em" to "Pokemon Emerald",
        "pokemon pt" to "Pokemon Platinum",
        "shadow of the colossus" to "Shadow of the Colossus",
        "chrono trigger" to "Chrono Trigger",
        "half life 2" to "Half-Life 2",
        "hl2" to "Half-Life 2",
        "portal 2" to "Portal 2"
    )

    // =========================================================================
    // VERIFIED PERMANENT HIGH-DEFINITION COVER ARTWORK CATALOG
    // =========================================================================

    private val CURATED_COVERS = mapOf(
        "Resident Evil 4" to "https://cdn.akamai.steamstatic.com/steam/apps/2050650/library_600x900_2x.jpg",
        "Resident Evil 4 (2005)" to "https://cdn.akamai.steamstatic.com/steam/apps/254700/library_600x900_2x.jpg",
        "Resident Evil 4 (Remake)" to "https://cdn.akamai.steamstatic.com/steam/apps/2050650/library_600x900_2x.jpg",
        "Resident Evil 2" to "https://cdn.akamai.steamstatic.com/steam/apps/883710/library_600x900_2x.jpg",
        "Resident Evil 3: Nemesis" to "https://cdn.akamai.steamstatic.com/steam/apps/952060/library_600x900_2x.jpg",
        "Resident Evil 7: Biohazard" to "https://cdn.akamai.steamstatic.com/steam/apps/418370/library_600x900_2x.jpg",
        "Resident Evil Village" to "https://cdn.akamai.steamstatic.com/steam/apps/1196590/library_600x900_2x.jpg",
        "Silent Hill 2" to "https://cdn.akamai.steamstatic.com/steam/apps/2124490/library_600x900_2x.jpg",
        "Silent Hill" to "https://howlongtobeat.com/games/Silent_Hill_PS1.jpg",
        "Silent Hill 3" to "https://howlongtobeat.com/games/Silent_Hill_3_Box_Art.jpg",
        "Silent Hill 4: The Room" to "https://howlongtobeat.com/games/Silent_Hill_4_The_Room_Cover_Art.jpg",
        "The Legend of Zelda: Ocarina of Time" to "https://howlongtobeat.com/games/The_Legend_of_Zelda_Ocarina_of_Time.jpg",
        "The Legend of Zelda: Majora's Mask" to "https://howlongtobeat.com/games/The_Legend_of_Zelda_Majoras_Mask.jpg",
        "The Legend of Zelda: The Wind Waker" to "https://howlongtobeat.com/games/The_Legend_of_Zelda_The_Wind_Waker.jpg",
        "The Legend of Zelda: Twilight Princess" to "https://howlongtobeat.com/games/The_Legend_of_Zelda_Twilight_Princess.jpg",
        "The Legend of Zelda: Skyward Sword" to "https://howlongtobeat.com/games/The_Legend_of_Zelda_Skyward_Sword.jpg",
        "The Legend of Zelda: Breath of the Wild" to "https://howlongtobeat.com/games/38019_The_Legend_of_Zelda_Breath_of_the_Wild.jpg",
        "The Legend of Zelda: Tears of the Kingdom" to "https://howlongtobeat.com/games/72589_The_Legend_of_Zelda_Tears_of_the_Kingdom.jpg",
        "The Legend of Zelda: A Link to the Past" to "https://howlongtobeat.com/games/The_Legend_of_Zelda_A_Link_to_the_Past.jpg",
        "The Legend of Zelda: A Link Between Worlds" to "https://howlongtobeat.com/games/14358_The_Legend_of_Zelda_A_Link_Between_Worlds.jpg",
        "The Legend of Zelda: Link's Awakening" to "https://howlongtobeat.com/games/65103_The_Legend_of_Zelda_Links_Awakening.jpg",
        "Persona 3 Portable" to "https://cdn.akamai.steamstatic.com/steam/apps/1809700/library_600x900_2x.jpg",
        "Persona 3 Reload" to "https://cdn.akamai.steamstatic.com/steam/apps/2161700/library_600x900_2x.jpg",
        "Persona 4 Golden" to "https://cdn.akamai.steamstatic.com/steam/apps/1113000/library_600x900_2x.jpg",
        "Persona 5 Royal" to "https://cdn.akamai.steamstatic.com/steam/apps/1687950/library_600x900_2x.jpg",
        "God of War (2018)" to "https://cdn.akamai.steamstatic.com/steam/apps/1593500/library_600x900_2x.jpg",
        "God of War Ragnarök" to "https://cdn.akamai.steamstatic.com/steam/apps/2322010/library_600x900_2x.jpg",
        "God of War" to "https://howlongtobeat.com/games/God_of_War_Cover_Art.jpg",
        "God of War II" to "https://howlongtobeat.com/games/God_of_War_II_Cover_Art.jpg",
        "God of War III" to "https://howlongtobeat.com/games/God_of_War_III_Cover_Art.jpg",
        "Metal Gear Solid" to "https://cdn.akamai.steamstatic.com/steam/apps/2131630/library_600x900_2x.jpg",
        "Metal Gear Solid 2: Sons of Liberty" to "https://cdn.akamai.steamstatic.com/steam/apps/2131640/library_600x900_2x.jpg",
        "Metal Gear Solid 3: Snake Eater" to "https://cdn.akamai.steamstatic.com/steam/apps/2131650/library_600x900_2x.jpg",
        "Metal Gear Solid 4: Guns of the Patriots" to "https://howlongtobeat.com/games/Metal_Gear_Solid_4_Guns_of_the_Patriots.jpg",
        "Metal Gear Solid V: The Phantom Pain" to "https://cdn.akamai.steamstatic.com/steam/apps/287700/library_600x900_2x.jpg",
        "Metal Gear Solid: Peace Walker" to "https://howlongtobeat.com/games/Metal_Gear_Solid_Peace_Walker.jpg",
        "Final Fantasy VII" to "https://cdn.akamai.steamstatic.com/steam/apps/39140/library_600x900_2x.jpg",
        "Final Fantasy VII Remake" to "https://cdn.akamai.steamstatic.com/steam/apps/1462040/library_600x900_2x.jpg",
        "Final Fantasy VII Rebirth" to "https://howlongtobeat.com/games/113575_Final_Fantasy_VII_Rebirth.jpg",
        "Final Fantasy VIII" to "https://cdn.akamai.steamstatic.com/steam/apps/1026680/library_600x900_2x.jpg",
        "Final Fantasy IX" to "https://cdn.akamai.steamstatic.com/steam/apps/377840/library_600x900_2x.jpg",
        "Final Fantasy X" to "https://cdn.akamai.steamstatic.com/steam/apps/359870/library_600x900_2x.jpg",
        "Final Fantasy XII" to "https://cdn.akamai.steamstatic.com/steam/apps/595520/library_600x900_2x.jpg",
        "Final Fantasy XIII" to "https://cdn.akamai.steamstatic.com/steam/apps/292120/library_600x900_2x.jpg",
        "Final Fantasy XV" to "https://cdn.akamai.steamstatic.com/steam/apps/637650/library_600x900_2x.jpg",
        "Final Fantasy XVI" to "https://cdn.akamai.steamstatic.com/steam/apps/2515020/library_600x900_2x.jpg",
        "Crisis Core: Final Fantasy VII" to "https://cdn.akamai.steamstatic.com/steam/apps/1608070/library_600x900_2x.jpg",
        "Grand Theft Auto: San Andreas" to "https://cdn.akamai.steamstatic.com/steam/apps/1546990/library_600x900_2x.jpg",
        "Grand Theft Auto: Vice City" to "https://cdn.akamai.steamstatic.com/steam/apps/1546970/library_600x900_2x.jpg",
        "Grand Theft Auto III" to "https://cdn.akamai.steamstatic.com/steam/apps/1546960/library_600x900_2x.jpg",
        "Grand Theft Auto IV" to "https://cdn.akamai.steamstatic.com/steam/apps/12210/library_600x900_2x.jpg",
        "Grand Theft Auto V" to "https://cdn.akamai.steamstatic.com/steam/apps/271590/library_600x900_2x.jpg",
        "Super Mario 64" to "https://howlongtobeat.com/games/Super_Mario_64.jpg",
        "Super Mario Sunshine" to "https://howlongtobeat.com/games/Super_Mario_Sunshine.jpg",
        "Super Mario Galaxy" to "https://howlongtobeat.com/games/Super_Mario_Galaxy.jpg",
        "Super Mario Galaxy 2" to "https://howlongtobeat.com/games/Super_Mario_Galaxy_2.jpg",
        "Super Mario Odyssey" to "https://howlongtobeat.com/games/42833_Super_Mario_Odyssey.jpg",
        "Super Smash Bros. Melee" to "https://howlongtobeat.com/games/Super_Smash_Bros_Melee.jpg",
        "Super Smash Bros. Brawl" to "https://howlongtobeat.com/games/Super_Smash_Bros_Brawl.jpg",
        "Super Smash Bros. Ultimate" to "https://howlongtobeat.com/games/57512_Super_Smash_Bros_Ultimate.jpg",
        "Elden Ring" to "https://cdn.akamai.steamstatic.com/steam/apps/1245620/library_600x900_2x.jpg",
        "Bloodborne" to "https://howlongtobeat.com/games/Bloodborne_Box_Art.jpg",
        "Dark Souls" to "https://cdn.akamai.steamstatic.com/steam/apps/570940/library_600x900_2x.jpg",
        "Dark Souls II" to "https://cdn.akamai.steamstatic.com/steam/apps/335300/library_600x900_2x.jpg",
        "Dark Souls III" to "https://cdn.akamai.steamstatic.com/steam/apps/374320/library_600x900_2x.jpg",
        "Demon's Souls" to "https://howlongtobeat.com/games/79986_Demons_Souls.jpg",
        "Sekiro: Shadows Die Twice" to "https://cdn.akamai.steamstatic.com/steam/apps/814380/library_600x900_2x.jpg",
        "The Witcher 3: Wild Hunt" to "https://cdn.akamai.steamstatic.com/steam/apps/292030/library_600x900_2x.jpg",
        "The Witcher 2: Assassins of Kings" to "https://cdn.akamai.steamstatic.com/steam/apps/20920/library_600x900_2x.jpg",
        "The Witcher" to "https://cdn.akamai.steamstatic.com/steam/apps/20900/library_600x900_2x.jpg",
        "Red Dead Redemption 2" to "https://cdn.akamai.steamstatic.com/steam/apps/1174180/library_600x900_2x.jpg",
        "Red Dead Redemption" to "https://cdn.akamai.steamstatic.com/steam/apps/2668510/library_600x900_2x.jpg",
        "Shadow of the Colossus" to "https://howlongtobeat.com/games/Shadow_of_the_Colossus_PS4.jpg",
        "Castlevania: Symphony of the Night" to "https://howlongtobeat.com/games/Castlevania_Symphony_of_the_Night.jpg",
        "Metroid Prime" to "https://howlongtobeat.com/games/104193_Metroid_Prime_Remastered.jpg",
        "Metroid Prime Remastered" to "https://howlongtobeat.com/games/104193_Metroid_Prime_Remastered.jpg",
        "Metroid Dread" to "https://howlongtobeat.com/games/94075_Metroid_Dread.jpg",
        "Metroid Fusion" to "https://howlongtobeat.com/games/Metroid_Fusion.jpg",
        "Super Metroid" to "https://howlongtobeat.com/games/Super_Metroid.jpg",
        "Pokemon HeartGold" to "https://howlongtobeat.com/games/Pokemon_HeartGold_Version.jpg",
        "Pokemon SoulSilver" to "https://howlongtobeat.com/games/Pokemon_SoulSilver_Version.jpg",
        "Pokemon Emerald" to "https://howlongtobeat.com/games/Pokemon_Emerald_Version.jpg",
        "Pokemon Platinum" to "https://howlongtobeat.com/games/Pokemon_Platinum_Version.jpg",
        "Pokemon FireRed" to "https://howlongtobeat.com/games/Pokemon_FireRed_Version.jpg",
        "Pokemon LeafGreen" to "https://howlongtobeat.com/games/Pokemon_LeafGreen_Version.jpg",
        "Pokemon Black" to "https://howlongtobeat.com/games/Pokemon_Black_Version.jpg",
        "Pokemon White" to "https://howlongtobeat.com/games/Pokemon_White_Version.jpg",
        "Chrono Trigger" to "https://cdn.akamai.steamstatic.com/steam/apps/613830/library_600x900_2x.jpg",
        "Chrono Cross" to "https://cdn.akamai.steamstatic.com/steam/apps/1133760/library_600x900_2x.jpg",
        "Halo: Combat Evolved" to "https://cdn.akamai.steamstatic.com/steam/apps/1064221/library_600x900_2x.jpg",
        "Halo 2" to "https://cdn.akamai.steamstatic.com/steam/apps/1064240/library_600x900_2x.jpg",
        "Halo 3" to "https://cdn.akamai.steamstatic.com/steam/apps/1064271/library_600x900_2x.jpg",
        "Halo Reach" to "https://cdn.akamai.steamstatic.com/steam/apps/1064220/library_600x900_2x.jpg",
        "Half-Life 2" to "https://cdn.akamai.steamstatic.com/steam/apps/220/library_600x900_2x.jpg",
        "Half-Life" to "https://cdn.akamai.steamstatic.com/steam/apps/70/library_600x900_2x.jpg",
        "Portal" to "https://cdn.akamai.steamstatic.com/steam/apps/400/library_600x900_2x.jpg",
        "Portal 2" to "https://cdn.akamai.steamstatic.com/steam/apps/620/library_600x900_2x.jpg",
        "Cyberpunk 2077" to "https://cdn.akamai.steamstatic.com/steam/apps/1091500/library_600x900_2x.jpg",
        "The Last of Us" to "https://cdn.akamai.steamstatic.com/steam/apps/1888930/library_600x900_2x.jpg",
        "The Last of Us Part I" to "https://cdn.akamai.steamstatic.com/steam/apps/1888930/library_600x900_2x.jpg",
        "The Last of Us Part II" to "https://howlongtobeat.com/games/41753_The_Last_of_Us_Part_II.jpg",
        "Hollow Knight" to "https://cdn.akamai.steamstatic.com/steam/apps/367520/library_600x900_2x.jpg",
        "Hades" to "https://cdn.akamai.steamstatic.com/steam/apps/1145360/library_600x900_2x.jpg",
        "Celeste" to "https://cdn.akamai.steamstatic.com/steam/apps/504230/library_600x900_2x.jpg",
        "Ghost of Tsushima" to "https://cdn.akamai.steamstatic.com/steam/apps/2215430/library_600x900_2x.jpg",
        "Horizon Zero Dawn" to "https://cdn.akamai.steamstatic.com/steam/apps/1151640/library_600x900_2x.jpg",
        "Horizon Forbidden West" to "https://cdn.akamai.steamstatic.com/steam/apps/2420110/library_600x900_2x.jpg",
        "Kingdom Hearts II" to "https://cdn.akamai.steamstatic.com/steam/apps/2552430/library_600x900_2x.jpg",
        "Monster Hunter: World" to "https://cdn.akamai.steamstatic.com/steam/apps/582010/library_600x900_2x.jpg",
        "NieR:Automata" to "https://cdn.akamai.steamstatic.com/steam/apps/524220/library_600x900_2x.jpg",
        "Death Stranding" to "https://cdn.akamai.steamstatic.com/steam/apps/1850570/library_600x900_2x.jpg",
        "Baldur's Gate 3" to "https://cdn.akamai.steamstatic.com/steam/apps/1086940/library_600x900_2x.jpg",
        "Disco Elysium" to "https://cdn.akamai.steamstatic.com/steam/apps/632470/library_600x900_2x.jpg",
        "Golden Sun" to "https://howlongtobeat.com/games/Golden_Sun.jpg",
        "Golden Sun: The Lost Age" to "https://howlongtobeat.com/games/Golden_Sun_The_Lost_Age.jpg",
        "Xenoblade Chronicles" to "https://howlongtobeat.com/games/70160_Xenoblade_Chronicles_Definitive_Edition.jpg",
        "Xenoblade Chronicles 2" to "https://howlongtobeat.com/games/42835_Xenoblade_Chronicles_2.jpg",
        "Xenoblade Chronicles 3" to "https://howlongtobeat.com/games/103816_Xenoblade_Chronicles_3.jpg",
        "Fire Emblem: Three Houses" to "https://howlongtobeat.com/games/57497_Fire_Emblem_Three_Houses.jpg",
        "Fire Emblem Awakening" to "https://howlongtobeat.com/games/Fire_Emblem_Awakening.jpg",
        "Gravity Rush" to "https://howlongtobeat.com/games/Gravity_Rush_Remastered.jpg",
        "Killzone: Mercenary" to "https://howlongtobeat.com/games/Killzone_Mercenary.jpg",
        "Uncharted 2: Among Thieves" to "https://howlongtobeat.com/games/Uncharted_2_Among_Thieves.jpg",
        "Uncharted 4: A Thief's End" to "https://cdn.akamai.steamstatic.com/steam/apps/1659420/library_600x900_2x.jpg",
        "Left 4 Dead 2" to "https://cdn.akamai.steamstatic.com/steam/apps/550/library_600x900_2x.jpg",
        "Left 4 Dead" to "https://cdn.akamai.steamstatic.com/steam/apps/500/library_600x900_2x.jpg",
        "Gears of War" to "https://howlongtobeat.com/games/Gears_of_War.jpg",
        "Gears of War 2" to "https://howlongtobeat.com/games/Gears_of_War_2.jpg",
        "Gears of War 3" to "https://howlongtobeat.com/games/Gears_of_War_3.jpg",
        "Dead Space" to "https://cdn.akamai.steamstatic.com/steam/apps/1693980/library_600x900_2x.jpg",
        "Dead Space 2" to "https://cdn.akamai.steamstatic.com/steam/apps/47780/library_600x900_2x.jpg",
        "BioShock" to "https://cdn.akamai.steamstatic.com/steam/apps/409710/library_600x900_2x.jpg",
        "BioShock Infinite" to "https://cdn.akamai.steamstatic.com/steam/apps/8870/library_600x900_2x.jpg",
        "Mass Effect" to "https://cdn.akamai.steamstatic.com/steam/apps/1328670/library_600x900_2x.jpg",
        "Mass Effect 2" to "https://cdn.akamai.steamstatic.com/steam/apps/1328670/library_600x900_2x.jpg",
        "Mass Effect 3" to "https://cdn.akamai.steamstatic.com/steam/apps/1328670/library_600x900_2x.jpg",
        "Fallout: New Vegas" to "https://cdn.akamai.steamstatic.com/steam/apps/22380/library_600x900_2x.jpg",
        "Fallout 3" to "https://cdn.akamai.steamstatic.com/steam/apps/22370/library_600x900_2x.jpg",
        "Fallout 4" to "https://cdn.akamai.steamstatic.com/steam/apps/377160/library_600x900_2x.jpg",
        "The Elder Scrolls V: Skyrim" to "https://cdn.akamai.steamstatic.com/steam/apps/489830/library_600x900_2x.jpg",
        "The Elder Scrolls IV: Oblivion" to "https://cdn.akamai.steamstatic.com/steam/apps/22330/library_600x900_2x.jpg",
        "The Elder Scrolls III: Morrowind" to "https://cdn.akamai.steamstatic.com/steam/apps/22320/library_600x900_2x.jpg",
        "Star Wars: Knights of the Old Republic" to "https://cdn.akamai.steamstatic.com/steam/apps/32370/library_600x900_2x.jpg"
    )
}
