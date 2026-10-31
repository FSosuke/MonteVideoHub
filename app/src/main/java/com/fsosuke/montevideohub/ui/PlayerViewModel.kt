package com.fsosuke.montevideohub.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import android.net.Uri
import androidx.media3.common.MediaMetadata
import com.fsosuke.montevideohub.data.History
import com.fsosuke.montevideohub.data.Settings
import com.fsosuke.montevideohub.download.DownloadEntry
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import com.fsosuke.montevideohub.data.SearchRepository
import com.fsosuke.montevideohub.data.Track
import com.fsosuke.montevideohub.playback.MediaItems
import com.fsosuke.montevideohub.playback.PlayerConnection
import com.fsosuke.montevideohub.playback.PlayerUiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PlayerViewModel(app: Application) : AndroidViewModel(app) {

    private val connection = PlayerConnection(app, viewModelScope)
    val state: StateFlow<PlayerUiState> = connection.state
    val player: StateFlow<Player?> = connection.player

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    /** Keys der Titel, deren Stream gerade aufgelöst wird (für Ladeanzeigen). */
    private val _resolving = MutableStateFlow<Set<String>>(emptySet())
    val resolving: StateFlow<Set<String>> = _resolving.asStateFlow()

    init {
        viewModelScope.launch { connection.errors.collect { _messages.emit(it) } }
        // Einstellungen (Video-Modus, max. Qualität) live auf den Player anwenden
        viewModelScope.launch {
            Settings.state.map { it.videoMode to it.maxVideoHeight }.distinctUntilChanged().collect { (video, height) ->
                val c = connection.await()
                c.trackSelectionParameters = c.trackSelectionParameters.buildUpon()
                    .setTrackTypeDisabled(C.TRACK_TYPE_VIDEO, !video)
                    .setMaxVideoSize(Int.MAX_VALUE, height)
                    .build()
            }
        }
    }

    /** Heruntergeladene Datei abspielen (offline). */
    fun playLocal(entry: DownloadEntry) {
        viewModelScope.launch {
            val c = connection.await()
            val uri = Uri.parse(entry.uri)
            val item = MediaItem.Builder()
                .setMediaId("LOCAL:${entry.id}")
                .setUri(uri)
                .setRequestMetadata(MediaItem.RequestMetadata.Builder().setMediaUri(uri).build())
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(entry.track.title)
                        .setArtist(entry.track.artist)
                        .setArtworkUri(entry.track.thumbnail?.let(Uri::parse))
                        .build()
                )
                .build()
            val index = if (c.mediaItemCount == 0) 0 else c.currentMediaItemIndex + 1
            c.addMediaItem(index, item)
            c.seekToDefaultPosition(index)
            c.start()
        }
    }

    /** Sofort abspielen – die restliche Warteschlange bleibt erhalten. */
    fun play(track: Track) = resolveThen(track) { c, item ->
        val index = if (c.mediaItemCount == 0) 0 else c.currentMediaItemIndex + 1
        c.addMediaItem(index, item)
        c.seekToDefaultPosition(index)
        c.start()
    }

    fun playNext(track: Track) = resolveThen(track) { c, item ->
        val index = if (c.mediaItemCount == 0) 0 else c.currentMediaItemIndex + 1
        c.addMediaItem(index, item)
        if (c.mediaItemCount == 1) c.start()
        _messages.tryEmit("Als Nächstes: ${track.title}")
    }

    fun enqueue(track: Track) = resolveThen(track) { c, item ->
        c.addMediaItem(item)
        if (c.mediaItemCount == 1) c.start()
        _messages.tryEmit("Zur Warteschlange hinzugefügt")
    }

    fun togglePlayPause() = connection.use { c ->
        when {
            c.isPlaying -> c.pause()
            c.playbackState == Player.STATE_ENDED -> {
                c.seekToDefaultPosition(0)
                c.play()
            }
            else -> c.start()
        }
    }

    fun next() = connection.use { it.seekToNext() }
    fun previous() = connection.use { it.seekToPrevious() }
    fun seekTo(positionMs: Long) = connection.use { it.seekTo(positionMs) }
    fun skipTo(index: Int) = connection.use { it.seekToDefaultPosition(index); it.start() }
    fun remove(index: Int) = connection.use { it.removeMediaItem(index) }
    fun toggleShuffle() = connection.use { it.shuffleModeEnabled = !it.shuffleModeEnabled }

    fun setVideoMode(enabled: Boolean) = Settings.update { it.copy(videoMode = enabled) }

    fun cycleRepeat() = connection.use {
        it.repeatMode = when (it.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
    }

    private fun MediaController.start() {
        if (playbackState == Player.STATE_IDLE) prepare()
        play()
    }

    private fun resolveThen(track: Track, action: (MediaController, MediaItem) -> Unit) {
        if (track.key in _resolving.value) return
        viewModelScope.launch {
            _resolving.update { it + track.key }
            try {
                val stream = SearchRepository.resolve(track)
                History.add(track)
                action(connection.await(), MediaItems.from(track, stream))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _messages.emit("„${track.title}“ konnte nicht geladen werden: ${e.message ?: e.javaClass.simpleName}")
            } finally {
                _resolving.update { it - track.key }
            }
        }
    }

    override fun onCleared() {
        connection.release()
    }
}
