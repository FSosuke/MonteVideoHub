package com.fsosuke.montevideohub.playback

import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import com.fsosuke.montevideohub.data.Source
import com.fsosuke.montevideohub.data.SponsorBlock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** Springt bei YouTube-Titeln automatisch über SponsorBlock-Segmente. */
class SponsorSkipper(
    private val player: Player,
    private val scope: CoroutineScope,
) : Player.Listener {

    private var segments: List<SponsorBlock.Segment> = emptyList()
    private var currentId: String? = null
    private var loadJob: Job? = null

    init {
        player.addListener(this)
        scope.launch {
            while (isActive) {
                check()
                delay(400)
            }
        }
    }

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) = load(mediaItem)

    private fun load(item: MediaItem?) {
        segments = emptyList()
        loadJob?.cancel()
        val mediaId = item?.mediaId
        currentId = mediaId
        val prefix = "${Source.YOUTUBE.name}:"
        if (mediaId == null || !mediaId.startsWith(prefix)) return
        loadJob = scope.launch {
            val result = SponsorBlock.segments(mediaId.removePrefix(prefix))
            if (currentId == mediaId) segments = result
        }
    }

    private fun check() {
        if (!player.isPlaying || segments.isEmpty()) return
        val pos = player.currentPosition
        segments.firstOrNull { pos >= it.startMs && pos < it.endMs - 500 }?.let {
            player.seekTo(it.endMs)
        }
    }
}
