package com.example.data.hltb

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Robust direct client for HowLongToBeat searches and playtime data retrieval.
 * Employs automatic session token initialization, security honeypot header handling,
 * intelligent caching, and graceful fallback retries.
 */
class HltbClient(
    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(12, TimeUnit.SECONDS)
        .build()
) {

    companion object {
        private const val TAG = "HltbClient"
        private const val HLTB_BASE_URL = "https://howlongtobeat.com"
        private const val HLTB_INIT_URL = "https://howlongtobeat.com/api/search/site/init"
        private const val HLTB_SEARCH_URL = "https://howlongtobeat.com/api/search/site"
        private const val HLTB_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36 GameVault/1.0"
        
        // Session token validity (10 minutes)
        private const val TOKEN_MAX_AGE_MS = 10L * 60 * 1000L
    }

    private val sessionMutex = Mutex()
    private var cachedToken: String? = null
    private var cachedHpKey: String? = null
    private var cachedHpVal: String? = null
    private var tokenTimestamp: Long = 0L

    /**
     * Retrieves or refreshes the active session token required for HLTB search endpoints.
     */
    private suspend fun getSessionAuth(forceRefresh: Boolean = false): Triple<String, String, String>? = sessionMutex.withLock {
        val now = System.currentTimeMillis()
        if (!forceRefresh && cachedToken != null && (now - tokenTimestamp < TOKEN_MAX_AGE_MS)) {
            return@withLock Triple(cachedToken!!, cachedHpKey.orEmpty(), cachedHpVal.orEmpty())
        }

        try {
            val initReq = Request.Builder()
                .url("$HLTB_INIT_URL?t=$now")
                .addHeader("User-Agent", HLTB_USER_AGENT)
                .addHeader("Referer", "$HLTB_BASE_URL/")
                .addHeader("Origin", HLTB_BASE_URL)
                .addHeader("Accept", "application/json, text/plain, */*")
                .build()

            val response = okHttpClient.newCall(initReq).execute()
            val body = response.body?.string()

            if (response.isSuccessful && !body.isNullOrBlank()) {
                val json = JSONObject(body)
                val token = json.optString("token", "")
                val hpKey = json.optString("hpKey", "")
                val hpVal = json.optString("hpVal", "")

                if (token.isNotBlank()) {
                    cachedToken = token
                    cachedHpKey = hpKey
                    cachedHpVal = hpVal
                    tokenTimestamp = now
                    Log.d(TAG, "HLTB session token initialized successfully (hpKey: $hpKey)")
                    return@withLock Triple(token, hpKey, hpVal)
                }
            } else {
                Log.w(TAG, "Failed to init HLTB session token, code: ${response.code}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception while initializing HLTB session auth", e)
        }
        return@withLock null
    }

    /**
     * Searches HowLongToBeat for games matching the given query title.
     */
    suspend fun searchGames(query: String): Result<List<HltbGameEntry>> = withContext(Dispatchers.IO) {
        val cleanQuery = query.trim()
        if (cleanQuery.isBlank()) {
            return@withContext Result.success(emptyList())
        }

        try {
            // First attempt with current session token
            val result = executeSearch(cleanQuery, forceTokenRefresh = false)
            if (result.isSuccess) {
                return@withContext result
            }

            // If failed due to expired token or authentication failure, refresh token and retry once
            Log.d(TAG, "HLTB search attempt failed. Refreshing session auth and retrying...")
            return@withContext executeSearch(cleanQuery, forceTokenRefresh = true)
        } catch (e: Exception) {
            Log.e(TAG, "Error performing HLTB search for '$cleanQuery'", e)
            Result.failure(e)
        }
    }

    private fun executeSearch(query: String, forceTokenRefresh: Boolean): Result<List<HltbGameEntry>> {
        val auth = kotlinx.coroutines.runBlocking { getSessionAuth(forceTokenRefresh) }

        // Build search terms from query (strip special punctuation for term splitting)
        val terms = query
            .replace(Regex("[:\\-_/•|]"), " ")
            .split(" ")
            .map { it.trim().replace(Regex("[^a-zA-Z0-9]"), "") }
            .filter { it.isNotBlank() }

        val searchTermsArray = if (terms.isNotEmpty()) terms else listOf(query)

        val payload = JSONObject().apply {
            put("searchType", "games")
            put("searchTerms", JSONArray(searchTermsArray))
            put("searchPage", 1)
            put("size", 20)
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
                        put("difficulty", "")
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
            put("useCache", true)

            // Inject security honeypot token pair into request body
            if (auth != null && auth.second.isNotBlank()) {
                put(auth.second, auth.third)
            }
        }

        val requestBody = payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())

        val requestBuilder = Request.Builder()
            .url(HLTB_SEARCH_URL)
            .post(requestBody)
            .addHeader("User-Agent", HLTB_USER_AGENT)
            .addHeader("Referer", "$HLTB_BASE_URL/")
            .addHeader("Origin", HLTB_BASE_URL)
            .addHeader("Accept", "application/json, text/plain, */*")
            .addHeader("Content-Type", "application/json")

        if (auth != null) {
            if (auth.first.isNotBlank()) requestBuilder.addHeader("x-auth-token", auth.first)
            if (auth.second.isNotBlank()) requestBuilder.addHeader("x-hp-key", auth.second)
            if (auth.third.isNotBlank()) requestBuilder.addHeader("x-hp-val", auth.third)
        }

        val response = okHttpClient.newCall(requestBuilder.build()).execute()
        val responseBody = response.body?.string()

        if (response.code == 401 || response.code == 403) {
            return Result.failure(Exception("HTTP ${response.code}: HLTB session auth token expired"))
        }

        if (!response.isSuccessful || responseBody.isNullOrBlank()) {
            Log.w(TAG, "HLTB Search responded with code: ${response.code}")
            return Result.failure(Exception("HTTP ${response.code}: Failed to retrieve data from HowLongToBeat"))
        }

        val jsonObject = JSONObject(responseBody)
        val dataArray = jsonObject.optJSONArray("data") ?: JSONArray()
        val results = mutableListOf<HltbGameEntry>()

        for (i in 0 until dataArray.length()) {
            val item = dataArray.optJSONObject(i) ?: continue
            val gameId = item.optLong("game_id", 0L)
            val gameName = item.optString("game_name", "")
            if (gameId <= 0L || gameName.isBlank()) continue

            // Times are stored in seconds on HLTB; convert to hours
            val compMainSeconds = item.optLong("comp_main", 0L)
            val compPlusSeconds = item.optLong("comp_plus", 0L)
            val comp100Seconds = item.optLong("comp_100", 0L)

            val mainHours = if (compMainSeconds > 0) compMainSeconds / 3600f else 0f
            val plusHours = if (compPlusSeconds > 0) compPlusSeconds / 3600f else 0f
            val comp100Hours = if (comp100Seconds > 0) comp100Seconds / 3600f else 0f

            val platforms = item.optString("profile_platform", "")
            val releaseYear = item.optInt("release_world", 0)
            val gameImage = item.optString("game_image", "")

            results.add(
                HltbGameEntry(
                    gameId = gameId,
                    gameName = gameName,
                    mainStoryHours = mainHours,
                    mainExtraHours = plusHours,
                    completionistHours = comp100Hours,
                    platforms = platforms,
                    releaseYear = releaseYear,
                    imageUrl = if (gameImage.isNotBlank()) "https://howlongtobeat.com/games/$gameImage" else ""
                )
            )
        }

        return Result.success(results)
    }
}

