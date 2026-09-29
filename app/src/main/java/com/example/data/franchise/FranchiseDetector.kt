package com.example.data.franchise

import com.example.data.model.Game
import com.example.data.model.GameFranchise

/**
 * Intelligent franchise detector and cross-platform grouping engine.
 * Automatically identifies franchise series from titles and metadata,
 * grouping games across platforms while avoiding false positive clusters.
 */
object FranchiseDetector {

    private data class CuratedFranchiseRule(
        val canonicalName: String,
        val pattern: Regex,
        val bannerUrl: String = "",
        val description: String = ""
    )

    // Curated definitions for major gaming franchises with canonical naming
    private val CURATED_RULES: List<CuratedFranchiseRule> = listOf(
        CuratedFranchiseRule(
            canonicalName = "Assassin's Creed",
            pattern = Regex("""\bAssassin['’]?s?\s*Creed\b""", RegexOption.IGNORE_CASE)
        ),
        CuratedFranchiseRule(
            canonicalName = "Call of Duty",
            pattern = Regex("""\b(Call\s*of\s*Duty|CoD:\s*|CoD\s*\d)\b""", RegexOption.IGNORE_CASE)
        ),
        CuratedFranchiseRule(
            canonicalName = "Resident Evil",
            pattern = Regex("""\b(Resident\s*Evil|Biohazard)\b""", RegexOption.IGNORE_CASE)
        ),
        CuratedFranchiseRule(
            canonicalName = "Final Fantasy",
            pattern = Regex("""\bFinal\s*Fantasy\b""", RegexOption.IGNORE_CASE)
        ),
        CuratedFranchiseRule(
            canonicalName = "The Legend of Zelda",
            pattern = Regex("""\b(The\s*Legend\s*of\s*Zelda|Legend\s*of\s*Zelda|Zelda:)\b""", RegexOption.IGNORE_CASE)
        ),
        CuratedFranchiseRule(
            canonicalName = "God of War",
            pattern = Regex("""\bGod\s*of\s*War\b""", RegexOption.IGNORE_CASE)
        ),
        CuratedFranchiseRule(
            canonicalName = "Grand Theft Auto",
            pattern = Regex("""\b(Grand\s*Theft\s*Auto|GTA\s*([IVX\d]+|Vice|San|Liberty|Chinatown|Online))\b""", RegexOption.IGNORE_CASE)
        ),
        CuratedFranchiseRule(
            canonicalName = "Metal Gear",
            pattern = Regex("""\b(Metal\s*Gear(\s*Solid)?|MGS\s*([IVX\d]+|Peace|Rising))\b""", RegexOption.IGNORE_CASE)
        ),
        CuratedFranchiseRule(
            canonicalName = "Fallout",
            pattern = Regex("""\bFallout\b""", RegexOption.IGNORE_CASE)
        ),
        CuratedFranchiseRule(
            canonicalName = "The Elder Scrolls",
            pattern = Regex("""\b(The\s*Elder\s*Scrolls|Elder\s*Scrolls|Skyrim|Morrowind|Oblivion|Daggerfall)\b""", RegexOption.IGNORE_CASE)
        ),
        CuratedFranchiseRule(
            canonicalName = "Super Mario",
            pattern = Regex("""\b(Super\s*Mario|Mario\s*Bros|Paper\s*Mario|Mario\s*Kart|Mario\s*Party|Mario\s*Galaxy|Mario\s*Odyssey|Mario\s*Sunshine|Mario\s*64)\b""", RegexOption.IGNORE_CASE)
        ),
        CuratedFranchiseRule(
            canonicalName = "Pokémon",
            pattern = Regex("""\b(Pok[eé]mon)\b""", RegexOption.IGNORE_CASE)
        ),
        CuratedFranchiseRule(
            canonicalName = "Dark Souls",
            pattern = Regex("""\bDark\s*Souls\b""", RegexOption.IGNORE_CASE)
        ),
        CuratedFranchiseRule(
            canonicalName = "Demon's Souls",
            pattern = Regex("""\bDemon['’]?s\s*Souls\b""", RegexOption.IGNORE_CASE)
        ),
        CuratedFranchiseRule(
            canonicalName = "Bloodborne",
            pattern = Regex("""\bBloodborne\b""", RegexOption.IGNORE_CASE)
        ),
        CuratedFranchiseRule(
            canonicalName = "Elden Ring",
            pattern = Regex("""\bElden\s*Ring\b""", RegexOption.IGNORE_CASE)
        ),
        CuratedFranchiseRule(
            canonicalName = "The Witcher",
            pattern = Regex("""\b(The\s*Witcher|Witcher)\b""", RegexOption.IGNORE_CASE)
        ),
        CuratedFranchiseRule(
            canonicalName = "Kingdom Hearts",
            pattern = Regex("""\bKingdom\s*Hearts\b""", RegexOption.IGNORE_CASE)
        ),
        CuratedFranchiseRule(
            canonicalName = "Halo",
            pattern = Regex("""\bHalo(\s*(:\s*|Combat|Reach|Infinite|\d+|Wars|ODST|Master))""", RegexOption.IGNORE_CASE)
        ),
        CuratedFranchiseRule(
            canonicalName = "Uncharted",
            pattern = Regex("""\bUncharted\b""", RegexOption.IGNORE_CASE)
        ),
        CuratedFranchiseRule(
            canonicalName = "Mass Effect",
            pattern = Regex("""\bMass\s*Effect\b""", RegexOption.IGNORE_CASE)
        ),
        CuratedFranchiseRule(
            canonicalName = "Dragon Age",
            pattern = Regex("""\bDragon\s*Age\b""", RegexOption.IGNORE_CASE)
        ),
        CuratedFranchiseRule(
            canonicalName = "Monster Hunter",
            pattern = Regex("""\bMonster\s*Hunter\b""", RegexOption.IGNORE_CASE)
        ),
        CuratedFranchiseRule(
            canonicalName = "Persona",
            pattern = Regex("""\bPersona\b""", RegexOption.IGNORE_CASE)
        ),
        CuratedFranchiseRule(
            canonicalName = "Shin Megami Tensei",
            pattern = Regex("""\bShin\s*Megami\s*Tensei\b""", RegexOption.IGNORE_CASE)
        ),
        CuratedFranchiseRule(
            canonicalName = "Silent Hill",
            pattern = Regex("""\bSilent\s*Hill\b""", RegexOption.IGNORE_CASE)
        ),
        CuratedFranchiseRule(
            canonicalName = "Castlevania",
            pattern = Regex("""\bCastlevania\b""", RegexOption.IGNORE_CASE)
        ),
        CuratedFranchiseRule(
            canonicalName = "Crash Bandicoot",
            pattern = Regex("""\bCrash\s*Bandicoot\b""", RegexOption.IGNORE_CASE)
        ),
        CuratedFranchiseRule(
            canonicalName = "Spyro",
            pattern = Regex("""\bSpyro(\s*the\s*Dragon)?\b""", RegexOption.IGNORE_CASE)
        ),
        CuratedFranchiseRule(
            canonicalName = "Tomb Raider",
            pattern = Regex("""\b(Tomb\s*Raider|Lara\s*Croft)\b""", RegexOption.IGNORE_CASE)
        ),
        CuratedFranchiseRule(
            canonicalName = "Sonic the Hedgehog",
            pattern = Regex("""\bSonic(\s*(the\s*Hedgehog|Adventure|Frontiers|Mania|Generations|Colors|Unleashed|Forces|Heroes|CD))?\b""", RegexOption.IGNORE_CASE)
        ),
        CuratedFranchiseRule(
            canonicalName = "Doom",
            pattern = Regex("""\bDoom(\s*(II|III|IV|64|Eternal|2016|\d+))?\b""", RegexOption.IGNORE_CASE)
        ),
        CuratedFranchiseRule(
            canonicalName = "Wolfenstein",
            pattern = Regex("""\bWolfenstein\b""", RegexOption.IGNORE_CASE)
        ),
        CuratedFranchiseRule(
            canonicalName = "Half-Life",
            pattern = Regex("""\bHalf-Life\b""", RegexOption.IGNORE_CASE)
        ),
        CuratedFranchiseRule(
            canonicalName = "Portal",
            pattern = Regex("""\bPortal(\s*(\d+|Stories|Revolution))?\b""", RegexOption.IGNORE_CASE)
        ),
        CuratedFranchiseRule(
            canonicalName = "BioShock",
            pattern = Regex("""\bBioShock\b""", RegexOption.IGNORE_CASE)
        ),
        CuratedFranchiseRule(
            canonicalName = "Borderlands",
            pattern = Regex("""\bBorderlands\b""", RegexOption.IGNORE_CASE)
        ),
        CuratedFranchiseRule(
            canonicalName = "Far Cry",
            pattern = Regex("""\bFar\s*Cry\b""", RegexOption.IGNORE_CASE)
        ),
        CuratedFranchiseRule(
            canonicalName = "Devil May Cry",
            pattern = Regex("""\b(Devil\s*May\s*Cry|DmC:?\s*Devil\s*May\s*Cry)\b""", RegexOption.IGNORE_CASE)
        ),
        CuratedFranchiseRule(
            canonicalName = "Street Fighter",
            pattern = Regex("""\bStreet\s*Fighter\b""", RegexOption.IGNORE_CASE)
        ),
        CuratedFranchiseRule(
            canonicalName = "Tekken",
            pattern = Regex("""\bTekken\b""", RegexOption.IGNORE_CASE)
        ),
        CuratedFranchiseRule(
            canonicalName = "Mortal Kombat",
            pattern = Regex("""\bMortal\s*Kombat\b""", RegexOption.IGNORE_CASE)
        ),
        CuratedFranchiseRule(
            canonicalName = "Super Smash Bros.",
            pattern = Regex("""\b(Super\s*Smash\s*Bros|Smash\s*Bros)\b""", RegexOption.IGNORE_CASE)
        ),
        CuratedFranchiseRule(
            canonicalName = "Metroid",
            pattern = Regex("""\bMetroid\b""", RegexOption.IGNORE_CASE)
        ),
        CuratedFranchiseRule(
            canonicalName = "Kirby",
            pattern = Regex("""\bKirby\b""", RegexOption.IGNORE_CASE)
        ),
        CuratedFranchiseRule(
            canonicalName = "Donkey Kong",
            pattern = Regex("""\bDonkey\s*Kong\b""", RegexOption.IGNORE_CASE)
        ),
        CuratedFranchiseRule(
            canonicalName = "Mega Man",
            pattern = Regex("""\b(Mega\s*Man|Megaman)\b""", RegexOption.IGNORE_CASE)
        ),
        CuratedFranchiseRule(
            canonicalName = "Fire Emblem",
            pattern = Regex("""\bFire\s*Emblem\b""", RegexOption.IGNORE_CASE)
        ),
        CuratedFranchiseRule(
            canonicalName = "Xenoblade Chronicles",
            pattern = Regex("""\bXenoblade(\s*Chronicles)?\b""", RegexOption.IGNORE_CASE)
        ),
        CuratedFranchiseRule(
            canonicalName = "Bayonetta",
            pattern = Regex("""\bBayonetta\b""", RegexOption.IGNORE_CASE)
        ),
        CuratedFranchiseRule(
            canonicalName = "Yakuza / Like a Dragon",
            pattern = Regex("""\b(Yakuza|Like\s*a\s*Dragon|Judgment|Lost\s*Judgment)\b""", RegexOption.IGNORE_CASE)
        ),
        CuratedFranchiseRule(
            canonicalName = "Dragon Quest",
            pattern = Regex("""\bDragon\s*Quest\b""", RegexOption.IGNORE_CASE)
        ),
        CuratedFranchiseRule(
            canonicalName = "Need for Speed",
            pattern = Regex("""\b(Need\s*for\s*Speed|NFS:?\s*)\b""", RegexOption.IGNORE_CASE)
        ),
        CuratedFranchiseRule(
            canonicalName = "Battlefield",
            pattern = Regex("""\bBattlefield\b""", RegexOption.IGNORE_CASE)
        ),
        CuratedFranchiseRule(
            canonicalName = "Tom Clancy's",
            pattern = Regex("""\b(Tom\s*Clancy['’]?s|Splinter\s*Cell|Rainbow\s*Six|Ghost\s*Recon|The\s*Division)\b""", RegexOption.IGNORE_CASE)
        ),
        CuratedFranchiseRule(
            canonicalName = "Hitman",
            pattern = Regex("""\bHitman\b""", RegexOption.IGNORE_CASE)
        ),
        CuratedFranchiseRule(
            canonicalName = "Deus Ex",
            pattern = Regex("""\bDeus\s*Ex\b""", RegexOption.IGNORE_CASE)
        ),
        CuratedFranchiseRule(
            canonicalName = "Dishonored",
            pattern = Regex("""\bDishonored\b""", RegexOption.IGNORE_CASE)
        ),
        CuratedFranchiseRule(
            canonicalName = "Watch Dogs",
            pattern = Regex("""\bWatch\s*Dogs?\b""", RegexOption.IGNORE_CASE)
        ),
        CuratedFranchiseRule(
            canonicalName = "Dead Space",
            pattern = Regex("""\bDead\s*Space\b""", RegexOption.IGNORE_CASE)
        ),
        CuratedFranchiseRule(
            canonicalName = "Batman: Arkham",
            pattern = Regex("""\b(Batman:?\s*Arkham|Batman\s*Arkham)\b""", RegexOption.IGNORE_CASE)
        ),
        CuratedFranchiseRule(
            canonicalName = "Spider-Man",
            pattern = Regex("""\b(Marvel['’]?s\s*)?Spider-Man\b""", RegexOption.IGNORE_CASE)
        ),
        CuratedFranchiseRule(
            canonicalName = "Ratchet & Clank",
            pattern = Regex("""\bRatchet\s*(&|and)\s*Clank\b""", RegexOption.IGNORE_CASE)
        ),
        CuratedFranchiseRule(
            canonicalName = "Jak and Daxter",
            pattern = Regex("""\b(Jak\s*(&|and)\s*Daxter|Jak\s*(II|2|3|X))\b""", RegexOption.IGNORE_CASE)
        ),
        CuratedFranchiseRule(
            canonicalName = "Sly Cooper",
            pattern = Regex("""\b(Sly\s*Cooper|Sly\s*(2|3|4))\b""", RegexOption.IGNORE_CASE)
        ),
        CuratedFranchiseRule(
            canonicalName = "Gran Turismo",
            pattern = Regex("""\bGran\s*Turismo\b""", RegexOption.IGNORE_CASE)
        ),
        CuratedFranchiseRule(
            canonicalName = "Forza",
            pattern = Regex("""\bForza(\s*(Horizon|Motorsport))?\b""", RegexOption.IGNORE_CASE)
        ),
        CuratedFranchiseRule(
            canonicalName = "Gears of War",
            pattern = Regex("""\b(Gears\s*of\s*War|Gears\s*(4|5|Tactics))\b""", RegexOption.IGNORE_CASE)
        ),
        CuratedFranchiseRule(
            canonicalName = "Red Dead",
            pattern = Regex("""\bRed\s*Dead(\s*(Redemption|Revolver))?\b""", RegexOption.IGNORE_CASE)
        ),
        CuratedFranchiseRule(
            canonicalName = "Max Payne",
            pattern = Regex("""\bMax\s*Payne\b""", RegexOption.IGNORE_CASE)
        ),
        CuratedFranchiseRule(
            canonicalName = "Alan Wake",
            pattern = Regex("""\bAlan\s*Wake\b""", RegexOption.IGNORE_CASE)
        ),
        CuratedFranchiseRule(
            canonicalName = "Horizon",
            pattern = Regex("""\bHorizon(\s*(:\s*)?(Zero\s*Dawn|Forbidden\s*West|Call\s*of\s*the\s*Mountain))\b""", RegexOption.IGNORE_CASE)
        ),
        CuratedFranchiseRule(
            canonicalName = "The Last of Us",
            pattern = Regex("""\bThe\s*Last\s*of\s*Us\b""", RegexOption.IGNORE_CASE)
        ),
        CuratedFranchiseRule(
            canonicalName = "Diablo",
            pattern = Regex("""\bDiablo\b""", RegexOption.IGNORE_CASE)
        ),
        CuratedFranchiseRule(
            canonicalName = "StarCraft",
            pattern = Regex("""\bStarCraft\b""", RegexOption.IGNORE_CASE)
        ),
        CuratedFranchiseRule(
            canonicalName = "Warcraft",
            pattern = Regex("""\b(Warcraft|World\s*of\s*Warcraft)\b""", RegexOption.IGNORE_CASE)
        ),
        CuratedFranchiseRule(
            canonicalName = "Civilization",
            pattern = Regex("""\b(Sid\s*Meier['’]?s\s*)?Civilization\b""", RegexOption.IGNORE_CASE)
        ),
        CuratedFranchiseRule(
            canonicalName = "Age of Empires",
            pattern = Regex("""\bAge\s*of\s*Empires\b""", RegexOption.IGNORE_CASE)
        ),
        CuratedFranchiseRule(
            canonicalName = "Total War",
            pattern = Regex("""\bTotal\s*War\b""", RegexOption.IGNORE_CASE)
        ),
        CuratedFranchiseRule(
            canonicalName = "Rayman",
            pattern = Regex("""\bRayman\b""", RegexOption.IGNORE_CASE)
        ),
        CuratedFranchiseRule(
            canonicalName = "Prince of Persia",
            pattern = Regex("""\bPrince\s*of\s*Persia\b""", RegexOption.IGNORE_CASE)
        ),
        CuratedFranchiseRule(
            canonicalName = "Darksiders",
            pattern = Regex("""\bDarksiders\b""", RegexOption.IGNORE_CASE)
        ),
        CuratedFranchiseRule(
            canonicalName = "Just Cause",
            pattern = Regex("""\bJust\s*Cause\b""", RegexOption.IGNORE_CASE)
        ),
        CuratedFranchiseRule(
            canonicalName = "Saints Row",
            pattern = Regex("""\bSaints\s*Row\b""", RegexOption.IGNORE_CASE)
        ),
        CuratedFranchiseRule(
            canonicalName = "Mafia",
            pattern = Regex("""\bMafia(\s*([I\d]+|:\s*Definitive|Definitive))?\b""", RegexOption.IGNORE_CASE)
        ),
        CuratedFranchiseRule(
            canonicalName = "Baldur's Gate",
            pattern = Regex("""\bBaldur['’]?s\s*Gate\b""", RegexOption.IGNORE_CASE)
        ),
        CuratedFranchiseRule(
            canonicalName = "Divinity",
            pattern = Regex("""\bDivinity(\s*:\s*Original\s*Sin)?\b""", RegexOption.IGNORE_CASE)
        ),
        CuratedFranchiseRule(
            canonicalName = "XCOM",
            pattern = Regex("""\bX-?COM\b""", RegexOption.IGNORE_CASE)
        ),
        CuratedFranchiseRule(
            canonicalName = "Sniper Elite",
            pattern = Regex("""\bSniper\s*Elite\b""", RegexOption.IGNORE_CASE)
        ),
        CuratedFranchiseRule(
            canonicalName = "Payday",
            pattern = Regex("""\bPayday\b""", RegexOption.IGNORE_CASE)
        ),
        CuratedFranchiseRule(
            canonicalName = "Armored Core",
            pattern = Regex("""\bArmored\s*Core\b""", RegexOption.IGNORE_CASE)
        ),
        CuratedFranchiseRule(
            canonicalName = "Ace Combat",
            pattern = Regex("""\bAce\s*Combat\b""", RegexOption.IGNORE_CASE)
        ),
        CuratedFranchiseRule(
            canonicalName = "Tales of Series",
            pattern = Regex("""\bTales\s*of\s+[A-Za-z]+""", RegexOption.IGNORE_CASE)
        ),
        CuratedFranchiseRule(
            canonicalName = "Crysis",
            pattern = Regex("""\bCrysis\b""", RegexOption.IGNORE_CASE)
        ),
        CuratedFranchiseRule(
            canonicalName = "Metro",
            pattern = Regex("""\bMetro\s*(2033|Last\s*Light|Exodus|Redux)?\b""", RegexOption.IGNORE_CASE)
        ),
        CuratedFranchiseRule(
            canonicalName = "Titanfall",
            pattern = Regex("""\bTitanfall\b""", RegexOption.IGNORE_CASE)
        ),
        CuratedFranchiseRule(
            canonicalName = "LittleBigPlanet",
            pattern = Regex("""\b(LittleBigPlanet|Sackboy)\b""", RegexOption.IGNORE_CASE)
        ),
        CuratedFranchiseRule(
            canonicalName = "Hollow Knight",
            pattern = Regex("""\bHollow\s*Knight\b""", RegexOption.IGNORE_CASE)
        ),
        CuratedFranchiseRule(
            canonicalName = "Ori",
            pattern = Regex("""\bOri\s*and\s*the\b""", RegexOption.IGNORE_CASE)
        ),
        CuratedFranchiseRule(
            canonicalName = "Cyberpunk",
            pattern = Regex("""\bCyberpunk(\s*2077)?\b""", RegexOption.IGNORE_CASE)
        ),
        CuratedFranchiseRule(
            canonicalName = "Star Wars",
            pattern = Regex("""\bStar\s*Wars\b""", RegexOption.IGNORE_CASE)
        ),
        CuratedFranchiseRule(
            canonicalName = "Banjo-Kazooie",
            pattern = Regex("""\bBanjo-(Kazooie|Tooie)\b""", RegexOption.IGNORE_CASE)
        ),
        CuratedFranchiseRule(
            canonicalName = "Chrono",
            pattern = Regex("""\bChrono\s*(Trigger|Cross)\b""", RegexOption.IGNORE_CASE)
        ),
        CuratedFranchiseRule(
            canonicalName = "Ace Attorney",
            pattern = Regex("""\b(Phoenix\s*Wright|Ace\s*Attorney)\b""", RegexOption.IGNORE_CASE)
        ),
        CuratedFranchiseRule(
            canonicalName = "Danganronpa",
            pattern = Regex("""\bDanganronpa\b""", RegexOption.IGNORE_CASE)
        ),
        CuratedFranchiseRule(
            canonicalName = "Life is Strange",
            pattern = Regex("""\bLife\s*is\s*Strange\b""", RegexOption.IGNORE_CASE)
        ),
        CuratedFranchiseRule(
            canonicalName = "Guilty Gear",
            pattern = Regex("""\bGuilty\s*Gear\b""", RegexOption.IGNORE_CASE)
        ),
        CuratedFranchiseRule(
            canonicalName = "Soulcalibur",
            pattern = Regex("""\b(Soul\s*Calibur|Soulcalibur)\b""", RegexOption.IGNORE_CASE)
        ),
        CuratedFranchiseRule(
            canonicalName = "Dead or Alive",
            pattern = Regex("""\bDead\s*or\s*Alive\b""", RegexOption.IGNORE_CASE)
        ),
        CuratedFranchiseRule(
            canonicalName = "BlazBlue",
            pattern = Regex("""\bBlazBlue\b""", RegexOption.IGNORE_CASE)
        )
    )

