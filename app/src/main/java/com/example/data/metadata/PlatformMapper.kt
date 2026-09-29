package com.example.data.metadata

import com.example.data.model.GameConsole

object PlatformMapper {

    /**
     * Maps an external platform slug or name (from RAWG, IGDB, or general search)
     * into the application's native [GameConsole] enum.
     */
    fun mapToConsole(slugOrName: String): GameConsole? {
        val clean = slugOrName.trim().lowercase()
            .replace(Regex("[^a-z0-9]"), "-")
            .replace(Regex("-+"), "-")

        return when {
            // PlayStation Family
            clean.contains("playstation-5") || clean == "ps5" || clean.contains("ps-5") -> GameConsole.PS5
            clean.contains("playstation-4") || clean == "ps4" || clean.contains("ps-4") -> GameConsole.PS4
            clean.contains("playstation-3") || clean == "ps3" || clean.contains("ps-3") -> GameConsole.PS3
            clean.contains("playstation-2") || clean == "ps2" || clean.contains("ps-2") -> GameConsole.PS2
            clean.contains("ps-vita") || clean.contains("playstation-vita") || clean == "vita" || clean.contains("psvita") -> GameConsole.PS_VITA
            clean.contains("psp") || clean.contains("playstation-portable") -> GameConsole.PSP

            // Nintendo Family
            clean.contains("nintendo-switch") || clean == "switch" -> GameConsole.SWITCH
            clean.contains("nintendo-3ds") || clean == "3ds" || clean.contains("new-nintendo-3ds") -> GameConsole.NINTENDO_3DS
            clean.contains("nintendo-ds") || clean == "nds" || clean == "nintendo-dsi" -> GameConsole.NINTENDO_DS
            clean.contains("wii-u") || clean == "wiiu" -> GameConsole.WII_U
            clean.contains("wii") -> GameConsole.WII
            clean.contains("gamecube") || clean.contains("nintendo-gamecube") || clean == "gc" -> GameConsole.GAMECUBE
            clean.contains("gameboy-advance") || clean == "gba" || clean.contains("game-boy-advance") -> GameConsole.GBA
            clean.contains("nintendo-64") || clean == "n64" -> GameConsole.N64

            // Xbox Family
            clean.contains("xbox-one") || clean.contains("xbox-series") || clean == "xbox-one-x" || clean == "xbox-one-s" -> GameConsole.XBOX_ONE
            clean.contains("xbox-360") || clean == "xbox360" -> GameConsole.XBOX_360
            clean.contains("xbox-old") || clean.contains("xbox-original") || clean == "xbox" -> GameConsole.XBOX

            // PC / Computer
            clean.contains("pc") || clean.contains("windows") || clean.contains("steam") ||
                clean.contains("macos") || clean.contains("mac") || clean.contains("linux") -> GameConsole.PC

            else -> null
        }
    }

    /**
     * Takes a list of raw external platform strings and resolves all recognized [GameConsole] instances,
     * sorted chronologically / according to the app's standard order.
     */
    fun mapPlatformsToConsoles(platformNamesOrSlugs: List<String>): List<GameConsole> {
        val resolved = mutableSetOf<GameConsole>()
        for (item in platformNamesOrSlugs) {
            val console = mapToConsole(item)
            if (console != null && console != GameConsole.ALL) {
                resolved.add(console)
            }
        }
        return resolved.toList()
    }

    /**
     * Formats a list of platform names or mapped consoles into a user-friendly comma separated string.
     */
    fun formatSupportedPlatforms(consoles: List<GameConsole>, rawPlatformNames: List<String> = emptyList()): String {
        if (consoles.isNotEmpty()) {
            return consoles.joinToString(", ") { it.displayName }
        }
        return rawPlatformNames.take(5).joinToString(", ")
    }
}
