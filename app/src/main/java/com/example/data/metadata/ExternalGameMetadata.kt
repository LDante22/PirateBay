package com.example.data.metadata

import com.example.data.model.GameConsole

/**
 * Detailed external game metadata retrieved from external database (RAWG/IGDB).
 */
data class ExternalGameMetadata(
    val externalProvider: String = "RAWG",
    val externalGameId: String = "",
    val title: String = "",
    val slug: String = "",
    val coverArtUrl: String = "",
    val bannerArtUrl: String = "",
    val description: String = "",
    val releaseDate: String = "",
    val releaseYear: Int = 0,
    val developers: List<String> = emptyList(),
    val publishers: List<String> = emptyList(),
    val genres: List<String> = emptyList(),
    val rawPlatforms: List<String> = emptyList(),
    val mappedConsoles: List<GameConsole> = emptyList(),
    val suggestedConsole: GameConsole = GameConsole.PS2,
    val metacriticScore: Int = 0,
    val websiteUrl: String = "",
    val rating: Float = 0f
) {
    val developerDisplay: String
        get() = developers.joinToString(", ").ifBlank { publishers.joinToString(", ") }

    val genreDisplay: String
        get() = genres.take(3).joinToString(", ")

    val platformsDisplay: String
        get() = PlatformMapper.formatSupportedPlatforms(mappedConsoles, rawPlatforms)
}

/**
 * Lightweight search candidate for the live search result picker.
 */
data class ExternalMetadataSearchResult(
    val externalGameId: String,
    val title: String,
    val slug: String = "",
    val releaseYear: Int = 0,
    val releaseDate: String = "",
    val coverArtUrl: String = "",
    val bannerArtUrl: String = "",
    val genres: List<String> = emptyList(),
    val rawPlatforms: List<String> = emptyList(),
    val mappedConsoles: List<GameConsole> = emptyList(),
    val suggestedConsole: GameConsole? = null,
    val developer: String = "",
    val metacriticScore: Int = 0,
    val confidenceScore: Float = 1.0f,
    val matchBadge: String = "", // e.g. "Exact Match", "Remaster", "Multi-Platform", "Edition"
    val isRemakeOrRemaster: Boolean = false
) {
    val genreDisplay: String
        get() = genres.take(2).joinToString(", ")

    val platformsSummary: String
        get() = if (mappedConsoles.isNotEmpty()) {
            mappedConsoles.take(3).joinToString(", ") { it.shortName } + if (mappedConsoles.size > 3) " +${mappedConsoles.size - 3}" else ""
        } else {
            rawPlatforms.take(2).joinToString(", ")
        }
}

/**
 * Loading and operation status for external metadata retrieval.
 */
enum class MetadataOperationStatus {
    IDLE,
    SEARCHING,
    LOADING_DETAILS,
    SUCCESS,
    NOT_FOUND,
    RATE_LIMITED,
    NETWORK_ERROR,
    UNAVAILABLE
}

data class MetadataSearchState(
    val status: MetadataOperationStatus = MetadataOperationStatus.IDLE,
    val query: String = "",
    val results: List<ExternalMetadataSearchResult> = emptyList(),
    val errorMessage: String? = null,
    val isRateLimited: Boolean = false,
    val usingFallback: Boolean = false
)