    // Stop words that must NEVER be treated as algorithmic franchise roots
    private val INVALID_FRANCHISE_ROOTS = setOf(
        "the", "a", "an", "super", "ultra", "mega", "ultimate", "final",
        "chronicles", "adventures", "chronicle", "adventure", "quest",
        "war", "wars", "world", "worlds", "battle", "combat",
        "hero", "heroes", "legend", "legends", "dark", "game",
        "edition", "collection", "party", "pro", "star", "stars",
        "lord", "lords", "age", "kingdom", "monster", "new", "classic",
        "pocket", "mobile", "origin", "origins", "tales", "remake",
        "remaster", "hd", "vr", "goty", "deluxe", "special"
    )

    private val EDITION_CLEANER = Regex(
        """(?i)\s*[\(\[]?(Remake|Remastered|Definitive\s*Edition|HD\s*Remaster|Director'?s\s*Cut|GOTY|Game\s*of\s*the\s*Year\s*Edition|Anniversary\s*Edition|Special\s*Edition|Complete\s*Edition|Deluxe\s*Edition|Enhanced\s*Edition|HD|VR)[\)\]]?"""
    )

    private val NUMBER_TRIMMER = Regex(
        """(?i)\s+((X{0,3}(IX|IV|V?I{0,3}))|\d+)$"""
    )

