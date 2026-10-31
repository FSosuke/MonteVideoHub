package com.fsosuke.montevideohub.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import org.json.JSONArray

/**
 * SponsorBlock (wie in ReVanced): überspringt bei YouTube automatisch
 * Nicht-Musik-Teile in Musikvideos, Sponsoren und Eigenwerbung.
 */
object SponsorBlock {
    data class Segment(val startMs: Long, val endMs: Long)

    private const val CATEGORIES =
        """["music_offtopic","sponsor","selfpromo","interaction"]"""

    suspend fun segments(videoId: String): List<Segment> = withContext(Dispatchers.IO) {
        runCatching {
            val url = "https://sponsor.ajay.app/api/skipSegments".toHttpUrl().newBuilder()
                .addQueryParameter("videoID", videoId)
                .addQueryParameter("categories", CATEGORIES)
                .build()
            Http.client.newCall(Request.Builder().url(url).build()).execute().use { response ->
                if (!response.isSuccessful) return@use emptyList() // 404 = keine Segmente
                JSONArray(response.body?.string().orEmpty()).objects()
                    .filter { it.optString("actionType", "skip") == "skip" }
                    .mapNotNull { o ->
                        val seg = o.optJSONArray("segment") ?: return@mapNotNull null
                        Segment((seg.getDouble(0) * 1000).toLong(), (seg.getDouble(1) * 1000).toLong())
                    }
            }
        }.getOrDefault(emptyList())
    }
}
