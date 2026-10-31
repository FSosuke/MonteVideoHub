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

        parseItems(
            Http.json(Request.Builder().url(url).nicoApiHeaders().build())
                .getJSONObject("data")
                .optJSONArray("items")
        )
    }

    /** Ranking der letzten 24 h, z. B. genre = "music_sound" oder "all". */
    suspend fun ranking(genre: String = "music_sound"): List<Track> = withContext(Dispatchers.IO) {
        val url = "https://nvapi.nicovideo.jp/v1/ranking/genre/$genre".toHttpUrl().newBuilder()
            .addQueryParameter("term", "24h")
            .build()
        parseItems(
            Http.json(Request.Builder().url(url).nicoApiHeaders().build())
                .getJSONObject("data")
                .optJSONArray("items")
        )
    }

    /** Neueste Videos eines Nutzers (für Abos). */
    suspend fun userVideos(userId: String): List<Track> = withContext(Dispatchers.IO) {
        val urls = listOf(
            "https://nvapi.nicovideo.jp/v3/users/$userId/videos",
            "https://nvapi.nicovideo.jp/v1/users/$userId/videos",
        )
        var last: Exception? = null
        for (base in urls) {
            try {
                val url = base.toHttpUrl().newBuilder()
                    .addQueryParameter("sortKey", "registeredAt")
                    .addQueryParameter("sortOrder", "desc")
                    .addQueryParameter("pageSize", "20")
                    .addQueryParameter("page", "1")
                    .build()
                val items = Http.json(Request.Builder().url(url).nicoApiHeaders().build())
                    .getJSONObject("data").optJSONArray("items")
                return@withContext parseItems(items)
            } catch (e: Exception) {
                last = e
            }
        }
        throw last ?: IOException("Unbekannter Fehler")
    }

    /** Gemeinsames Format von Suche, Ranking und Nutzer-Videos (ggf. in "essential" verpackt). */
    private fun parseItems(items: JSONArray?): List<Track> =
        items.objects()
            .map { it.optJSONObject("essential") ?: it }
            .filter { !it.optBoolean("isPaymentRequired") && it.has("id") }
            .map { o ->
                val owner = o.optJSONObject("owner")
                val thumb = o.optJSONObject("thumbnail")
                Track(
                    source = Source.NICONICO,
                    id = o.getString("id"),
                    title = o.optString("title"),
                    artist = owner?.optString("name")?.takeIf { it.isNotBlank() && it != "null" } ?: "niconico",
                    durationSec = o.optLong("duration", -1),
                    thumbnail = listOf("largeUrl", "listingUrl", "url")
                        .firstNotNullOfOrNull { k -> thumb?.optString(k)?.takeIf { it.isNotBlank() && it != "null" } },
                    views = o.optJSONObject("count")?.optLong("view", -1) ?: -1,
                    channelId = owner?.takeIf { it.optString("ownerType") == "user" }?.optString("id")
                        ?.takeIf { it.isNotBlank() && it != "null" },
                    channelAvatar = owner?.optString("iconUrl")?.takeIf { it.isNotBlank() && it != "null" },
                    uploadedAt = parseIsoDate(o.optString("registeredAt")),
                )
            }

    suspend fun resolve(id: String): ResolvedStream = withContext(Dispatchers.IO) {
        val trackId = randomTrackId()
        val api = watchData(id, trackId)

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

    /**
     * Watch-Daten: angemeldet über /api/watch/v3 (Login-Videos, Premium-Qualität),
     * sonst bzw. als Fallback über die Gast-API.
     * Hinweis: "/watch/<id>?responseType=json" antwortet inzwischen mit HTTP 400.
     */
    private fun watchData(id: String, trackId: String): JSONObject {
        fun load(path: String) = Http.json(
            Request.Builder()
                .url(
                    "https://www.nicovideo.jp/api/watch/$path/$id".toHttpUrl().newBuilder()
                        .addQueryParameter("_frontendId", "6")
                        .addQueryParameter("_frontendVersion", "0")
                        .addQueryParameter("actionTrackId", trackId)
                        .build()
                )
                .nicoApiHeaders()
                .build()
        ).getJSONObject("data")

        if (NicoAccount.isLoggedIn) {
            runCatching { return load("v3") }
        }
        return load("v3_guest")
    }

    private fun randomTrackId(): String {
        val chars = ('a'..'z') + ('A'..'Z') + ('0'..'9')
        val prefix = (1..10).map { chars.random() }.joinToString("")
        return "${prefix}_${System.currentTimeMillis()}"
    }
}

internal fun JSONArray?.objects(): List<JSONObject> =
    if (this == null) emptyList() else (0 until length()).mapNotNull { optJSONObject(it) }

/** ISO-8601 („2026-10-30T19:32:08+09:00“) → Epoch-ms, -1 bei Fehler. */
internal fun parseIsoDate(value: String?): Long =
    runCatching { java.time.OffsetDateTime.parse(value).toInstant().toEpochMilli() }.getOrDefault(-1L)