    /**
     * Determines the franchise for a given game.
     * Checks curated dictionary first, then falls back to algorithmic detection.
     */
    fun detectFranchiseForGame(game: Game, allGames: List<Game> = emptyList()): String? {
        val title = game.title.trim()
        if (title.isBlank()) return null

        // 1. Curated rules check
        for (rule in CURATED_RULES) {
            if (rule.pattern.containsMatchIn(title)) {
                return rule.canonicalName
            }
        }

        // 2. Algorithmic candidate extraction
        val candidateRoot = extractAlgorithmicRoot(title) ?: return null

        // If candidateRoot is valid, verify confidence:
        // A candidate is only accepted if at least 2 DIFFERENT games in the user's library share that root.
        if (allGames.isNotEmpty()) {
            val normalizedCandidate = candidateRoot.lowercase()
            val matchingGames = allGames.filter { g ->
                val otherRoot = extractAlgorithmicRoot(g.title)?.lowercase()
                otherRoot == normalizedCandidate
            }
            if (countUniqueGames(matchingGames) >= 2) {
                return candidateRoot
            }
        }

        return null
    }

    /**
     * Normalizes a game title into a canonical release identity to detect whether
     * multiple library entries are copies/versions of the same game or genuinely different games.
     *
     * Strips:
     * - Parenthetical / bracketed notes: e.g. "(PS2)", "(GameCube)", "[PC]", "(USA)"
     * - Trailing platform indicators: e.g. "— PS2", " - GameCube", " - PC", "/ PS3"
     * - Remaster/edition suffixes: e.g. "Remastered", "Definitive Edition", "HD Remaster", "HD", "GOTY"
     * - Roman numerals converted to digits: e.g. "II" -> "2", "VII" -> "7"
     */
    fun normalizeGameIdentity(rawTitle: String): String {
        var title = rawTitle.trim()
        if (title.isBlank()) return ""

        // 1. Remove parenthetical or bracketed notes (e.g. "(PS2)", "(GameCube)", "[PC]", "(USA)")
        title = title.replace(Regex("""(?i)\s*[\(\[][^\)\]]*[\)\]]"""), " ")

        // 2. Remove trailing platform suffixes separated by dashes, em-dashes, en-dashes, or slashes
        title = title.replace(
            Regex(
                """(?i)\s*[-–—/]\s*(PlayStation(\s*[1-5])?|PS[1-5]|PC|Xbox(\s*(360|One|Series\s*[XS]))?|GameCube|GC|Nintendo\s*Switch|Switch|Wii(\s*U)?|Nintendo\s*64|N64|Super\s*Nintendo|SNES|NES|GBA|GBC|GB|NDS|3DS|PSP|PS\s*Vita|Vita|Sega\s*Genesis|Genesis|Dreamcast)\s*$"""
            ),
            ""
        )

        // 3. Remove edition / remaster / re-release tags
        title = title.replace(
            Regex(
                """(?i)\b(remastered?|hd\s*remaster|hd|definitive(\s*edition)?|special\s*edition|goty(\s*edition)?|game\s*of\s*the\s*year(\s*edition)?|anniversary(\s*edition)?|director'?s\s*cut|enhanced(\s*edition)?|complete\s*edition|deluxe(\s*edition)?|collector'?s\s*edition|standard\s*edition|original\s*game|legacy\s*edition)\b"""
            ),
            ""
        )

        // 4. Remove punctuation and non-alphanumeric characters
        title = title.replace(Regex("""[^a-zA-Z0-9\s]"""), " ")

        // 5. Convert roman numerals to arabic digits at word boundaries
        val romanMap = mapOf(
            "xvi" to "16", "xv" to "15", "xiv" to "14", "xiii" to "13", "xii" to "12",
            "xi" to "11", "x" to "10", "ix" to "9", "viii" to "8", "vii" to "7",
            "vi" to "6", "v" to "5", "iv" to "4", "iii" to "3", "ii" to "2"
        )
        val words = title.lowercase().split(Regex("""\s+""")).filter { it.isNotBlank() }
        val mappedWords = words.map { romanMap[it] ?: it }

        return mappedWords.joinToString(" ").trim()
    }

