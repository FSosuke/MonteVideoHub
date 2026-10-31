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
 *  - Suche über die offizielle Snapshot-Such-API v2
 *  - Wiedergabe über die Watch-API + "domand"-HLS-Freigabe (wie im Browser)
 */
object NicoSource {
    private const val SEARCH_URL =
        "https://snapshot.search.nicovideo.jp/api/v2/snapshot/video/contents/search"

    private fun Request.Builder.nicoApiHeaders() = apply {
        header("X-Frontend-Id", "6")
        header("X-Frontend-Version", "0")
        header("X-Niconico-Language", "ja-jp")
        header("Referer", "https://www.nicovideo.jp/")
        header("Origin", "https://www.nicovideo.jp")
    }

    suspend fun search(query: String, musicOnly: Boolean): List<Track> = withContext(Dispatchers.IO) {
        val url = SEARCH_URL.toHttpUrl().newBuilder()
            .addQueryParameter("q", query)
            .addQueryParameter("targets", "title,tags,description")
            .addQueryParameter("fields", "contentId,title,thumbnailUrl,lengthSeconds,viewCounter")
            .addQueryParameter("_sort", "-viewCounter")
            .addQueryParameter("_limit", "30")
            .addQueryParameter("_context", "MonteVideoHub")
            .apply {
                if (musicOnly) {
                    addQueryParameter("filters[genre.keyword][0]", "音楽・サウンド")
                    addQueryParameter("filters[genre.keyword][1]", "音楽")
                }
            }
            .build()

        val data = Http.json(Request.Builder().url(url).build()).getJSONArray("data")
        data.objects().map { o ->
            Track(
                source = Source.NICONICO,
                id = o.getString("contentId"),
                title = o.optString("title"),
                artist = "niconico",
                durationSec = o.optLong("lengthSeconds", -1),
                thumbnail = o.optString("thumbnailUrl").takeIf { it.isNotBlank() },
                views = o.optLong("viewCounter", -1),
            )
        }
    }

    suspend fun resolve(id: String): ResolvedStream = withContext(Dispatchers.IO) {
        val trackId = randomTrackId()
        val watchUrl = "https://www.nicovideo.jp/watch/$id".toHttpUrl().newBuilder()
            .addQueryParameter("responseType", "json")
            .addQueryParameter("actionTrackId", trackId)
            .build()
        val api = Http.json(Request.Builder().url(watchUrl).nicoApiHeaders().build())
            .getJSONObject("data")
            .getJSONObject("response")

        val domand = api.optJSONObject("media")?.optJSONObject("domand")
            ?: throw IOException("Video nur mit niconico-Login bzw. Premium abspielbar")

        val audio = domand.optJSONArray("audios").objects()
            .filter { it.optBoolean("isAvailable") }
            .maxByOrNull { it.optInt("bitRate") }
            ?: throw IOException("Keine Audiospur verfügbar")
        // Für eine reine Musik-App die kleinste Videospur – sie wird im Player ohnehin deaktiviert.
        val video = domand.optJSONArray("videos").objects()
            .filter { it.optBoolean("isAvailable") }
            .minByOrNull { it.optInt("bitRate", Int.MAX_VALUE) }
            ?: throw IOException("Keine Videospur verfügbar")

        val accessKey = domand.getString("accessRightKey")
        val actionTrackId = api.optJSONObject("client")?.optString("watchTrackId")
            ?.takeIf { it.isNotBlank() } ?: trackId

        val body = JSONObject()
            .put("outputs", JSONArray().put(JSONArray().put(video.getString("id")).put(audio.getString("id"))))
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
