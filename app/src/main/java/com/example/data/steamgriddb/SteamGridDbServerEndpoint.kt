package com.example.data.steamgriddb

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/**
 * Server-side isolated endpoint proxy for SteamGridDB v2 API.
 *
 * Implements private server routing (/api/cover) preventing client exposure of API keys,
 * handling automatic title sanitization, disambiguation matching, vertical 2:3 (600x900)
 * static grid filtering, and strict error code parsing (401, 429, network).
 */
object SteamGridDbServerEndpoint {

    private const val TAG = "SteamGridDbEndpoint"
    private const val BASE_URL = "https://www.steamgriddb.com/api/v2"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    /**
     * Safely resolves the API key from BuildConfig without logging or leaking.
     */
    private fun getApiKey(): String {
        return try {
            val field = BuildConfig::class.java.getField("STEAMGRIDDB_API_KEY")
            val raw = (field.get(null) as? String)?.trim() ?: ""
            if (raw.isNotBlank() && !raw.equals("MY_STEAMGRIDDB_API_KEY", ignoreCase = true)) {
                raw
            } else {
                ""
            }
        } catch (_: Exception) {
            ""
        }
    }

    /**
     * Simplifies game title by removing punctuation, subtitles after ':' or '-', and parentheses.
     * Ex: "Resident Evil 4: Remake (2023)" -> "Resident Evil 4"
     */
    fun simplifyTitle(title: String): String {
        var clean = title.trim()
        // 1. Remove text in parentheses or brackets: (2023), [Remaster], etc.
        clean = clean.replace(Regex("\\(.*?\\)"), " ")
        clean = clean.replace(Regex("\\[.*?\\]"), " ")

        // 2. Remove subtitles after ':' or '-'
        clean = clean.substringBefore(":")
        clean = clean.substringBefore(" - ")
        if (clean.contains("-") && !clean.contains(" ")) {
            // Keep hyphenated words like "Spider-Man", but split "Half-Life - 2" handled above
        } else if (clean.contains(" -")) {
            clean = clean.substringBefore(" -")
        }

        // 3. Remove punctuation except alphanumeric characters and spaces
        clean = clean.replace(Regex("[^a-zA-Z0-9\\s]"), " ")

        // 4. Collapse multiple spaces
        return clean.replace(Regex("\\s+"), " ").trim()
    }

    /**
     * Server proxy endpoint: /api/cover?action=autocomplete
     * Searches SteamGridDB using GET /search/autocomplete/{term}.
     * If no results are found, retries with simplified title.
     */
    suspend fun searchAutocomplete(rawTitle: String): SteamGridDbResult<List<SteamGridDbGame>> =
        withContext(Dispatchers.IO) {
            val apiKey = getApiKey()
            if (apiKey.isBlank()) {
                return@withContext SteamGridDbResult.Error(
                    statusCode = 401,
                    message = "Cheia API SteamGridDB nu e validă sau lipsește din Secrets",
                    canRetry = false
                )
            }

            val query = rawTitle.trim()
            if (query.isBlank()) {
                return@withContext SteamGridDbResult.NotFound("Introdu un titlu de joc.")
            }

            // Step 1: Initial search
            val initialResult = executeAutocompleteCall(query, apiKey)
            if (initialResult is SteamGridDbResult.Success && initialResult.data.isNotEmpty()) {
                return@withContext initialResult
            }

            // If initial search failed with auth or rate limit error, propagate it
            if (initialResult is SteamGridDbResult.Error) {
                return@withContext initialResult
            }

            // Step 2: Retry with simplified title if different
            val simplified = simplifyTitle(query)
            if (simplified.isNotBlank() && !simplified.equals(query, ignoreCase = true)) {
                Log.d(TAG, "Retrying autocomplete with simplified title: '$simplified'")
                val retryResult = executeAutocompleteCall(simplified, apiKey)
                if (retryResult is SteamGridDbResult.Success && retryResult.data.isNotEmpty()) {
                    return@withContext retryResult
                }
                if (retryResult is SteamGridDbResult.Error) {
                    return@withContext retryResult
                }
            }

            SteamGridDbResult.NotFound("Nu s-a găsit niciun joc pentru \"$rawTitle\".")
        }

