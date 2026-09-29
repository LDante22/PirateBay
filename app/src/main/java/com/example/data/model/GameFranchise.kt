package com.example.data.model

import androidx.compose.runtime.Immutable

/**
 * Represents a collection of games belonging to the same franchise across all platforms.
 */
@Immutable
data class GameFranchise(
    val name: String,
    val games: List<Game>,
    val bannerUrl: String = "",
    val coverUrl: String = "",
    val description: String = ""
) {
    val gameCount: Int get() = games.size

    val uniqueGameCount: Int get() = com.example.data.franchise.FranchiseDetector.countUniqueGames(games)

    val platforms: List<GameConsole> get() = games.map { it.console }.distinct().sortedBy { it.displayName }

    val platformCount: Int get() = platforms.size

    val yearRange: String get() {
        val years = games.map { it.releaseYear }.filter { it > 0 }.sorted()
        return when {
            years.isEmpty() -> ""
            years.size == 1 -> "${years.first()}"
            years.first() == years.last() -> "${years.first()}"
            else -> "${years.first()} – ${years.last()}"
        }
    }

    val earliestYear: Int get() = games.map { it.releaseYear }.filter { it > 0 }.minOrNull() ?: 0

    val latestYear: Int get() = games.map { it.releaseYear }.filter { it > 0 }.maxOrNull() ?: 0

    val representativeBannerUrl: String get() {
        if (bannerUrl.isNotBlank()) return bannerUrl
        val bannerFromGames = games.firstOrNull { it.bannerArtUrl.isNotBlank() }?.bannerArtUrl
        if (!bannerFromGames.isNullOrBlank()) return bannerFromGames
        val coverFromGames = games.firstOrNull { it.coverArtUrl.isNotBlank() }?.coverArtUrl
        return coverFromGames.orEmpty()
    }

    val representativeCoverUrl: String get() {
        if (coverUrl.isNotBlank()) return coverUrl
        val coverFromGames = games.firstOrNull { it.coverArtUrl.isNotBlank() }?.coverArtUrl
        return coverFromGames.orEmpty()
    }

    val averageRating: Float get() {
        val rated = games.filter { it.userRating > 0f }
        return if (rated.isEmpty()) 0f else rated.map { it.userRating }.average().toFloat()
    }
}