    /**
     * Counts how many unique, distinct games are present in a list of game library entries.
     * Multiple platforms or duplicate re-releases of the same game (e.g. RE4 on PS2 and GameCube)
     * count as only ONE unique game.
     */
    fun countUniqueGames(games: List<Game>): Int {
        if (games.isEmpty()) return 0
        return games
            .map { normalizeGameIdentity(it.title) }
            .filter { it.isNotBlank() }
            .distinct()
            .size
    }

    /**
     * Algorithmic extractor for title roots.
     * Strips subtitles, trailing Roman/Arabic numbers, and edition tags.
     */
    fun extractAlgorithmicRoot(rawTitle: String): String? {
        val cleaned = rawTitle.replace(EDITION_CLEANER, "").trim()
        if (cleaned.length < 3) return null

        // Check for subtitle separator (: or - or —)
        val candidate = if (cleaned.contains(":") || cleaned.contains(" - ") || cleaned.contains(" — ")) {
            val prefix = cleaned.split(Regex("""[:—]|\s+-\s+"""))[0].trim()
            prefix.replace(NUMBER_TRIMMER, "").trim()
        } else {
            cleaned.replace(NUMBER_TRIMMER, "").trim()
        }

        val normalized = candidate.lowercase()
        if (normalized.length < 3) return null
        if (INVALID_FRANCHISE_ROOTS.contains(normalized)) return null

        return candidate
    }

