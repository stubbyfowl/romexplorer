package com.romexplorer.app.network

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.security.MessageDigest

/**
 * Thin wrapper around the official RetroAchievements Web API.
 * https://api-docs.retroachievements.org/
 *
 * You need your own RA username + Web API key (Settings screen), same as
 * what you'd type into iiSU or RetroArch. Nothing here is scraped or
 * unofficial - it's the same public API those apps use.
 *
 * Hashing note: RA's canonical hashing rules differ per console (some strip
 * headers, multi-disc systems use cue/track-aware hashing, etc.). This MVP
 * implements the common case - MD5 of the raw file, with header-stripping for
 * systems where the header is well known and fixed-size (NES/iNES, Lynx).
 * Anything else (PS1/PS2/N64/Saturn/Dreamcast/PSP...) will usually still let
 * you browse metadata, but achievement hash-matching for those may need the
 * console-specific rules added to RaHasher below.
 */
class RetroAchievementsClient(
    private val context: Context,
    private val username: String,
    private val apiKey: String
) {
    private val http = OkHttpClient()
    private val base = "https://retroachievements.org/API"

    data class GameProgress(
        val gameId: Int,
        val title: String,
        val iconUrl: String,
        val achievementsTotal: Int,
        val achievementsUnlocked: Int
    )

    suspend fun lookupByHash(hash: String): Int? = withContext(Dispatchers.IO) {
        val url = "$base/API_GetGameID.php?y=$apiKey&m=$hash"
        val body = get(url) ?: return@withContext null
        val json = JSONObject(body)
        val id = json.optInt("GameID", 0)
        if (id > 0) id else null
    }

    suspend fun getProgress(gameId: Int): GameProgress? = withContext(Dispatchers.IO) {
        val url = "$base/API_GetGameInfoAndUserProgress.php?z=$username&y=$apiKey&g=$gameId&u=$username"
        val body = get(url) ?: return@withContext null
        val json = JSONObject(body)
        GameProgress(
            gameId = gameId,
            title = json.optString("Title"),
            iconUrl = "https://media.retroachievements.org" + json.optString("ImageIcon"),
            achievementsTotal = json.optJSONObject("Achievements")?.length() ?: json.optInt("NumAchievements"),
            achievementsUnlocked = json.optInt("NumAwardedToUser")
        )
    }

    private fun get(url: String): String? {
        val req = Request.Builder().url(url).get().build()
        http.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) return null
            return resp.body?.string()
        }
    }
}

object RaHasher {

    // Cartridge-based systems fit comfortably under this; large CD/DVD images
    // (PS1/PS2/Dreamcast/etc.) need RA's disc-aware hashing which isn't
    // implemented here, so we skip them rather than reading gigabytes into RAM.
    private const val MAX_HASH_BYTES = 64L * 1024 * 1024

    /** Returns lowercase hex MD5 suitable for API_GetGameID.php's `m` param, or null if unsupported/too large. */
    fun hash(context: Context, uri: Uri, system: String): String? {
        val size = context.contentResolver.openFileDescriptor(uri, "r")?.use { it.statSize } ?: return null
        if (size > MAX_HASH_BYTES) return null
        val stream = context.contentResolver.openInputStream(uri) ?: return null
        stream.use { input ->
            val bytes = input.readBytes()
            val payload = when (system) {
                "NES" -> stripInesHeaderIfPresent(bytes)
                else -> bytes
            }
            val md5 = MessageDigest.getInstance("MD5")
            val digest = md5.digest(payload)
            return digest.joinToString("") { "%02x".format(it) }
        }
    }

    private fun stripInesHeaderIfPresent(bytes: ByteArray): ByteArray {
        // iNES header: bytes 0-3 == "NES\u001A", header is fixed 16 bytes.
        if (bytes.size > 16 &&
            bytes[0] == 'N'.code.toByte() &&
            bytes[1] == 'E'.code.toByte() &&
            bytes[2] == 'S'.code.toByte() &&
            bytes[3] == 0x1A.toByte()
        ) {
            return bytes.copyOfRange(16, bytes.size)
        }
        return bytes
    }
}
