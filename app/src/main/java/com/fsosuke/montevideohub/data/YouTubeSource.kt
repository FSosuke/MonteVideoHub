package com.fsosuke.montevideohub.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.services.youtube.linkHandler.YoutubeSearchQueryHandlerFactory
import org.schabi.newpipe.extractor.stream.DeliveryMethod
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import java.io.IOException

object YouTubeSource {
    private val service get() = ServiceList.YouTube

    /** musicOnly = YouTube-Music-Suche nach Songs, sonst normale Videosuche. */
    suspend fun search(query: String, musicOnly: Boolean): List<Track> = withContext(Dispatchers.IO) {
        val filter = if (musicOnly) {
            YoutubeSearchQueryHandlerFactory.MUSIC_SONGS
        } else {
            YoutubeSearchQueryHandlerFactory.VIDEOS
        }
        val extractor = service.getSearchExtractor(query, listOf(filter), "")
        extractor.fetchPage()
        extractor.initialPage.items
            .filterIsInstance<StreamInfoItem>()
            .mapNotNull { item ->
                val id = runCatching { service.streamLHFactory.getId(item.url) }.getOrNull()
                    ?: return@mapNotNull null
                Track(
                    source = Source.YOUTUBE,
                    id = id,
                    title = item.name.orEmpty(),
                    artist = item.uploaderName.orEmpty().removeSuffix(" - Topic"),
                    durationSec = item.duration,
                    thumbnail = item.thumbnails.maxByOrNull { it.height }?.url,
                    views = item.viewCount,
                )
            }
    }

    /** Liefert den besten reinen Audiostream (keine Werbung – wir spielen direkt ab). */
    suspend fun resolve(id: String): ResolvedStream = withContext(Dispatchers.IO) {
        val info = StreamInfo.getInfo(service, "https://www.youtube.com/watch?v=$id")
        val best = info.audioStreams
            .filter { it.isUrl && it.deliveryMethod == DeliveryMethod.PROGRESSIVE_HTTP }
            .maxByOrNull { it.averageBitrate }
        when {
            best != null -> ResolvedStream(best.content, null)
            !info.hlsUrl.isNullOrBlank() -> ResolvedStream(info.hlsUrl, "application/x-mpegURL")
            else -> throw IOException("Kein abspielbarer Audiostream gefunden")
        }
    }
}