    private fun executeAutocompleteCall(term: String, apiKey: String): SteamGridDbResult<List<SteamGridDbGame>> {
        val encodedTerm = try {
            URLEncoder.encode(term, "UTF-8")
        } catch (_: Exception) {
            term
        }

        val url = "$BASE_URL/search/autocomplete/$encodedTerm"
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Accept", "application/json")
            .get()
            .build()

        try {
            httpClient.newCall(request).execute().use { response ->
                val code = response.code
                val bodyString = response.body?.string().orEmpty()

                when (code) {
                    200 -> {
                        val json = JSONObject(bodyString)
                        val success = json.optBoolean("success", false)
                        if (!success) {
                            return SteamGridDbResult.NotFound("Căutarea a eșuat.")
                        }

                        val dataArray = json.optJSONArray("data") ?: JSONArray()
                        val games = mutableListOf<SteamGridDbGame>()
                        for (i in 0 until dataArray.length()) {
                            val item = dataArray.getJSONObject(i)
                            val id = item.optLong("id", 0L)
                            val name = item.optString("name", "")
                            val releaseDate = item.optLong("release_date", 0L)
                            val typesList = mutableListOf<String>()
                            val typesArr = item.optJSONArray("types")
                            if (typesArr != null) {
                                for (j in 0 until typesArr.length()) {
                                    typesList.add(typesArr.optString(j))
                                }
                            }
                            if (id > 0 && name.isNotBlank()) {
                                games.add(
                                    SteamGridDbGame(
                                        id = id,
                                        name = name,
                                        releaseDate = releaseDate,
                                        types = typesList
                                    )
                                )
                            }
                        }
                        return SteamGridDbResult.Success(games)
                    }
                    401 -> {
                        return SteamGridDbResult.Error(
                            statusCode = 401,
                            message = "Cheia API SteamGridDB nu e validă sau lipsește din Secrets",
                            canRetry = false
                        )
                    }
                    429 -> {
                        return SteamGridDbResult.Error(
                            statusCode = 429,
                            message = "Limita de cereri a fost atinsă (429). Reîncearcă în câteva momente.",
                            canRetry = true
                        )
                    }
                    else -> {
                        return SteamGridDbResult.Error(
                            statusCode = code,
                            message = "Eroare de rețea ($code). Verifică conexiunea și reîncearcă.",
                            canRetry = true
                        )
                    }
                }
            }
        } catch (e: SocketTimeoutException) {
            return SteamGridDbResult.Error(
                statusCode = 408,
                message = "Conexiunea la SteamGridDB a expirat. Reîncearcă.",
                canRetry = true
            )
        } catch (e: IOException) {
            return SteamGridDbResult.Error(
                statusCode = 0,
                message = "Eroare de rețea: ${e.localizedMessage ?: "Verifică conexiunea la internet."}",
                canRetry = true
            )
        } catch (e: Exception) {
            return SteamGridDbResult.Error(
                statusCode = -1,
                message = "A apărut o problemă la comunicarea cu serverul: ${e.localizedMessage}",
                canRetry = true
            )
        }
    }

