package com.fsosuke.montevideohub.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fsosuke.montevideohub.data.FeedRepository
import com.fsosuke.montevideohub.data.FeedState
import com.fsosuke.montevideohub.data.Source
import com.fsosuke.montevideohub.data.Subscription
import com.fsosuke.montevideohub.data.SubscriptionImport
import com.fsosuke.montevideohub.data.SubscriptionStore
import com.fsosuke.montevideohub.data.Track
import com.fsosuke.montevideohub.data.YouTubeSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ChannelSearchState(
    val query: String = "",
    val loading: Boolean = false,
    val results: List<Subscription> = emptyList(),
    val error: String? = null,
)

class SubscriptionsViewModel(app: Application) : AndroidViewModel(app) {
    val subscriptions: StateFlow<List<Subscription>> = SubscriptionStore.items
    val feed: StateFlow<FeedState> = FeedRepository.state

    /** Ausgewählter Kanal (Key) als Filter, null = alle */
    private val _selected = MutableStateFlow<String?>(null)
    val selected: StateFlow<String?> = _selected.asStateFlow()

    private val _search = MutableStateFlow(ChannelSearchState())
    val search: StateFlow<ChannelSearchState> = _search.asStateFlow()
    private var searchJob: Job? = null

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    init {
        if (FeedRepository.isStale) refresh()
    }

    fun refresh() {
        viewModelScope.launch { FeedRepository.refresh() }
    }

    fun select(key: String?) = _selected.update { if (it == key) null else key }

    /** Kanal eines Titels abonnieren bzw. Abo beenden. */
    fun toggle(track: Track) {
        val ref = track.channelId
        if (ref == null) {
            _messages.tryEmit("Kanal für diesen Titel unbekannt")
            return
        }
        SubscriptionStore.items.value.firstOrNull { it.source == track.source && it.id == ref }?.let {
            unsubscribe(it)
            return
        }
        viewModelScope.launch {
            try {
                val sub = when (track.source) {
                    Source.YOUTUBE -> runCatching { YouTubeSource.channel(ref) }.getOrElse { e ->
                        if (ref.startsWith("UC")) Subscription(Source.YOUTUBE, ref, track.artist, track.channelAvatar)
                        else throw e
                    }
                    Source.NICONICO -> Subscription(Source.NICONICO, ref, track.artist, track.channelAvatar)
                }
                subscribe(sub)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _messages.emit("Abonnieren fehlgeschlagen: ${e.message}")
            }
        }
    }

    fun subscribe(sub: Subscription) {
        SubscriptionStore.addAll(listOf(sub))
        _messages.tryEmit("„${sub.name}“ abonniert")
        refresh()
    }

    fun unsubscribe(sub: Subscription) {
        SubscriptionStore.remove(sub)
        if (_selected.value == sub.key) _selected.value = null
        _messages.tryEmit("Abo von „${sub.name}“ beendet")
        refresh()
    }

    fun onSearchQuery(q: String) = _search.update { it.copy(query = q) }

    fun searchChannels() {
        val q = _search.value.query.trim()
        if (q.isEmpty()) return
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _search.update { it.copy(loading = true, error = null) }
            val result = runCatching { YouTubeSource.searchChannels(q) }
            _search.update {
                it.copy(
                    loading = false,
                    results = result.getOrDefault(emptyList()),
                    error = result.exceptionOrNull()?.let { e -> e.message ?: e.javaClass.simpleName },
                )
            }
        }
    }

    /** Google Takeout (Abos.csv) oder NewPipe-Export (JSON) importieren. */
    fun importFrom(uri: Uri?) {
        uri ?: return
        viewModelScope.launch {
            try {
                val text = withContext(Dispatchers.IO) {
                    getApplication<Application>().contentResolver.openInputStream(uri)?.use {
                        it.readBytes().toString(Charsets.UTF_8)
                    }.orEmpty()
                }
                val subs = SubscriptionImport.parse(text)
                if (subs.isEmpty()) {
                    _messages.emit("Keine Abos in der Datei gefunden – bitte Abos.csv aus Google Takeout wählen")
                    return@launch
                }
                val added = SubscriptionStore.addAll(subs)
                _messages.emit("$added neue Abos importiert (${subs.size} in der Datei)")
                FeedRepository.refresh()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _messages.emit("Import fehlgeschlagen: ${e.message}")
            }
        }
    }
}
