package com.fsosuke.montevideohub.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

/** „Zuletzt gespielt“ – lokal, max. 50 Einträge. */
object History {
    private const val MAX = 50
    private lateinit var prefs: SharedPreferences
    private val _items = MutableStateFlow<List<Track>>(emptyList())
    val items: StateFlow<List<Track>> = _items.asStateFlow()

    fun init(context: Context) {
        prefs = context.getSharedPreferences("history", Context.MODE_PRIVATE)
        _items.value = runCatching {
            JSONArray(prefs.getString("tracks", "[]")).objects().mapNotNull { it.toTrack() }
        }.getOrDefault(emptyList())
    }

    fun add(track: Track) {
        val list = (listOf(track) + _items.value.filter { it.key != track.key }).take(MAX)
        _items.value = list
        prefs.edit().putString("tracks", JSONArray(list.map { it.toJson() }).toString()).apply()
    }

    fun clear() {
        _items.value = emptyList()
        prefs.edit().remove("tracks").apply()
    }
}

fun Track.toJson(): JSONObject = JSONObject()
    .put("source", source.name)
    .put("id", id)
    .put("title", title)
    .put("artist", artist)
    .put("duration", durationSec)
    .put("thumbnail", thumbnail)
    .put("views", views)
    .put("channelId", channelId)
    .put("channelAvatar", channelAvatar)

fun JSONObject.toTrack(): Track? = runCatching {
    Track(
        source = Source.valueOf(getString("source")),
        id = getString("id"),
        title = optString("title"),
        artist = optString("artist"),
        durationSec = optLong("duration", -1),
        thumbnail = optString("thumbnail").takeIf { it.isNotBlank() && it != "null" },
        views = optLong("views", -1),
        channelId = optString("channelId").takeIf { it.isNotBlank() && it != "null" },
        channelAvatar = optString("channelAvatar").takeIf { it.isNotBlank() && it != "null" },
    )
}.getOrNull()
