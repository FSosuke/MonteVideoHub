package com.fsosuke.montevideohub.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

/**
 * niconico:
 *  - Suche über nvapi (dieselbe API wie die Suchseite von nicovideo.jp)
 *  - Wiedergabe über die Gast-Watch-API v3 + "domand"-HLS-Freigabe (wie im Browser)
 */
object NicoSource {
    private const val SEARCH_URL = "https://nvapi.nicovideo.jp/v2/search/video"

    private fun Request.Builder.nicoApiHeaders() = apply {
        header("X-Frontend-Id", "6")
        header("X-Frontend-Version", "0")
        header("X-Niconico-Language", "ja-jp")
        header("Referer", "https://www.nicovideo.jp/")
        header("Origin", "https://www.nicovideo.jp")
    }

    /** Gleiche Suche wie auf nicovideo.jp (liefert auch Uploader-Namen). */
    suspend fun search(query: String, musicOnly: Boolean): List<Track> = withContext(Dispatchers.IO) {
        val url = SEARCH_URL.toHttpUrl().newBuilder()
            .addQueryParameter("keyword", query)
            .addQueryParameter("sortKey", "hot")
            .addQueryParameter("sortOrder", "none")
            .addQueryParameter("pageSize", "30")
            .addQueryParameter("page", "1")
            .apply { if (musicOnly) addQueryParameter("genres", "music_sound") }
            .build()

        val items = Http.json(Request.Builder().url(url).nicoApiHeaders().build())
            .getJSONObject("data")
            .optJSONArray("items")
        items.objects()
            .filter { !it.optBoolean("isPaymentRequired") }
            .map { o ->
                val thumb = o.optJSONObject("thumbnail")
                Track(
                    source = Source.NICONICO,
                    id = o.getString("id"),
                    title = o.optString("title"),
                    artist = o.optJSONObject("owner")?.optString("name")?.takeIf { it.isNotBlank() } ?: "niconico",
                    durationSec = o.optLong("duration", -1),
                    thumbnail = listOf("largeUrl", "listingUrl", "url")
                        .firstNotNullOfOrNull { k -> thumb?.optString(k)?.takeIf { it.isNotBlank() && it != "null" } },
                    views = o.optJSONObject("count")?.optLong("view", -1) ?: -1,
                )
            }
    }

    suspend fun resolve(id: String): ResolvedStream = withContext(Dispatchers.IO) {
        val trackId = randomTrackId()
        // Hinweis: "/watch/<id>?responseType=json" antwortet inzwischen mit HTTP 400.
        val watchUrl = "https://www.nicovideo.jp/api/watch/v3_guest/$id".toHttpUrl().newBuilder()
            .addQueryParameter("_frontendId", "6")
            .addQueryParameter("_frontendVersion", "0")
            .addQueryParameter("actionTrackId", trackId)
            .build()
        val api = Http.json(Request.Builder().url(watchUrl).nicoApiHeaders().build())
            .getJSONObject("data")

        val domand = api.optJSONObject("media")?.optJSONObject("domand")
            ?: throw IOException("Video nur mit niconico-Login bzw. Premium abspielbar")

        val audio = domand.optJSONArray("audios").objects()
            .filter { it.optBoolean("isAvailable") }
            .maxByOrNull { it.optInt("bitRate") }
            ?: throw IOException("Keine Audiospur verfügbar")
        // Alle verfügbaren Videoqualitäten anfordern – der Player wählt passend zur Verbindung.
        // Im Audio-Modus ist die Videospur deaktiviert und wird gar nicht geladen.
        val videos = domand.optJSONArray("videos").objects()
            .filter { it.optBoolean("isAvailable") }
            .ifEmpty { throw IOException("Keine Videospur verfügbar") }

        val accessKey = domand.getString("accessRightKey")
        val actionTrackId = api.optJSONObject("client")?.optString("watchTrackId")
            ?.takeIf { it.isNotBlank() } ?: trackId

        val body = JSONObject()
            .put("outputs", JSONArray().apply {
                videos.forEach { put(JSONArray().put(it.getString("id")).put(audio.getString("id"))) }
            })
            .toString()
            .toRequestBody("application/json".toMediaType())

        val hlsUrl = "https://nvapi.nicovideo.jp/v1/watch/$id/access-rights/hls".toHttpUrl()
            .newBuilder()
            .addQueryParameter("actionTrackId", actionTrackId)
            .build()
        val response = Http.json(
            Request.Builder()
                .url(hlsUrl)
                .post(body)
                .nicoApiHeaders()
                .header("X-Access-Right-Key", accessKey)
                .header("X-Request-With", "https://www.nicovideo.jp")
                .build()
        )
        // Die Antwort setzt das Cookie `domand_bid`, das Http.client automatisch weiterverwendet.
        val contentUrl = response.getJSONObject("data").getString("contentUrl")
        ResolvedStream(contentUrl, "application/x-mpegURL")
    }

    private fun randomTrackId(): String {
        val chars = ('a'..'z') + ('A'..'Z') + ('0'..'9')
        val prefix = (1..10).map { chars.random() }.joinToString("")
        return "${prefix}_${System.currentTimeMillis()}"
    }
}

internal fun JSONArray?.objects(): List<JSONObject> =
    if (this == null) emptyList() else (0 until length()).mapNotNull { optJSONObject(it) }
