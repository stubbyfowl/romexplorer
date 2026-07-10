package com.romexplorer.app.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup

/**
 * RetroExplore doesn't publish a public JSON API, so this does the same thing
 * your browser does: load the search results page for a title, follow the
 * first matching /game/<id>-<slug> link, and pull genre / box art / summary
 * off the game page. It's a personal, on-demand, low-volume lookup (one
 * request per game you own, cached forever in Room afterwards) - the same
 * category of thing a browser extension or read-it-later app does.
 *
 * Because this depends on RetroExplore's current HTML structure, it's
 * inherently best-effort: if they redesign the site, update the selectors
 * below. Every call is defensive and simply returns null on failure so a
 * broken lookup never crashes a scan.
 */
class RetroExploreClient {

    private val http = OkHttpClient()
    private val base = "https://retroexplore.com"

    data class GameMeta(
        val gameId: String,
        val title: String,
        val system: String?,
        val genre: String?,
        val boxArtUrl: String?,
        val description: String?
    )

    suspend fun search(title: String): GameMeta? = withContext(Dispatchers.IO) {
        try {
            val query = java.net.URLEncoder.encode(title, "UTF-8")
            val searchDoc = Jsoup.connect("$base/browse?search=$query")
                .userAgent("Mozilla/5.0 (RomExplorer personal app)")
                .timeout(10_000)
                .get()

            val firstLink = searchDoc.select("a[href^=/game/]").firstOrNull() ?: return@withContext null
            val gamePath = firstLink.attr("href")
            val gameId = gamePath.removePrefix("/game/").substringBefore('-')

            fetchGame(gameId, gamePath)
        } catch (e: Exception) {
            null
        }
    }

    private fun fetchGame(gameId: String, gamePath: String): GameMeta? {
        return try {
            val doc = Jsoup.connect("$base$gamePath")
                .userAgent("Mozilla/5.0 (RomExplorer personal app)")
                .timeout(10_000)
                .get()

            val title = doc.selectFirst("h1")?.text()?.trim() ?: return null
            val boxArt = doc.selectFirst("meta[property=og:image]")?.attr("content")
            val description = doc.selectFirst("meta[name=description]")?.attr("content")

            // System/genre line usually renders as "PS1 2001 · SNK · Run and Gun" near the title.
            val metaLine = doc.select("p, span, div").firstOrNull {
                it.text().contains("·")
            }?.text()
            val parts = metaLine?.split("·")?.map { it.trim() }
            val system = parts?.getOrNull(0)?.split(" ")?.firstOrNull()
            val genre = parts?.lastOrNull()

            GameMeta(
                gameId = gameId,
                title = title,
                system = system,
                genre = genre,
                boxArtUrl = boxArt,
                description = description
            )
        } catch (e: Exception) {
            null
        }
    }
}
