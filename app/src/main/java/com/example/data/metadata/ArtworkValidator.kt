package com.example.data.metadata

import com.example.data.model.Game
import com.example.data.model.GameConsole

/**
 * Strict Validator for Game Back Artwork & Complete Case Wraps.
 *
 * Core Mandates:
 * - Back artwork MUST belong to the EXACT same game and platform as the selected game.
 * - Under NO circumstances may artwork belonging to another game or platform be displayed.
 * - Random back cover selection, global back cover pools, and generic screenshot fallbacks are REJECTED.
 * - Missing back artwork is preferred over incorrect artwork (falling back cleanly to the authentic
 *   physical case procedural layout).
 */
object ArtworkValidator {

    private data class KnownAssetRule(
        val assetKey: String,
        val expectedTitleKeyword: String,
        val expectedConsoleId: String
    )

    private val KNOWN_ASSET_RULES = listOf(
        KnownAssetRule("gta_sa_complete_case", "san andreas", "ps2"),
        KnownAssetRule("re4_complete_case", "resident evil 4", "ps2"),
        KnownAssetRule("mgs_complete_case", "metal gear solid", "ps2"),
        KnownAssetRule("halo_complete_case", "halo", "xbox"),
        KnownAssetRule("gametdb.com/wii", "", "wii"),
        KnownAssetRule("gametdb.com/gamecube", "", "gamecube")
    )

    /**
     * Validates whether a back cover URL is legitimate and strictly belongs to the given game.
     * Rejects generic screenshots, pooled images, and cross-game assets.
     */
    fun isBackCoverValidForGame(game: Game, backCoverUrl: String): Boolean {
        return isBackCoverValidForGame(game.title, game.console, backCoverUrl)
    }

    fun isBackCoverValidForGame(title: String, console: GameConsole?, backCoverUrl: String): Boolean {
        if (backCoverUrl.isBlank()) return false

        // Always allow local user-selected gallery photos or file URIs
        if (backCoverUrl.startsWith("file:") || backCoverUrl.startsWith("content:") || backCoverUrl.startsWith("/")) {
            return true
        }

        // 1. REJECT raw in-game screenshots from IGDB or other screenshot pools
        if (backCoverUrl.contains("t_screenshot_big") ||
            backCoverUrl.contains("t_screenshot_med") ||
            backCoverUrl.contains("images.igdb.com/igdb/image/upload/t_screenshot")
        ) {
            return false
        }

        // 2. REJECT assets that belong to a different game or console
        val lowerUrl = backCoverUrl.lowercase()
        val consoleId = console?.id.orEmpty()
        for (rule in KNOWN_ASSET_RULES) {
            if (lowerUrl.contains(rule.assetKey.lowercase())) {
                val normGameTitle = normalizeTitle(title)
                if (rule.expectedTitleKeyword.isNotBlank() && !normGameTitle.contains(rule.expectedTitleKeyword)) {
                    return false
                }
                if (rule.expectedConsoleId.isNotBlank() && consoleId.isNotBlank() && !consoleId.equals(rule.expectedConsoleId, ignoreCase = true)) {
                    return false
                }
            }
        }

        return true
    }

    /**
     * Validates whether a complete continuous case wrap (front + spine + back)
     * strictly belongs to the given game and platform.
     */
    fun isCompleteWrapValidForGame(game: Game, wrapUrl: String): Boolean {
        return isCompleteWrapValidForGame(game.title, game.console, wrapUrl)
    }

    fun isCompleteWrapValidForGame(title: String, console: GameConsole?, wrapUrl: String): Boolean {
        if (wrapUrl.isBlank()) return false

        // Always allow local user-selected gallery photos or file URIs
        if (wrapUrl.startsWith("file:") || wrapUrl.startsWith("content:") || wrapUrl.startsWith("/")) {
            return true
        }

        val lowerUrl = wrapUrl.lowercase()
        val consoleId = console?.id.orEmpty()
        for (rule in KNOWN_ASSET_RULES) {
            if (lowerUrl.contains(rule.assetKey.lowercase())) {
                val normGameTitle = normalizeTitle(title)
                if (rule.expectedTitleKeyword.isNotBlank() && !normGameTitle.contains(rule.expectedTitleKeyword)) {
                    return false
                }
                if (rule.expectedConsoleId.isNotBlank() && consoleId.isNotBlank() && !consoleId.equals(rule.expectedConsoleId, ignoreCase = true)) {
                    return false
                }
            }
        }

        return true
    }

    /**
     * Validates that a CoverProjectVariant matches the specific game and platform before application.
     */
    fun isValidVariantForGame(game: Game, variant: CoverProjectVariant): Boolean {
        val titleMatch = normalizeTitle(game.title).contains(normalizeTitle(variant.title)) ||
                normalizeTitle(variant.title).contains(normalizeTitle(game.title))
        val consoleMatch = game.consoleId.equals(variant.consoleId, ignoreCase = true)
        return titleMatch && consoleMatch
    }

    fun normalizeTitle(raw: String): String {
        return raw.lowercase().trim()
            .replace(":", "")
            .replace("-", " ")
            .replace("'", "")
            .replace(".", "")
            .replace(Regex("\\s+"), " ")
    }
}
