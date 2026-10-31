package com.fsosuke.montevideohub.playback

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.fsosuke.montevideohub.data.Source
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class QueueEntry(
    val mediaId: String,
    val title: String,
    val artist: String,
    val artwork: String?,
    val source: Source?,
)

/** Für Downloads aus dem Player: Warteschlangen-Eintrag zurück in einen Track wandeln. */
fun QueueEntry.toTrack(): com.fsosuke.montevideohub.data.Track? {
    val src = source ?: return null
    return com.fsosuke.montevideohub.data.Track(
        source = src,
        id = mediaId.substringAfter(':'),
        title = title,
        artist = artist,
        durationSec = -1,
        thumbnail = artwork,
        views = -1,
    )
}

data class PlayerUiState(
    val connected: Boolean = false,
    val current: QueueEntry? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    val queue: List<QueueEntry> = emptyList(),
    val currentIndex: Int = -1,
    val shuffle: Boolean = false,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
    /** Video-Modus aktiv (sonst nur Ton). */
    val videoMode: Boolean = true,
    /** Aktueller Titel hat eine Videospur. */
    val hasVideo: Boolean = false,
)

/** Verbindet die UI per MediaController mit dem PlaybackService. */
class PlayerConnection(context: Context, scope: CoroutineScope) {

    private val appContext = context.applicationContext
    private val future = MediaController.Builder(
        appContext,
        SessionToken(appContext, ComponentName(appContext, PlaybackService::class.java)),
    ).buildAsync()

    private var controller: MediaController? = null

    private val _player = MutableStateFlow<Player?>(null)
    /** Für die Videoanzeige (PlayerView). */
    val player: StateFlow<Player?> = _player.asStateFlow()

    private val _state = MutableStateFlow(PlayerUiState())
    val state: StateFlow<PlayerUiState> = _state.asStateFlow()

    private val _errors = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val errors: SharedFlow<String> = _errors.asSharedFlow()

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) = refresh()

        override fun onPlayerError(error: PlaybackException) {
            _errors.tryEmit("Wiedergabefehler: ${error.errorCodeName}")
        }
    }

    init {
        scope.launch {
            val c = try {
                future.await()
            } catch (e: Exception) {
                _errors.tryEmit("Player konnte nicht gestartet werden")
                return@launch
            }
            controller = c
            _player.value = c
            c.addListener(listener)
            refresh()
            while (isActive) {
                if (c.isPlaying) {
                    _state.update {
                        it.copy(positionMs = c.currentPosition, durationMs = c.safeDuration())
                    }
                }
                delay(500)
            }
        }
    }

    suspend fun await(): MediaController = future.await()

    fun use(block: (MediaController) -> Unit) {
        controller?.let(block)
    }

    private fun refresh() {
        val c = controller ?: return
        _state.value = PlayerUiState(
            connected = true,
            current = c.currentMediaItem?.toEntry(),
            isPlaying = c.isPlaying,
            isBuffering = c.playbackState == Player.STATE_BUFFERING,
            positionMs = c.currentPosition,
            durationMs = c.safeDuration(),
            queue = (0 until c.mediaItemCount).map { c.getMediaItemAt(it).toEntry() },
            currentIndex = c.currentMediaItemIndex,
            shuffle = c.shuffleModeEnabled,
            repeatMode = c.repeatMode,
            videoMode = C.TRACK_TYPE_VIDEO !in c.trackSelectionParameters.disabledTrackTypes,
            hasVideo = c.currentTracks.containsType(C.TRACK_TYPE_VIDEO),
        )
    }

    fun release() {
        controller?.removeListener(listener)
        _player.value = null
        MediaController.releaseFuture(future)
    }

    private fun Player.safeDuration(): Long =
        duration.takeIf { it != C.TIME_UNSET && it > 0 } ?: 0L

    private fun MediaItem.toEntry() = QueueEntry(
        mediaId = mediaId,
        title = mediaMetadata.title?.toString().orEmpty(),
        artist = mediaMetadata.artist?.toString().orEmpty(),
        artwork = mediaMetadata.artworkUri?.toString(),
        source = Source.entries.firstOrNull { mediaId.startsWith("${it.name}:") },
    )
}