    /**
     * Groups all games in the user's library into cross-platform GameFranchise collections.
     *
     * CORE RULE:
     * A single game must NEVER create a franchise.
     * Only creates and displays a franchise when the library contains AT LEAST 2 DIFFERENT GAMES.
     * Different platforms, copies, or editions of the same game count as ONE unique game.
     */
    fun detectFranchises(allGames: List<Game>): List<GameFranchise> {
        if (allGames.isEmpty()) return emptyList()

        // 1. Group games by detected franchise
        val franchiseMap = mutableMapOf<String, MutableList<Game>>()

        for (game in allGames) {
            val franchiseName = detectFranchiseForGame(game, allGames)
            if (franchiseName != null) {
                franchiseMap.getOrPut(franchiseName) { mutableListOf() }.add(game)
            }
        }

        // 2. Filter franchises to ONLY those with AT LEAST 2 DIFFERENT/UNIQUE games.
        // A single game must NEVER create a franchise.
        // Multiple platforms or versions of the same game count as ONE game.
        val eligibleFranchises = franchiseMap.filter { (_, games) ->
            countUniqueGames(games) >= 2
        }

        // 3. Build GameFranchise objects
        return eligibleFranchises.map { (name, games) ->
            // Sort games chronologically by release year, then title
            val sortedGames = games.sortedWith(
                compareBy<Game> { if (it.releaseYear > 0) it.releaseYear else 9999 }
                    .thenBy { it.title }
            )

            // Select best representative banner:
            // Prefer any game that has an explicit high-res banner art URL, else fallback to top cover
            val bestBanner = sortedGames.firstOrNull { it.bannerArtUrl.isNotBlank() }?.bannerArtUrl.orEmpty()
            val bestCover = sortedGames.firstOrNull { it.coverArtUrl.isNotBlank() }?.coverArtUrl.orEmpty()

            GameFranchise(
                name = name,
                games = sortedGames,
                bannerUrl = bestBanner,
                coverUrl = bestCover
            )
        }.sortedWith(
            // Franchises sorted by number of games descending, then alphabetically by name
            compareByDescending<GameFranchise> { it.gameCount }
                .thenBy { it.name.lowercase() }
        )
    }

    /**
     * Filters games in a franchise by platform and optional search text.
     */
    fun filterFranchiseGames(
        games: List<Game>,
        selectedConsoleId: String = "ALL",
        searchQuery: String = ""
    ): List<Game> {
        return games.filter { game ->
            val matchesConsole = selectedConsoleId.equals("ALL", ignoreCase = true) ||
                    game.consoleId.equals(selectedConsoleId, ignoreCase = true)
            val matchesQuery = searchQuery.isBlank() ||
                    game.title.contains(searchQuery, ignoreCase = true) ||
                    game.console.displayName.contains(searchQuery, ignoreCase = true) ||
                    game.genre.contains(searchQuery, ignoreCase = true)
            matchesConsole && matchesQuery
        }
    }
}
