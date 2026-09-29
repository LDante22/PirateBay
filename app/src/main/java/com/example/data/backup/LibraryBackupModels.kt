package com.example.data.backup

import androidx.compose.runtime.Immutable
import com.example.data.model.Game

/**
 * Format used for library exports
 */
@Immutable
enum class ExportFormat(val extension: String, val mimeType: String, val displayName: String) {
    JSON("json", "application/json", "JSON Backup (.json)"),
    CSV("csv", "text/csv", "CSV Spreadsheet (.csv)")
}

/**
 * Strategy for handling duplicate / existing records during backup restore
 */
@Immutable
enum class ConflictResolutionStrategy(
    val id: String,
    val title: String,
    val description: String,
    val iconEmoji: String
) {
    KEEP_EXISTING(
        id = "KEEP_EXISTING",
        title = "Keep Existing (Skip)",
        description = "Only import brand new games. Any existing games in your library remain untouched.",
        iconEmoji = "🛡️"
    ),
    MERGE(
        id = "MERGE",
        title = "Smart Merge (Recommended)",
        description = "Preserve your ratings, personal notes, and status, while filling missing metadata, HLTB playtimes, and covers from the backup.",
        iconEmoji = "🔄"
    ),
    REPLACE_EXISTING(
        id = "REPLACE_EXISTING",
        title = "Replace Existing (Overwrite)",
        description = "Overwrite matching existing games with the backup version entirely.",
        iconEmoji = "⚡"
    )
}

/**
 * Represents a matched record between the backup file and local library
 */
@Immutable
data class ExistingGameMatch(
    val backupGame: Game,
    val localGame: Game,
    val matchReason: String
)

/**
 * Summary result produced after analyzing a JSON backup file
 */
@Immutable
data class ImportAnalysisResult(
    val isValid: Boolean = false,
    val errorMessage: String? = null,
    val formatName: String = "the-tavern-game-library-backup",
    val formatVersion: Int = 1,
    val exportedAt: String = "",
    val appName: String = "",
    val totalRecordsInBackup: Int = 0,
    val newGames: List<Game> = emptyList(),
    val existingMatches: List<ExistingGameMatch> = emptyList(),
    val invalidRecordsCount: Int = 0,
    val invalidRecordsErrors: List<String> = emptyList(),
    val rawBackupGames: List<Game> = emptyList()
) {
    val totalValidGames: Int
        get() = newGames.size + existingMatches.size

    val hasConflicts: Boolean
        get() = existingMatches.isNotEmpty()
}

/**
 * Sealed class for tracking ongoing backup & restore operations in UI
 */
@Immutable
sealed class BackupOperationState {
    object Idle : BackupOperationState()

    @Immutable
    data class Exporting(
        val format: ExportFormat,
        val current: Int,
        val total: Int,
        val message: String
    ) : BackupOperationState()

    @Immutable
    data class ExportReady(
        val format: ExportFormat,
        val fileContent: String,
        val defaultFileName: String,
        val gamesCount: Int
    ) : BackupOperationState()

    @Immutable
    data class ImportAnalyzing(
        val current: Int,
        val total: Int,
        val message: String
    ) : BackupOperationState()

    @Immutable
    data class ImportPreview(
        val analysis: ImportAnalysisResult
    ) : BackupOperationState()

    @Immutable
    data class Importing(
        val current: Int,
        val total: Int,
        val message: String
    ) : BackupOperationState()

    @Immutable
    data class Completed(
        val message: String,
        val count: Int,
        val details: String = ""
    ) : BackupOperationState()

    @Immutable
    data class Error(
        val message: String
    ) : BackupOperationState()
}
