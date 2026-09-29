package com.example.data.model

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.compose.ui.graphics.Color

@Immutable
enum class GameConsole(
    val id: String,
    val displayName: String,
    val shortName: String,
    val mediaType: String,
    val brandColorHex: Long,
    val accentColorHex: Long,
    val bannerText: String,
    val emoji: String,
    val isDisc: Boolean,
    val category: String = "All",
    val releaseYear: Int = 0
) {
    ALL(
        id = "ALL",
        displayName = "All Consoles",
        shortName = "All",
        mediaType = "All Media",
        brandColorHex = 0xFF6366F1,
        accentColorHex = 0xFF818CF8,
        bannerText = "ALL PLATFORMS",
        emoji = "🎮",
        isDisc = true,
        category = "Overview",
        releaseYear = 0
    ),

    // ==========================================
    // 1️⃣ FIRST: PC
    // ==========================================
    PC(
        id = "PC",
        displayName = "PC / Windows",
        shortName = "PC",
        mediaType = "CD-ROM / Digital",
        brandColorHex = 0xFF171A21,
        accentColorHex = 0xFF66C0F4,
        bannerText = "PC CD-ROM",
        emoji = "💻",
        isDisc = true,
        category = "PC",
        releaseYear = 1981
    ),

    // ==========================================
    // 2️⃣ SECOND: PLAYSTATION (CHRONOLOGICAL ORDER)
    // ==========================================
    PS2(
        id = "PS2",
        displayName = "PlayStation 2",
        shortName = "PS2",
        mediaType = "DVD-ROM Disc",
        brandColorHex = 0xFF0F172A,
        accentColorHex = 0xFF2563EB,
        bannerText = "PlayStation 2",
        emoji = "💿",
        isDisc = true,
        category = "PlayStation",
        releaseYear = 2000
    ),
    PSP(
        id = "PSP",
        displayName = "PlayStation Portable",
        shortName = "PSP",
        mediaType = "Universal Media Disc (UMD)",
        brandColorHex = 0xFF1E293B,
        accentColorHex = 0xFF38BDF8,
        bannerText = "PSP™",
        emoji = "🕹️",
        isDisc = true,
        category = "PlayStation",
        releaseYear = 2004
    ),
    PS3(
        id = "PS3",
        displayName = "PlayStation 3",
        shortName = "PS3",
        mediaType = "Blu-ray Disc",
        brandColorHex = 0xFF18181B,
        accentColorHex = 0xFFEF4444,
        bannerText = "PlayStation 3",
        emoji = "📀",
        isDisc = true,
        category = "PlayStation",
        releaseYear = 2006
    ),
    PS_VITA(
        id = "PS_VITA",
        displayName = "PlayStation Vita",
        shortName = "PS Vita",
        mediaType = "NVG Game Card",
        brandColorHex = 0xFF003791,
        accentColorHex = 0xFF0096E6,
        bannerText = "PlayStation®Vita",
        emoji = "🎮",
        isDisc = false,
        category = "PlayStation",
        releaseYear = 2011
    ),
    PS4(
        id = "PS4",
        displayName = "PlayStation 4",
        shortName = "PS4",
        mediaType = "Blu-ray Disc",
        brandColorHex = 0xFF003791,
        accentColorHex = 0xFF0070D1,
        bannerText = "PlayStation 4",
        emoji = "🎮",
        isDisc = true,
        category = "PlayStation",
        releaseYear = 2013
    ),
    PS5(
        id = "PS5",
        displayName = "PlayStation 5",
        shortName = "PS5",
        mediaType = "Ultra HD Blu-ray Disc",
        brandColorHex = 0xFF0B101E,
        accentColorHex = 0xFF38BDF8,
        bannerText = "PlayStation 5",
        emoji = "⚡",
        isDisc = true,
        category = "PlayStation",
        releaseYear = 2020
    ),

    // ==========================================
    // 3️⃣ THIRD: NINTENDO (CHRONOLOGICAL ORDER)
    // ==========================================
    N64(
        id = "N64",
        displayName = "Nintendo 64",
        shortName = "N64",
        mediaType = "N64 Cartridge",
        brandColorHex = 0xFF1E1B4B,
        accentColorHex = 0xFFF59E0B,
        bannerText = "NINTENDO 64",
        emoji = "🕹️",
        isDisc = false,
        category = "Nintendo",
        releaseYear = 1996
    ),
    GBA(
        id = "GBA",
        displayName = "Game Boy Advance",
        shortName = "GBA",
        mediaType = "GBA Game Pak Cartridge",
        brandColorHex = 0xFF3B1D70,
        accentColorHex = 0xFFA78BFA,
        bannerText = "GAME BOY ADVANCE",
        emoji = "👾",
        isDisc = false,
        category = "Nintendo",
        releaseYear = 2001
    ),
    GAMECUBE(
        id = "GAMECUBE",
        displayName = "Nintendo GameCube",
        shortName = "GameCube",
        mediaType = "8cm Mini DVD-ROM",
        brandColorHex = 0xFF432C7A,
        accentColorHex = 0xFFA855F7,
        bannerText = "NINTENDO GAMECUBE™",
        emoji = "🟣",
        isDisc = true,
        category = "Nintendo",
        releaseYear = 2001
    ),
    NINTENDO_DS(
        id = "NINTENDO_DS",
        displayName = "Nintendo DS",
        shortName = "NDS",
        mediaType = "DS Game Card",
        brandColorHex = 0xFF1E293B,
        accentColorHex = 0xFF0EA5E9,
        bannerText = "NINTENDO DS™",
        emoji = "📱",
        isDisc = false,
        category = "Nintendo",
        releaseYear = 2004
    ),
    WII(
        id = "WII",
        displayName = "Nintendo Wii",
        shortName = "Wii",
        mediaType = "Wii Optical Disc",
        brandColorHex = 0xFFF1F5F9,
        accentColorHex = 0xFF0EA5E9,
        bannerText = "Wii",
        emoji = "⚪",
        isDisc = true,
        category = "Nintendo",
        releaseYear = 2006
    ),
    NINTENDO_3DS(
        id = "NINTENDO_3DS",
        displayName = "Nintendo 3DS",
        shortName = "3DS",
        mediaType = "3DS Game Card",
        brandColorHex = 0xFFDC2626,
        accentColorHex = 0xFFEA580C,
        bannerText = "NINTENDO 3DS™",
        emoji = "🟥",
        isDisc = false,
        category = "Nintendo",
        releaseYear = 2011
    ),
    WII_U(
        id = "WII_U",
        displayName = "Nintendo Wii U",
        shortName = "Wii U",
        mediaType = "Wii U Optical Disc",
        brandColorHex = 0xFF003B5C,
        accentColorHex = 0xFF00B4D8,
        bannerText = "Wii U™",
        emoji = "🎮",
        isDisc = true,
        category = "Nintendo",
        releaseYear = 2012
    ),
    SWITCH(
        id = "SWITCH",
        displayName = "Nintendo Switch",
        shortName = "Switch",
        mediaType = "Switch Game Card",
        brandColorHex = 0xFFE11D48,
        accentColorHex = 0xFF06B6D4,
        bannerText = "NINTENDO SWITCH™",
        emoji = "🔴",
        isDisc = false,
        category = "Nintendo",
        releaseYear = 2017
    ),

    // ==========================================
    // 4️⃣ FOURTH: XBOX (CHRONOLOGICAL ORDER)
    // ==========================================
    XBOX(
        id = "XBOX",
        displayName = "Xbox (Original)",
        shortName = "Xbox",
        mediaType = "DVD-ROM Disc",
        brandColorHex = 0xFF052E16,
        accentColorHex = 0xFF10B981,
        bannerText = "XBOX™",
        emoji = "🟢",
        isDisc = true,
        category = "Xbox",
        releaseYear = 2001
    ),
    XBOX_360(
        id = "XBOX_360",
        displayName = "Xbox 360",
        shortName = "Xbox 360",
        mediaType = "DVD-ROM (XGD2/XGD3)",
        brandColorHex = 0xFF14532D,
        accentColorHex = 0xFF22C55E,
        bannerText = "XBOX 360",
        emoji = "🟢",
        isDisc = true,
        category = "Xbox",
        releaseYear = 2005
    ),
    XBOX_ONE(
        id = "XBOX_ONE",
        displayName = "Xbox One",
        shortName = "Xbox One",
        mediaType = "Blu-ray Disc",
        brandColorHex = 0xFF064E3B,
        accentColorHex = 0xFF10B981,
        bannerText = "XBOX ONE",
        emoji = "🟩",
        isDisc = true,
        category = "Xbox",
        releaseYear = 2013
    );

    companion object {
        fun fromId(id: String): GameConsole {
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: ALL
        }

        fun selectableConsoles(): List<GameConsole> {
            return entries.filter { it != ALL }
        }
    }

    fun defaultEmulators(): List<String> {
        return when (this) {
            PSP -> listOf("PPSSPP", "RetroArch (PPSSPP)")
            PS_VITA -> listOf("Vita3K")
            PS2 -> listOf("PCSX2", "NetherSX2", "AetherSX2", "Play!")
            PC -> listOf("PC Native", "Steam", "Heroic", "DOSBox")
            PS3 -> listOf("RPCS3")
            PS4 -> listOf("ShadPS4", "fpPS4", "Spine", "Kyty")
            PS5 -> listOf("Kyty", "RPCSX", "Cloud / Remote")
            GBA -> listOf("mGBA", "VBA-M", "Pizza Boy GBA", "RetroArch (mGBA)")
            N64 -> listOf("Simple64", "Mupen64Plus", "Project64", "RetroArch (ParaLLEl N64)")
            NINTENDO_DS -> listOf("MelonDS", "DeSmuME", "DraStic", "RetroArch (melonDS)")
            XBOX -> listOf("xemu", "Cxbx-Reloaded")
            XBOX_360 -> listOf("Xenia", "Xenia Canary", "RetroArch")
            XBOX_ONE -> listOf("Xbox Dev Mode", "Xbox Cloud Gaming", "Xenia (BC)")
            WII -> listOf("Dolphin")
            WII_U -> listOf("Cemu", "RetroArch (Cemu)")
            GAMECUBE -> listOf("Dolphin")
            SWITCH -> listOf("Ryujinx", "Yuzu", "Sudachi", "Suyu", "Eden")
            NINTENDO_3DS -> listOf("Citra", "Lime3DS", "Citra Enhanced", "Mandarine")
            ALL -> listOf("PCSX2", "RPCS3", "ShadPS4", "Dolphin", "mGBA", "Simple64", "MelonDS", "Xenia", "xemu", "PPSSPP", "Vita3K", "Citra", "Ryujinx", "RetroArch")
        }
    }

    fun primaryEmulator(): String {
        return defaultEmulators().firstOrNull() ?: ""
    }
}

