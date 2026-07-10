package com.romexplorer.app.emulator

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

/**
 * Same idea as iiSU/ES-DE/Daijishou's "core" handoff: we don't ship any
 * emulation code ourselves, we just hand the content:// Uri to whichever
 * emulator the user already has installed, using each emulator's own launch
 * contract. Extend EMULATORS to add more.
 */
data class EmulatorTarget(
    val label: String,
    val packageName: String,
    val activityName: String? = null,
    val action: String = Intent.ACTION_VIEW,
    val extraKey: String? = null // some emulators want the path in an extra instead of data Uri
)

object EmulatorLauncher {

    private val EMULATORS: Map<String, List<EmulatorTarget>> = mapOf(
        "NES" to listOf(retroArch()),
        "SNES" to listOf(retroArch()),
        "GB" to listOf(retroArch()),
        "GBC" to listOf(retroArch()),
        "GBA" to listOf(retroArch(), EmulatorTarget("MyBoy!", "com.fastemulator.gba")),
        "N64" to listOf(retroArch(), EmulatorTarget("Mupen64Plus FZ", "com.mupen64plusae.v3.fzurita")),
        "Genesis" to listOf(retroArch()),
        "Master System" to listOf(retroArch()),
        "Game Gear" to listOf(retroArch()),
        "NDS" to listOf(EmulatorTarget("DraStic", "com.dsemu.drastic"), retroArch()),
        "3DS" to listOf(EmulatorTarget("Citra", "org.citra.citra_emu")),
        "PS1" to listOf(
            EmulatorTarget("DuckStation", "com.github.stenzek.duckstation"),
            retroArch()
        ),
        "PS2" to listOf(EmulatorTarget("AetherSX2 / NetherSX2", "xyz.aethersx2.android")),
        "PSP" to listOf(EmulatorTarget("PPSSPP", "org.ppsspp.ppsspp")),
        "GameCube" to listOf(EmulatorTarget("Dolphin", "org.dolphinemu.dolphinemu")),
        "Wii" to listOf(EmulatorTarget("Dolphin", "org.dolphinemu.dolphinemu")),
        "Dreamcast" to listOf(EmulatorTarget("Redream", "io.recompiled.redream"))
    )

    private fun retroArch() = EmulatorTarget("RetroArch", "com.retroarch")

    fun availableTargets(context: Context, system: String): List<EmulatorTarget> {
        val pm = context.packageManager
        return (EMULATORS[system] ?: emptyList()).filter { target ->
            try {
                pm.getPackageInfo(target.packageName, 0)
                true
            } catch (e: Exception) {
                false
            }
        }
    }

    /** Launches the first installed emulator that supports [system] with [romUri]. */
    fun launch(context: Context, system: String, romUri: Uri, title: String) {
        val target = availableTargets(context, system).firstOrNull()
        if (target == null) {
            Toast.makeText(
                context,
                "No installed emulator found for $system. Install RetroArch or a system-specific emulator.",
                Toast.LENGTH_LONG
            ).show()
            return
        }
        try {
            val intent = Intent(target.action).apply {
                setDataAndType(romUri, "application/octet-stream")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                setPackage(target.packageName)
                putExtra("com.romexplorer.app.GAME_TITLE", title)
            }
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "${target.label} couldn't open this file directly.", Toast.LENGTH_LONG).show()
        }
    }
}
