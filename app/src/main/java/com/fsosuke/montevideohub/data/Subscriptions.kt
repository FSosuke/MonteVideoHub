package com.fsosuke.montevideohub.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/** Ein abonnierter Kanal (YouTube) bzw. Nutzer (niconico) – ganz ohne Konto, nur lokal gespeichert. */
data class Subscription(
    val source: Source,
    /** YouTube: UC…-Kanal-ID, niconico: Nutzer-ID */
    val id: String,
    val name: String,
    val avatar: String? = null,
    val subscribers: Long = -1,
) {
    val key: String get() = "${source.name}:$id"
}

object SubscriptionStore {
    private lateinit var prefs: SharedPreferences
    private val _items = MutableStateFlow<List<Subscription>>(emptyList())
    val items: StateFlow<List<Subscription>> = _items.asStateFlow()

    fun init(context: Context) {
        prefs = context.getSharedPreferences("subscriptions", Context.MODE_PRIVATE)
        _items.value = runCatching {
            JSONArray(prefs.getString("items", "[]")).objects().mapNotNull { o ->
                runCatching {
                    Subscription(
                        source = Source.valueOf(o.getString("source")),
                        id = o.getString("id"),
                        name = o.optString("name"),
                        avatar = o.optString("avatar").takeIf { it.isNotBlank() && it != "null" },
                    )
                }.getOrNull()
            }
        }.getOrDefault(emptyList())
    }

    fun isSubscribed(source: Source, id: String?) = id != null && _items.value.any { it.source == source && it.id == id }

    /** Fügt hinzu bzw. aktualisiert (Name/Avatar). Gibt die Anzahl neuer Abos zurück. */
    fun addAll(subs: List<Subscription>): Int {
        var added = 0
        _items.update { current ->
            val map = LinkedHashMap(current.associateBy { it.key })
            subs.forEach { s ->
                if (s.key !in map) added++
                map[s.key] = s.copy(avatar = s.avatar ?: map[s.key]?.avatar)
            }
            map.values.sortedBy { it.name.lowercase() }
        }
        save()
        return added
    }

    fun remove(sub: Subscription) {
        _items.update { list -> list.filterNot { it.key == sub.key } }
        save()
    }

    private fun save() {
        prefs.edit().putString("items", JSONArray(_items.value.map { s ->
            JSONObject().put("source", s.source.name).put("id", s.id).put("name", s.name).put("avatar", s.avatar)
        }).toString()).apply()
    }
}

/** Import aus Google Takeout (Abos.csv / subscriptions.csv) oder einem NewPipe-Export (JSON). */
object SubscriptionImport {
    private val csvLine = Regex("""^\s*(UC[A-Za-z0-9_-]{22})\s*,\s*([^,]*)\s*,\s*(.*)$""")
    private val channelUrl = Regex("""/channel/(UC[A-Za-z0-9_-]{22})""")

    fun parse(text: String): List<Subscription> {
        val trimmed = text.trimStart('﻿', ' ', '\n', '\r', '\t')
        return if (trimmed.startsWith("{")) parseNewPipe(trimmed) else parseCsv(trimmed)
    }

    private fun parseCsv(text: String): List<Subscription> =
        text.lines().mapNotNull { line ->
            val m = csvLine.find(line) ?: return@mapNotNull null
            val title = m.groupValues[3].trim().removeSurrounding("\"").replace("\"\"", "\"")
            Subscription(Source.YOUTUBE, m.groupValues[1], title.ifBlank { m.groupValues[1] })
        }.distinctBy { it.key }

    private fun parseNewPipe(text: String): List<Subscription> =
        JSONObject(text).optJSONArray("subscriptions").objects().mapNotNull { o ->
            if (o.optInt("service_id", 0) != 0) return@mapNotNull null // 0 = YouTube
            val id = channelUrl.find(o.optString("url"))?.groupValues?.get(1) ?: return@mapNotNull null
            Subscription(Source.YOUTUBE, id, o.optString("name").ifBlank { id })
        }.distinctBy { it.key }
}

data class FeedState(
    val loading: Boolean = false,
    val items: List<Track> = emptyList(),
    val updatedAt: Long = 0,
    val failed: Int = 0,
)

/** Feed „Neu von deinen Abos“: neueste Videos aller Abos, nach Datum sortiert. */
object FeedRepository {
    private val _state = MutableStateFlow(FeedState())
    val state: StateFlow<FeedState> = _state.asStateFlow()

    val isStale: Boolean get() = System.currentTimeMillis() - _state.value.updatedAt > 30 * 60 * 1000

    suspend fun refresh(subs: List<Subscription> = SubscriptionStore.items.value) {
        if (_state.value.loading) return
        if (subs.isEmpty()) {
            _state.value = FeedState(updatedAt = System.currentTimeMillis())
            return
        }
        _state.update { it.copy(loading = true) }
        val limit = Semaphore(6)
        val results = coroutineScope {
            subs.map { sub ->
                async(Dispatchers.IO) { limit.withPermit { runCatching { fetch(sub) } } }
            }.awaitAll()
        }
        val items = results.mapNotNull { it.getOrNull() }.flatten()
            .distinctBy { it.key }
            .sortedByDescending { it.uploadedAt }
        _state.value = FeedState(false, items, System.currentTimeMillis(), results.count { it.isFailure })
    }

    suspend fun fetch(sub: Subscription): List<Track> = withContext(Dispatchers.IO) {
        when (sub.source) {
            Source.YOUTUBE -> YouTubeSource.channelFeed(sub.id)
            Source.NICONICO -> NicoSource.userVideos(sub.id).map { it.copy(channelId = sub.id) }
        }.map { it.copy(channelAvatar = it.channelAvatar ?: sub.avatar) }
    }
}