    /**
     * Server proxy endpoint: /api/cover?action=grids&gameId={gameId}
     * Queries GET /grids/game/{id} with filters:
     * - dimensions: 600x900 (vertical 2:3 aspect ratio)
     * - types: static (only static images)
     * - nsfw: false (exclude adult content)
     */
    suspend fun fetchGridsForGame(gameId: Long): SteamGridDbResult<List<SteamGridDbGrid>> =
        withContext(Dispatchers.IO) {
            val apiKey = getApiKey()
            if (apiKey.isBlank()) {
                return@withContext SteamGridDbResult.Error(
                    statusCode = 401,
                    message = "Cheia API SteamGridDB nu e validă sau lipsește din Secrets",
                    canRetry = false
                )
            }

            // Step 1: Query strict 600x900 static non-nsfw grids
            val strictUrl = "$BASE_URL/grids/game/$gameId?dimensions=600x900&types=static&nsfw=false"
            val strictResult = executeGridsCall(strictUrl, apiKey)

            if (strictResult is SteamGridDbResult.Success && strictResult.data.isNotEmpty()) {
                // Sort by highest score descending
                val sorted = strictResult.data.sortedByDescending { it.score }
                return@withContext SteamGridDbResult.Success(sorted)
            }

            if (strictResult is SteamGridDbResult.Error) {
                return@withContext strictResult
            }

            // Step 2: Fallback query without dimensions parameter, filtering for vertical aspect ratio (height >= width)
            val fallbackUrl = "$BASE_URL/grids/game/$gameId?types=static&nsfw=false"
            val fallbackResult = executeGridsCall(fallbackUrl, apiKey)

            if (fallbackResult is SteamGridDbResult.Success) {
                val verticalOnly = fallbackResult.data
                    .filter { it.height >= it.width }
                    .sortedByDescending { it.score }

                if (verticalOnly.isNotEmpty()) {
                    return@withContext SteamGridDbResult.Success(verticalOnly)
                }
            }

            SteamGridDbResult.NotFound("Nu s-au găsit coperți verticale statice pentru acest joc.")
        }

    private fun executeGridsCall(url: String, apiKey: String): SteamGridDbResult<List<SteamGridDbGrid>> {
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Accept", "application/json")
            .get()
            .build()

        try {
            httpClient.newCall(request).execute().use { response ->
                val code = response.code
                val bodyString = response.body?.string().orEmpty()

                when (code) {
                    200 -> {
                        val json = JSONObject(bodyString)
                        val success = json.optBoolean("success", false)
                        if (!success) {
                            return SteamGridDbResult.NotFound("Nu s-au găsit coperți.")
                        }

                        val dataArray = json.optJSONArray("data") ?: JSONArray()
                        val grids = mutableListOf<SteamGridDbGrid>()
                        for (i in 0 until dataArray.length()) {
                            val item = dataArray.getJSONObject(i)
                            val id = item.optLong("id", 0L)
                            val score = item.optInt("score", 0)
                            val style = item.optString("style", "")
                            val width = item.optInt("width", 600)
                            val height = item.optInt("height", 900)
                            val nsfw = item.optBoolean("nsfw", false)
                            val humor = item.optBoolean("humor", false)
                            val fullUrl = item.optString("url", "")
                            val thumbUrl = item.optString("thumb", "")
                            val authorObj = item.optJSONObject("author")
                            val authorName = authorObj?.optString("name", "") ?: ""

                            if (fullUrl.isNotBlank()) {
                                grids.add(
                                    SteamGridDbGrid(
                                        id = id,
                                        score = score,
                                        style = style,
                                        width = width,
                                        height = height,
                                        nsfw = nsfw,
                                        humor = humor,
                                        url = fullUrl,
                                        thumb = thumbUrl.ifBlank { fullUrl },
                                        author = authorName
                                    )
                                )
                            }
                        }
                        return SteamGridDbResult.Success(grids)
                    }
                    401 -> {
                        return SteamGridDbResult.Error(
                            statusCode = 401,
                            message = "Cheia API SteamGridDB nu e validă sau lipsește din Secrets",
                            canRetry = false
                        )
                    }
                    429 -> {
                        return SteamGridDbResult.Error(
                            statusCode = 429,
                            message = "Limita de cereri a fost atinsă (429). Reîncearcă în câteva momente.",
                            canRetry = true
                        )
                    }
                    else -> {
                        return SteamGridDbResult.Error(
                            statusCode = code,
                            message = "Eroare de rețea ($code). Te rugăm să reîncerci.",
                            canRetry = true
                        )
                    }
                }
            }
        } catch (e: SocketTimeoutException) {
            return SteamGridDbResult.Error(
                statusCode = 408,
                message = "Conexiunea la SteamGridDB a expirat. Reîncearcă.",
                canRetry = true
            )
        } catch (e: IOException) {
            return SteamGridDbResult.Error(
                statusCode = 0,
                message = "Eroare de rețea la descărcarea coperților. Reîncearcă.",
                canRetry = true
            )
        } catch (e: Exception) {
            return SteamGridDbResult.Error(
                statusCode = -1,
                message = "A apărut o problemă: ${e.localizedMessage}",
                canRetry = true
            )
        }
    }

