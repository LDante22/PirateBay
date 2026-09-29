package com.example.data.backup

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import com.example.data.model.CoverSourceType
import com.example.data.model.Game
import com.example.data.model.GameConsole
import com.example.data.model.WishlistStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object LibraryBackupService {

    private const val TAG = "LibraryBackupService"
    const val CURRENT_BACKUP_VERSION = 1
    const val DEFAULT_FORMAT_IDENTIFIER = "the-tavern-game-library-backup"

    /**
     * Generates a complete human-readable JSON backup string of the user's library.
     * Guaranteed to NOT include passwords, auth tokens, or private secrets.
     */
    suspend fun generateJsonExport(
        games: List<Game>,
        appName: String = "The Tavern Game Vault",
        appVersion: String = "1.0",
        onProgress: ((current: Int, total: Int) -> Unit)? = null
    ): String = withContext(Dispatchers.Default) {
        val total = games.size
        val root = JSONObject()
        val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }

        root.put("format", DEFAULT_FORMAT_IDENTIFIER)
        root.put("version", CURRENT_BACKUP_VERSION)
        root.put("exportedAt", isoFormat.format(Date()))
        root.put("appName", appName)
        root.put("appVersion", appVersion)
        root.put("totalGames", total)

        val gamesArray = JSONArray()
        games.forEachIndexed { index, game ->
            val gameObj = gameToJsonObject(game)
            gamesArray.put(gameObj)
            if (index % 25 == 0 || index == total - 1) {
                onProgress?.invoke(index + 1, total)
            }
        }
        root.put("games", gamesArray)

        root.toString(2)
    }

    /**
     * Generates a spreadsheet-friendly RFC 4180 CSV export with UTF-8 BOM.
     */
    suspend fun generateCsvExport(
        games: List<Game>,
        onProgress: ((current: Int, total: Int) -> Unit)? = null
    ): String = withContext(Dispatchers.Default) {
        val total = games.size
        val sb = StringBuilder()

        // Prepend UTF-8 Byte Order Mark (BOM) for Excel/Numbers/Google Sheets
        sb.append('\uFEFF')

        // CSV Header
        val headers = listOf(
            "Title",
            "Platform / Console",
            "Console ID",
            "Status",
            "User Rating",
            "HLTB Main Story (Hours)",
            "HLTB Main + Extras (Hours)",
            "HLTB Completionist (Hours)",
            "Favorite",
            "Genres",
            "Developer",
            "Publisher",
            "Release Year",
            "Release Date",
            "Metacritic Score",
            "External Game ID",
            "External Provider",
            "HLTB ID",
            "Emulator",
            "Recommended Backend",
            "Target Price",
            "Date Added",
            "Personal Notes",
            "Description"
        )
        sb.append(headers.joinToString(",") { escapeCsv(it) }).append("\r\n")

        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)

        games.forEachIndexed { index, game ->
            val dateAddedStr = if (game.addedTimestamp > 0) dateFormat.format(Date(game.addedTimestamp)) else ""
            val row = listOf(
                game.title,
                game.console.displayName,
                game.consoleId,
                game.status.label,
                String.format(Locale.US, "%.1f", game.userRating),
                if (game.hltbMainStoryHours > 0f) String.format(Locale.US, "%.1f", game.hltbMainStoryHours) else "",
                if (game.hltbMainExtraHours > 0f) String.format(Locale.US, "%.1f", game.hltbMainExtraHours) else "",
                if (game.hltbCompletionistHours > 0f) String.format(Locale.US, "%.1f", game.hltbCompletionistHours) else "",
                if (game.isFavorite) "Yes" else "No",
                game.genre,
                game.developer,
                game.publisher,
                if (game.releaseYear > 0) game.releaseYear.toString() else "",
                game.releaseDate,
                if (game.metacriticScore > 0) game.metacriticScore.toString() else "",
                game.externalGameId,
                game.externalProvider,
                if (game.hltbId > 0) game.hltbId.toString() else "",
                game.emulator,
                game.recommendedBackend,
                game.targetPrice,
                dateAddedStr,
                game.notes,
                game.description
            )

            sb.append(row.joinToString(",") { escapeCsv(it) }).append("\r\n")

            if (index % 25 == 0 || index == total - 1) {
                onProgress?.invoke(index + 1, total)
            }
        }

        sb.toString()
    }

    /**
     * Parses and analyzes a JSON backup file against the existing local database.
     * Detects new games, existing conflicts/duplicates, and malformed records.
     */
    suspend fun analyzeBackupJson(
        jsonString: String,
        currentLocalGames: List<Game>,
        onProgress: ((current: Int, total: Int) -> Unit)? = null
    ): ImportAnalysisResult = withContext(Dispatchers.Default) {
        val trimmed = jsonString.trim()
        if (trimmed.isBlank()) {
            return@withContext ImportAnalysisResult(
                isValid = false,
                errorMessage = "The backup file is empty."
            )
        }

        try {
            var formatName = DEFAULT_FORMAT_IDENTIFIER
            var formatVersion = CURRENT_BACKUP_VERSION
            var exportedAt = ""
            var appName = ""
            val gamesJsonArray: JSONArray

            if (trimmed.startsWith("{")) {
                val root = JSONObject(trimmed)
                formatName = root.optString("format", DEFAULT_FORMAT_IDENTIFIER)
                formatVersion = root.optInt("version", CURRENT_BACKUP_VERSION)
                exportedAt = root.optString("exportedAt", "")
                appName = root.optString("appName", "")

                gamesJsonArray = when {
                    root.has("games") -> root.getJSONArray("games")
                    root.has("library") -> root.getJSONArray("library")
                    root.has("items") -> root.getJSONArray("items")
                    else -> JSONArray()
                }
            } else if (trimmed.startsWith("[")) {
                // Direct array of games
                gamesJsonArray = JSONArray(trimmed)
            } else {
                return@withContext ImportAnalysisResult(
                    isValid = false,
                    errorMessage = "Unrecognized backup format. Please select a valid JSON backup file."
                )
            }

            val totalRecords = gamesJsonArray.length()
            if (totalRecords == 0) {
                return@withContext ImportAnalysisResult(
                    isValid = false,
                    errorMessage = "No game records were found in this backup file."
                )
            }

            val parsedGames = mutableListOf<Game>()
            val invalidErrors = mutableListOf<String>()

            for (i in 0 until totalRecords) {
                val item = gamesJsonArray.optJSONObject(i)
                if (item == null) {
                    invalidErrors.add("Record #${i + 1} is not a valid JSON object.")
                    continue
                }

                val title = item.optString("title", "").trim()
                if (title.isBlank()) {
                    invalidErrors.add("Record #${i + 1} is missing a title.")
                    continue
                }

                try {
                    val game = jsonObjectToGame(item)
                    parsedGames.add(game)
                } catch (e: Exception) {
                    invalidErrors.add("Record #${i + 1} ('$title') could not be parsed: ${e.message}")
                }
            }

            // Deduplicate inside the backup file itself first
            val uniqueBackupGames = mutableListOf<Game>()
            for (g in parsedGames) {
                val existingInBackup = uniqueBackupGames.firstOrNull { areGamesMatching(it, g) }
                if (existingInBackup == null) {
                    uniqueBackupGames.add(g)
                }
            }

            val newGamesList = mutableListOf<Game>()
            val existingMatchesList = mutableListOf<ExistingGameMatch>()
            val totalUnique = uniqueBackupGames.size

            uniqueBackupGames.forEachIndexed { index, backupGame ->
                val localMatch = currentLocalGames.firstOrNull { areGamesMatching(it, backupGame) }
                if (localMatch != null) {
                    val reason = when {
                        backupGame.id > 0L && localMatch.id == backupGame.id -> "Exact Library ID Match (#${localMatch.id})"
                        backupGame.externalGameId.isNotBlank() && backupGame.externalGameId == localMatch.externalGameId -> "External Database ID Match (${backupGame.externalProvider} #${backupGame.externalGameId})"
                        else -> "Same Title & Platform (${backupGame.title} on ${backupGame.console.displayName})"
                    }
                    existingMatchesList.add(
                        ExistingGameMatch(
                            backupGame = backupGame,
                            localGame = localMatch,
                            matchReason = reason
                        )
                    )
                } else {
                    newGamesList.add(backupGame)
                }

                if (index % 20 == 0 || index == totalUnique - 1) {
                    onProgress?.invoke(index + 1, totalUnique)
                }
            }

            ImportAnalysisResult(
                isValid = true,
                errorMessage = null,
                formatName = formatName,
                formatVersion = formatVersion,
                exportedAt = exportedAt,
                appName = appName,
                totalRecordsInBackup = totalRecords,
                newGames = newGamesList,
                existingMatches = existingMatchesList,
                invalidRecordsCount = invalidErrors.size,
                invalidRecordsErrors = invalidErrors,
                rawBackupGames = uniqueBackupGames
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to analyze backup JSON: ${e.message}", e)
            ImportAnalysisResult(
                isValid = false,
                errorMessage = "Invalid JSON structure: ${e.localizedMessage ?: "Parsing error"}"
            )
        }
    }

    /**
     * Checks if two game objects refer to the same game entity
     */
    fun areGamesMatching(a: Game, b: Game): Boolean {
        // 1. Stable positive internal ID match
        if (a.id > 0L && b.id > 0L && a.id == b.id) {
            return true
        }

        // 2. External provider ID match (e.g. RAWG ID or IGDB ID)
        if (a.externalGameId.isNotBlank() && b.externalGameId.isNotBlank() &&
            a.externalGameId.equals(b.externalGameId, ignoreCase = true) &&
            (a.externalProvider.equals(b.externalProvider, ignoreCase = true) || a.consoleId.equals(b.consoleId, ignoreCase = true))
        ) {
            return true
        }

        // 3. Normalized Title + Console ID match
        val titleA = normalizeTitle(a.title)
        val titleB = normalizeTitle(b.title)
        val consoleA = a.consoleId.trim().uppercase()
        val consoleB = b.consoleId.trim().uppercase()

        return titleA.isNotEmpty() && titleA == titleB && consoleA == consoleB
    }

    private fun normalizeTitle(title: String): String {
        return title.trim().lowercase()
            .replace(Regex("[^a-z0-9]"), "")
    }

    /**
     * Smart merge between existing local game and imported backup game:
     * - Preserves existing user ratings, notes, status, and favorites
     * - Populates missing metadata, HLTB completion times, emulator configs, and covers from backup
     */
    fun resolveGameMerge(local: Game, backup: Game): Game {
        return local.copy(
            // Keep local personal user preferences if set, otherwise fill from backup
            userRating = if (local.userRating > 0f) local.userRating else backup.userRating,
            notes = if (local.notes.isNotBlank()) local.notes else backup.notes,
            wishlistStatus = if (local.wishlistStatus.isNotBlank()) local.wishlistStatus else backup.wishlistStatus,
            isFavorite = local.isFavorite || backup.isFavorite,
            targetPrice = if (local.targetPrice.isNotBlank()) local.targetPrice else backup.targetPrice,

            // Fill or enhance metadata from backup if local is default/empty
            genre = if (local.genre.isNotBlank() && local.genre != "Action") local.genre else backup.genre.ifBlank { local.genre },
            releaseYear = if (local.releaseYear > 0) local.releaseYear else backup.releaseYear,
            developer = local.developer.ifBlank { backup.developer },
            publisher = local.publisher.ifBlank { backup.publisher },
            releaseDate = local.releaseDate.ifBlank { backup.releaseDate },
            description = local.description.ifBlank { backup.description },
            websiteUrl = local.websiteUrl.ifBlank { backup.websiteUrl },
            metacriticScore = if (local.metacriticScore > 0) local.metacriticScore else backup.metacriticScore,
            supportedPlatforms = local.supportedPlatforms.ifBlank { backup.supportedPlatforms },

            // Artwork and media
            coverArtUrl = local.coverArtUrl.ifBlank { backup.coverArtUrl },
            backCoverUrl = local.backCoverUrl.ifBlank { backup.backCoverUrl },
            completeCaseArtwork = local.completeCaseArtwork.ifBlank { backup.completeCaseArtwork },
            bannerArtUrl = local.bannerArtUrl.ifBlank { backup.bannerArtUrl },
            youtubeVideoId = local.youtubeVideoId.ifBlank { backup.youtubeVideoId },
            coverGradientStart = if (local.coverGradientStart != 0xFF1E1B4BL) local.coverGradientStart else backup.coverGradientStart,
            coverGradientEnd = if (local.coverGradientEnd != 0xFF0F172AL) local.coverGradientEnd else backup.coverGradientEnd,
            coverAccentColor = if (local.coverAccentColor != 0xFF6366F1L) local.coverAccentColor else backup.coverAccentColor,

            // Emulator configs & pro-tips
            emulator = local.emulator.ifBlank { backup.emulator },
            emulatorNotes = local.emulatorNotes.ifBlank { backup.emulatorNotes },
            recommendedBackend = local.recommendedBackend.ifBlank { backup.recommendedBackend },
            internalResolution = local.internalResolution.ifBlank { backup.internalResolution },
            compatibilityRating = local.compatibilityRating.ifBlank { backup.compatibilityRating },
            biosRequirement = local.biosRequirement.ifBlank { backup.biosRequirement },
            targetFramerate = local.targetFramerate.ifBlank { backup.targetFramerate },
            controllerLayout = local.controllerLayout.ifBlank { backup.controllerLayout },
            proTips = local.proTips.ifBlank { backup.proTips },

            // HLTB Completion times
            hltbId = if (local.hltbId > 0L) local.hltbId else backup.hltbId,
            hltbName = local.hltbName.ifBlank { backup.hltbName },
            hltbMainStoryHours = if (local.hltbMainStoryHours > 0f) local.hltbMainStoryHours else backup.hltbMainStoryHours,
            hltbMainExtraHours = if (local.hltbMainExtraHours > 0f) local.hltbMainExtraHours else backup.hltbMainExtraHours,
            hltbCompletionistHours = if (local.hltbCompletionistHours > 0f) local.hltbCompletionistHours else backup.hltbCompletionistHours,
            hltbLastUpdated = if (local.hltbLastUpdated > 0L) local.hltbLastUpdated else backup.hltbLastUpdated,
            hltbSyncStatus = local.hltbSyncStatus.ifBlank { backup.hltbSyncStatus },

            // External Provider IDs
            externalProvider = local.externalProvider.ifBlank { backup.externalProvider },
            externalGameId = local.externalGameId.ifBlank { backup.externalGameId },
            externalLastUpdated = if (local.externalLastUpdated > 0L) local.externalLastUpdated else backup.externalLastUpdated,
            externalSyncStatus = local.externalSyncStatus.ifBlank { backup.externalSyncStatus }
        )
    }

    /**
     * Converts a single Game domain model into a structured JSONObject
     */
    private fun gameToJsonObject(game: Game): JSONObject {
        val obj = JSONObject()
        obj.put("id", game.id)
        obj.put("title", game.title)
        obj.put("consoleId", game.consoleId)
        obj.put("genre", game.genre)
        obj.put("releaseYear", game.releaseYear)
        obj.put("emulator", game.emulator)
        obj.put("youtubeVideoId", game.youtubeVideoId)
        obj.put("coverArtUrl", game.coverArtUrl)
        obj.put("backCoverUrl", game.backCoverUrl)
        obj.put("completeCaseArtwork", game.completeCaseArtwork)
        obj.put("coverSource", game.coverSource)
        obj.put("backCoverSource", game.backCoverSource)
        obj.put("coverGradientStart", game.coverGradientStart)
        obj.put("coverGradientEnd", game.coverGradientEnd)
        obj.put("coverAccentColor", game.coverAccentColor)
        obj.put("wishlistStatus", game.wishlistStatus)
        obj.put("userRating", game.userRating.toDouble())
        obj.put("targetPrice", game.targetPrice)
        obj.put("developer", game.developer)
        obj.put("notes", game.notes)
        obj.put("isFavorite", game.isFavorite)
        obj.put("addedTimestamp", game.addedTimestamp)

        // Offline Emulator Settings
        obj.put("emulatorNotes", game.emulatorNotes)
        obj.put("recommendedBackend", game.recommendedBackend)
        obj.put("internalResolution", game.internalResolution)
        obj.put("compatibilityRating", game.compatibilityRating)
        obj.put("biosRequirement", game.biosRequirement)
        obj.put("targetFramerate", game.targetFramerate)
        obj.put("controllerLayout", game.controllerLayout)
        obj.put("proTips", game.proTips)

        // HowLongToBeat Stats
        obj.put("hltbId", game.hltbId)
        obj.put("hltbName", game.hltbName)
        obj.put("hltbMainStoryHours", game.hltbMainStoryHours.toDouble())
        obj.put("hltbMainExtraHours", game.hltbMainExtraHours.toDouble())
        obj.put("hltbCompletionistHours", game.hltbCompletionistHours.toDouble())
        obj.put("hltbLastUpdated", game.hltbLastUpdated)
        obj.put("hltbSyncStatus", game.hltbSyncStatus)

        // External Game Metadata (RAWG / IGDB)
        obj.put("externalProvider", game.externalProvider)
        obj.put("externalGameId", game.externalGameId)
        obj.put("externalLastUpdated", game.externalLastUpdated)
        obj.put("externalSyncStatus", game.externalSyncStatus)
        obj.put("bannerArtUrl", game.bannerArtUrl)
        obj.put("publisher", game.publisher)
        obj.put("releaseDate", game.releaseDate)
        obj.put("description", game.description)
        obj.put("websiteUrl", game.websiteUrl)
        obj.put("metacriticScore", game.metacriticScore)
        obj.put("supportedPlatforms", game.supportedPlatforms)

        return obj
    }

    /**
     * Converts a JSONObject back into a Game domain model with safe defaults
     */
    private fun jsonObjectToGame(obj: JSONObject): Game {
        val title = obj.optString("title", "").trim()
        val rawConsole = obj.optString("consoleId", obj.optString("platform", GameConsole.PS2.id))
        val console = GameConsole.fromId(rawConsole)

        return Game(
            id = obj.optLong("id", 0L),
            title = title,
            consoleId = console.id,
            genre = obj.optString("genre", "Action"),
            releaseYear = obj.optInt("releaseYear", 2010),
            emulator = obj.optString("emulator", ""),
            youtubeVideoId = obj.optString("youtubeVideoId", ""),
            coverArtUrl = obj.optString("coverArtUrl", ""),
            backCoverUrl = obj.optString("backCoverUrl", ""),
            completeCaseArtwork = obj.optString("completeCaseArtwork", ""),
            coverSource = obj.optString("coverSource", CoverSourceType.EXISTING),
            backCoverSource = obj.optString("backCoverSource", ""),
            coverGradientStart = obj.optLong("coverGradientStart", 0xFF1E1B4BL),
            coverGradientEnd = obj.optLong("coverGradientEnd", 0xFF0F172AL),
            coverAccentColor = obj.optLong("coverAccentColor", 0xFF6366F1L),
            wishlistStatus = obj.optString("wishlistStatus", WishlistStatus.WANT_TO_PLAY.id),
            userRating = obj.optDouble("userRating", 4.5).toFloat(),
            targetPrice = obj.optString("targetPrice", "$29.99"),
            developer = obj.optString("developer", ""),
            notes = obj.optString("notes", ""),
            isFavorite = obj.optBoolean("isFavorite", false),
            addedTimestamp = obj.optLong("addedTimestamp", System.currentTimeMillis()),
            userEmail = "",

            emulatorNotes = obj.optString("emulatorNotes", ""),
            recommendedBackend = obj.optString("recommendedBackend", ""),
            internalResolution = obj.optString("internalResolution", ""),
            compatibilityRating = obj.optString("compatibilityRating", ""),
            biosRequirement = obj.optString("biosRequirement", ""),
            targetFramerate = obj.optString("targetFramerate", ""),
            controllerLayout = obj.optString("controllerLayout", ""),
            proTips = obj.optString("proTips", ""),

            hltbId = obj.optLong("hltbId", 0L),
            hltbName = obj.optString("hltbName", ""),
            hltbMainStoryHours = obj.optDouble("hltbMainStoryHours", 0.0).toFloat(),
            hltbMainExtraHours = obj.optDouble("hltbMainExtraHours", 0.0).toFloat(),
            hltbCompletionistHours = obj.optDouble("hltbCompletionistHours", 0.0).toFloat(),
            hltbLastUpdated = obj.optLong("hltbLastUpdated", 0L),
            hltbSyncStatus = obj.optString("hltbSyncStatus", ""),

            externalProvider = obj.optString("externalProvider", "RAWG"),
            externalGameId = obj.optString("externalGameId", ""),
            externalLastUpdated = obj.optLong("externalLastUpdated", 0L),
            externalSyncStatus = obj.optString("externalSyncStatus", ""),
            bannerArtUrl = obj.optString("bannerArtUrl", ""),
            publisher = obj.optString("publisher", ""),
            releaseDate = obj.optString("releaseDate", ""),
            description = obj.optString("description", ""),
            websiteUrl = obj.optString("websiteUrl", ""),
            metacriticScore = obj.optInt("metacriticScore", 0),
            supportedPlatforms = obj.optString("supportedPlatforms", "")
        )
    }

    /**
     * Escapes standard CSV fields according to RFC 4180
     */
    private fun escapeCsv(value: String): String {
        val containsSpecial = value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")
        return if (containsSpecial) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }
    }

    /**
     * Reads text content from a Storage Access Framework Uri
     */
    suspend fun readContentFromUri(context: Context, uri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { reader ->
                    reader.readText()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read file from URI: ${e.message}", e)
            null
        }
    }

    /**
     * Writes text content to a Storage Access Framework Uri
     */
    suspend fun writeContentToUri(context: Context, uri: Uri, content: String): Boolean = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openOutputStream(uri)?.use { stream ->
                OutputStreamWriter(stream, Charsets.UTF_8).use { writer ->
                    writer.write(content)
                    writer.flush()
                }
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write file to URI: ${e.message}", e)
            false
        }
    }

    /**
     * Saves exported content to a temporary cache file and creates an Android Share Intent
     */
    suspend fun createShareIntent(
        context: Context,
        format: ExportFormat,
        content: String,
        fileName: String
    ): Intent = withContext(Dispatchers.IO) {
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val exportFile = File(exportDir, fileName)
        exportFile.writeText(content, Charsets.UTF_8)

        val fileUri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            exportFile
        )

        Intent(Intent.ACTION_SEND).apply {
            type = format.mimeType
            putExtra(Intent.EXTRA_STREAM, fileUri)
            putExtra(Intent.EXTRA_SUBJECT, "The Tavern Game Vault Library Export ($fileName)")
            putExtra(Intent.EXTRA_TEXT, "Here is my game library backup exported from The Tavern.")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