@Immutable
enum class WishlistStatus(
    val id: String,
    val label: String,
    val badgeColorHex: Long,
    val iconEmoji: String
) {
    WANT_TO_PLAY("WANT_TO_PLAY", "Want to Play", 0xFF818CF8, "✨"),
    TOP_PRIORITY("TOP_PRIORITY", "Top Priority", 0xFFF59E0B, "🔥"),
    PREORDER("PREORDER", "Pre-Ordered", 0xFFEC4899, "📦"),
    PLAYING("PLAYING", "Currently Playing", 0xFF10B981, "🎮"),
    ACQUIRED("ACQUIRED", "In Collection", 0xFF3B82F6, "🏆"),
    COMPLETED("COMPLETED", "Completed", 0xFF8B5CF6, "👑");

    companion object {
        fun fromId(id: String): WishlistStatus {
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: WANT_TO_PLAY
        }
    }
}

@Immutable
@Entity(tableName = "games")
data class Game(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val consoleId: String,
    val genre: String = "Action",
    val releaseYear: Int = 2010,
    val emulator: String = "",
    val youtubeVideoId: String = "",
    val coverArtUrl: String = "",
    val coverThumbUrl: String = "",
    val backCoverUrl: String = "",
    val completeCaseArtwork: String = "",
    val coverSource: String = CoverSourceType.EXISTING,
    val backCoverSource: String = "",
    val coverGradientStart: Long = 0xFF1E1B4B,
    val coverGradientEnd: Long = 0xFF0F172A,
    val coverAccentColor: Long = 0xFF6366F1,
    val wishlistStatus: String = WishlistStatus.WANT_TO_PLAY.id,
    val userRating: Float = 4.5f,
    val targetPrice: String = "$29.99",
    val developer: String = "",
    val notes: String = "",
    val isFavorite: Boolean = false,
    val addedTimestamp: Long = System.currentTimeMillis(),
    val userEmail: String = "",

    // Stored Emulator Notes & Configuration Settings for Offline Access
    val emulatorNotes: String = "",
    val recommendedBackend: String = "",
    val internalResolution: String = "",
    val compatibilityRating: String = "",
    val biosRequirement: String = "",
    val targetFramerate: String = "",
    val controllerLayout: String = "",
    val proTips: String = "",

    // Time to Beat (HowLongToBeat) Completion Times Metadata
    val hltbId: Long = 0L,
    val hltbName: String = "",
    val hltbMainStoryHours: Float = 0f,
    val hltbMainExtraHours: Float = 0f,
    val hltbCompletionistHours: Float = 0f,
    val hltbLastUpdated: Long = 0L,
    val hltbSyncStatus: String = "", // "SYNCED", "NOT_FOUND", "ERROR", "UNRESOLVED"

    // External Game Database Metadata (RAWG / IGDB)
    val externalProvider: String = "RAWG", // "RAWG", "IGDB", "LOCAL"
    val externalGameId: String = "",       // Unique API ID or Slug (e.g. "3328", "the-witcher-3-wild-hunt")
    val externalLastUpdated: Long = 0L,
    val externalSyncStatus: String = "",   // "SYNCED", "NOT_FOUND", "ERROR"
    val bannerArtUrl: String = "",         // High-res banner / background artwork
    val publisher: String = "",            // Publisher studio
    val releaseDate: String = "",          // Full release date "YYYY-MM-DD"
    val description: String = "",          // Official game synopsis / overview
    val websiteUrl: String = "",           // Official website URL
    val metacriticScore: Int = 0,          // Metacritic review score (0-100)
    val supportedPlatforms: String = ""    // Formatted list of mapped platforms (e.g. "PC, PS4, Xbox One, Nintendo Switch")
) {
    val console: GameConsole
        get() = GameConsole.fromId(consoleId)

    val status: WishlistStatus
        get() = WishlistStatus.fromId(wishlistStatus)

    val frontCover: String
        get() = coverArtUrl

    val listCover: String
        get() = coverThumbUrl.ifBlank { coverArtUrl }

    val hasCompleteCaseArtwork: Boolean
        get() = completeCaseArtwork.isNotBlank()

    val hasHltbData: Boolean
        get() = hltbId > 0L && (hltbMainStoryHours > 0f || hltbMainExtraHours > 0f || hltbCompletionistHours > 0f)

    val hasExternalMetadata: Boolean
        get() = externalGameId.isNotBlank() || bannerArtUrl.isNotBlank() || publisher.isNotBlank() || description.isNotBlank() || metacriticScore > 0

    val formattedMainStory: String
        get() = formatHours(hltbMainStoryHours)

    val formattedMainExtra: String
        get() = formatHours(hltbMainExtraHours)

    val formattedCompletionist: String
        get() = formatHours(hltbCompletionistHours)

    companion object {
        fun formatHours(hours: Float): String {
            return when {
                hours <= 0f -> "--"
                hours % 1f == 0f -> "${hours.toInt()}h"
                else -> "${String.format(java.util.Locale.US, "%.1f", hours)}h"
            }
        }
    }
}

/**
 * Deterministic source tracking for artwork in the Game Vault.
 * Priority hierarchy: MANUAL > EXISTING > COVER_PROJECT > PLACEHOLDER
 */
object CoverSourceType {
    const val STEAMGRIDDB = "steamgriddb"
    const val MANUAL = "manual"
    const val EXISTING = "existing"
    const val COVER_PROJECT = "coverProject"
    const val PLACEHOLDER = "placeholder"
}
