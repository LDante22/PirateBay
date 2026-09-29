package com.example.data.hltb

import java.util.Locale

/**
 * Data model for a game entry retrieved from HowLongToBeat.
 */
data class HltbGameEntry(
    val gameId: Long,
    val gameName: String,
    val mainStoryHours: Float = 0f,
    val mainExtraHours: Float = 0f,
    val completionistHours: Float = 0f,
    val platforms: String = "",
    val releaseYear: Int = 0,
    val imageUrl: String = "",
    val confidenceScore: Float = 1.0f
) {
    val hasValidPlaytime: Boolean
        get() = mainStoryHours > 0 || mainExtraHours > 0 || completionistHours > 0

    val formattedMainStory: String
        get() = formatHours(mainStoryHours)

    val formattedMainExtra: String
        get() = formatHours(mainExtraHours)

    val formattedCompletionist: String
        get() = formatHours(completionistHours)

    companion object {
        fun formatHours(hours: Float): String {
            return when {
                hours <= 0f -> "--"
                hours % 1f == 0f -> "${hours.toInt()}h"
                else -> "${String.format(Locale.US, "%.1f", hours)}h"
            }
        }
    }
}

/**
 * Sealed result for matching a game with HowLongToBeat.
 */
sealed class HltbMatchResult {
    data class Matched(
        val entry: HltbGameEntry,
        val confidenceScore: Float
    ) : HltbMatchResult()

    data object NotFound : HltbMatchResult()

    data class Unresolved(
        val candidates: List<HltbGameEntry>,
        val reason: String = "Multiple ambiguous matches found"
    ) : HltbMatchResult()

    data class Error(
        val message: String
    ) : HltbMatchResult()
}

/**
 * Loading state enum for UI presentation.
 */
enum class HltbLoadingStatus {
    IDLE,
    LOADING,
    SYNCED,
    NOT_FOUND,
    UNRESOLVED,
    ERROR
}
