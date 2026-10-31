package com.fsosuke.montevideohub.playback

import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.exoplayer.drm.DrmSessionManagerProvider
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.MergingMediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy

/**
 * Wie DefaultMediaSourceFactory, aber: Hat ein Titel einen separaten Videostream
 * (YouTube), werden Ton und Bild zu einer Quelle zusammengeführt.
 * Der Ton steht an erster Stelle, damit mediaId und Metadaten erhalten bleiben.
 */
@UnstableApi
class MonteMediaSourceFactory(dataSourceFactory: DataSource.Factory) : MediaSource.Factory {
    private val default = DefaultMediaSourceFactory(dataSourceFactory)
    private val progressive = ProgressiveMediaSource.Factory(dataSourceFactory)

    override fun setDrmSessionManagerProvider(provider: DrmSessionManagerProvider): MediaSource.Factory = apply {
        default.setDrmSessionManagerProvider(provider)
        progressive.setDrmSessionManagerProvider(provider)
    }

    override fun setLoadErrorHandlingPolicy(policy: LoadErrorHandlingPolicy): MediaSource.Factory = apply {
        default.setLoadErrorHandlingPolicy(policy)
        progressive.setLoadErrorHandlingPolicy(policy)
    }

    override fun getSupportedTypes(): IntArray = default.supportedTypes

    override fun createMediaSource(mediaItem: MediaItem): MediaSource {
        val audio = default.createMediaSource(mediaItem)
        val videoUri = mediaItem.requestMetadata.extras?.getString(MediaItems.EXTRA_VIDEO_URI)
            ?: return audio
        val video = progressive.createMediaSource(MediaItem.fromUri(videoUri))
        return MergingMediaSource(
            /* adjustPeriodTimeOffsets = */ false,
            /* clipDurations = */ true,
            audio,
            video,
        )
    }
}