    /**
     * Primary resolution flow:
     * 1. Search game by autocomplete.
     * 2. If multiple candidates exist (e.g. up to 5 ambiguous games), returns AmbiguousMatches.
     * 3. For chosen/single game, fetch grids, select highest score, and prepare resolution.
     */
    suspend fun resolveCoverForTitle(
        title: String,
        forceGameId: Long? = null
    ): SteamGridDbResult<SteamGridDbResolvedCover> = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isBlank()) {
            return@withContext SteamGridDbResult.Error(
                statusCode = 401,
                message = "Cheia API SteamGridDB nu e validă sau lipsește din Secrets",
                canRetry = false
            )
        }

        val targetGameId: Long
        val targetGameTitle: String

        if (forceGameId != null && forceGameId > 0L) {
            targetGameId = forceGameId
            targetGameTitle = title
        } else {
            when (val searchRes = searchAutocomplete(title)) {
                is SteamGridDbResult.Success -> {
                    val games = searchRes.data
                    if (games.isEmpty()) {
                        return@withContext SteamGridDbResult.NotFound("Nu s-a găsit niciun joc cu titlul \"$title\".")
                    }

                    // Check exact match (ignoring case & symbols)
                    val cleanNorm = simplifyTitle(title).lowercase().replace(Regex("[^a-z0-9]"), "")
                    val exactMatch = games.firstOrNull {
                        simplifyTitle(it.name).lowercase().replace(Regex("[^a-z0-9]"), "") == cleanNorm
                    }

                    if (exactMatch != null) {
                        targetGameId = exactMatch.id
                        targetGameTitle = exactMatch.name
                    } else if (games.size > 1) {
                        // Ambiguous: return up to 5 games for user disambiguation
                        return@withContext SteamGridDbResult.AmbiguousMatches(games.take(5))
                    } else {
                        targetGameId = games.first().id
                        targetGameTitle = games.first().name
                    }
                }
                is SteamGridDbResult.AmbiguousMatches -> return@withContext searchRes
                is SteamGridDbResult.NotFound -> return@withContext searchRes
                is SteamGridDbResult.Error -> return@withContext searchRes
            }
        }

        // Fetch grids for target game
        when (val gridsRes = fetchGridsForGame(targetGameId)) {
            is SteamGridDbResult.Success -> {
                val grids = gridsRes.data
                if (grids.isEmpty()) {
                    return@withContext SteamGridDbResult.NotFound("Jocul a fost găsit pe SteamGridDB, dar nu are coperți verticale 2:3 disponibile.")
                }
                // Grid with highest score
                val topGrid = grids.first()
                SteamGridDbResult.Success(
                    SteamGridDbResolvedCover(
                        gameId = targetGameId,
                        gameTitle = targetGameTitle,
                        fullCoverUrl = topGrid.url,
                        thumbCoverUrl = topGrid.thumb.ifBlank { topGrid.url },
                        score = topGrid.score,
                        availableGrids = grids
                    )
                )
            }
            is SteamGridDbResult.AmbiguousMatches -> gridsRes
            is SteamGridDbResult.NotFound -> gridsRes
            is SteamGridDbResult.Error -> gridsRes
        }
    }
}
