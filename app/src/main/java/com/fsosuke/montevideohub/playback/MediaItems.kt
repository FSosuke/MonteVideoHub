package com.fsosuke.montevideohub.playback

import android.net.Uri
import androidx.core.os.bundleOf
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.fsosuke.montevideohub.data.ResolvedStream
import com.fsosuke.montevideohub.data.Track

object MediaItems {
    const val EXTRA_MIME = "mime"
    const val EXTRA_VIDEO_URI = "videoUri"
    const val EXTRA_SOURCE = "source"

    fun from(track: Track, stream: ResolvedStream): MediaItem {
        val uri = Uri.parse(stream.uri)
        return MediaItem.Builder()
            .setMediaId(track.key)
            .setUri(uri)
            .setMimeType(stream.mimeType)
            .setRequestMetadata(
                MediaItem.RequestMetadata.Builder()
                    .setMediaUri(uri)
                    .setExtras(bundleOf(EXTRA_MIME to stream.mimeType, EXTRA_VIDEO_URI to stream.videoUri))
                    .build()
            )
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(track.title)
                    .setArtist(track.artist)
                    .setArtworkUri(track.thumbnail?.let(Uri::parse))
                    .setExtras(bundleOf(EXTRA_SOURCE to track.source.name))
                    .build()
            )
            .build()
    }
}
