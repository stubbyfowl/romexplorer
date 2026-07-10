package com.romexplorer.app.scan

/**
 * Best-effort mapping from file extension (and, as a fallback, the containing
 * folder name) to a system label. Labels match the short codes RetroExplore
 * itself uses (NES, SNES, PS1, PS2, N64, GBA, GBC, GB, NDS, Genesis, GG,
 * Master System, Dreamcast, PSP, Neo Geo, ...) so filtering/sorting lines up
 * with retroexplore.com.
 */
object SystemDetector {

    private val extensionMap: Map<String, String> = mapOf(
        "nes" to "NES", "unif" to "NES", "fds" to "NES",
        "sfc" to "SNES", "smc" to "SNES", "swc" to "SNES", "fig" to "SNES",
        "gb" to "GB", "sgb" to "GB",
        "gbc" to "GBC",
        "gba" to "GBA", "srl" to "GBA",
        "nds" to "NDS", "dsi" to "NDS",
        "3ds" to "3DS", "cia" to "3DS",
        "n64" to "N64", "z64" to "N64", "v64" to "N64",
        "gcm" to "GameCube", "rvz" to "GameCube/Wii", "wbfs" to "Wii", "wad" to "Wii",
        "md" to "Genesis", "gen" to "Genesis", "smd" to "Genesis", "32x" to "32X",
        "sms" to "Master System",
        "gg" to "Game Gear",
        "pce" to "PC Engine",
        "ngp" to "Neo Geo Pocket", "ngc" to "Neo Geo Pocket Color",
        "iso" to "PS2/PSP/GC (by folder)", // disambiguated by folder below
        "bin" to "PS1", "cue" to "PS1", "chd" to "PS1/Saturn/Dreamcast/PC Engine CD",
        "pbp" to "PSP",
        "cso" to "PSP",
        "gdi" to "Dreamcast", "cdi" to "Dreamcast",
        "cci" to "3DS",
        "xci" to "Switch", "nsp" to "Switch"
    )

    // Used when the extension alone is ambiguous (iso, chd, bin).
    private val folderHints: List<Pair<Regex, String>> = listOf(
        Regex("ps2|playstation ?2", RegexOption.IGNORE_CASE) to "PS2",
        Regex("psp", RegexOption.IGNORE_CASE) to "PSP",
        Regex("gamecube|gcn", RegexOption.IGNORE_CASE) to "GameCube",
        Regex("saturn", RegexOption.IGNORE_CASE) to "Saturn",
        Regex("dreamcast|dc", RegexOption.IGNORE_CASE) to "Dreamcast",
        Regex("psx|ps1|playstation ?1", RegexOption.IGNORE_CASE) to "PS1",
        Regex("pce|pcengine|turbografx", RegexOption.IGNORE_CASE) to "PC Engine CD"
    )

    val knownExtensions: Set<String> = extensionMap.keys

    fun detect(fileName: String, parentFolderName: String?): String {
        val ext = fileName.substringAfterLast('.', "").lowercase()
        val base = extensionMap[ext] ?: "Unknown"
        if (base != "PS2/PSP/GC (by folder)" && base != "PS1/Saturn/Dreamcast/PC Engine CD") {
            return base
        }
        val hint = parentFolderName?.let { folder ->
            folderHints.firstOrNull { it.first.containsMatchIn(folder) }?.second
        }
        return hint ?: if (ext == "iso") "PS2" else "PS1"
    }

    /**
     * Strips common ROM-set noise like "(USA)", "(Rev 1)", "[!]", underscores, etc.
     * so the guessed title is closer to what RetroExplore lists.
     */
    fun cleanTitle(fileName: String): String {
        var name = fileName.substringBeforeLast('.')
        name = name.replace(Regex("""\(.*?\)"""), "")
        name = name.replace(Regex("""\[.*?]"""), "")
        name = name.replace('_', ' ').replace('.', ' ')
        name = name.replace(Regex("""\s+"""), " ").trim()
        return name
    }
}
