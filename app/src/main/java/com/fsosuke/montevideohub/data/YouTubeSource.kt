package com.fsosuke.montevideohub.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.MediaFormat
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.services.youtube.linkHandler.YoutubeSearchQueryHandlerFactory
import org.schabi.newpipe.extractor.stream.DeliveryMethod
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import org.schabi.newpipe.extractor.stream.VideoStream
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
        extractor.initialPage.items.toTracks()
    }

    /**
     * Startseite: YouTube-Trends (Kiosk). Bevorzugt den Musik-Kiosk, falls vorhanden.
     * Liefert Titel des Abschnitts + Titel.
     */
    suspend fun home(): Pair<String, List<Track>> = withContext(Dispatchers.IO) {
        val kiosks = service.kioskList
        val ids = kiosks.availableKiosks.toList()
        val id = ids.firstOrNull { it.contains("music", ignoreCase = true) }
            ?: kiosks.defaultKioskId
        val extractor = kiosks.getExtractorById(id, null)
        extractor.fetchPage()
        val name = runCatching { extractor.name }.getOrNull()?.takeIf { it.isNotBlank() } ?: "Trends"
        name to extractor.initialPage.items.toTracks()
    }

    /** Verfügbare Kiosk-IDs (für Diagnose). */
    fun kioskIds(): List<String> = service.kioskList.availableKiosks.toList()

    private fun List<*>.toTracks(): List<Track> =
        filterIsInstance<StreamInfoItem>()
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

    /**
     * Bester Audiostream + passender Videostream (max. 1080p, Display des Xperia 5 V).
     * Keine Werbung – wir spielen die Streams direkt ab. Im Audio-Modus wird das Video nicht geladen.
     */
    suspend fun resolve(id: String): ResolvedStream = withContext(Dispatchers.IO) {
        val info = StreamInfo.getInfo(service, "https://www.youtube.com/watch?v=$id")
        val best = info.audioStreams
            .filter { it.isUrl && it.deliveryMethod == DeliveryMethod.PROGRESSIVE_HTTP }
            .maxByOrNull { it.averageBitrate }
        val video = info.videoOnlyStreams
            .filter { it.isUrl && it.deliveryMethod == DeliveryMethod.PROGRESSIVE_HTTP && it.height in 1..Settings.value.maxVideoHeight }
            .sortedWith(
                compareByDescending<VideoStream> { it.height }
                    .thenBy { codecRank(it.codec) }
                    .thenByDescending { it.fps }
            )
            .firstOrNull()
            ?: info.videoStreams
                .filter { it.isUrl && it.deliveryMethod == DeliveryMethod.PROGRESSIVE_HTTP }
                .maxByOrNull { it.height }
        when {
            best != null -> ResolvedStream(best.content, null, video?.content)
            !info.hlsUrl.isNullOrBlank() -> ResolvedStream(info.hlsUrl, "application/x-mpegURL")
            else -> throw IOException("Kein abspielbarer Audiostream gefunden")
        }
    }

    /** Streams für einen Download: M4A-Audio + (optional) H.264-Video, damit sie in MP4 passen. */
    data class DownloadStreams(val audioUrl: String, val videoUrl: String?, val muxedVideoUrl: String?)

    suspend fun downloadStreams(id: String, withVideo: Boolean, maxHeight: Int): DownloadStreams =
        withContext(Dispatchers.IO) {
            val info = StreamInfo.getInfo(service, "https://www.youtube.com/watch?v=$id")
            val progressive = info.audioStreams.filter { it.isUrl && it.deliveryMethod == DeliveryMethod.PROGRESSIVE_HTTP }
            val audio = progressive.filter { it.format == MediaFormat.M4A }.maxByOrNull { it.averageBitrate }
                ?: throw IOException("Kein M4A-Audiostream verfügbar")
            if (!withVideo) return@withContext DownloadStreams(audio.content, null, null)
            val video = info.videoOnlyStreams
                .filter {
                    it.isUrl && it.deliveryMethod == DeliveryMethod.PROGRESSIVE_HTTP &&
                        it.height in 1..maxHeight && it.codec?.startsWith("avc") == true
                }
                .maxByOrNull { it.height }
            val muxed = info.videoStreams
                .filter { it.isUrl && it.deliveryMethod == DeliveryMethod.PROGRESSIVE_HTTP && it.format == MediaFormat.MPEG_4 }
                .maxByOrNull { it.height }
            if (video == null && muxed == null) throw IOException("Kein MP4-Videostream verfügbar")
            DownloadStreams(audio.content, video?.content, muxed?.content)
        }

    /** H.264 und VP9 laufen überall in Hardware; AV1 nur als Ausweichlösung. */
    private fun codecRank(codec: String?): Int = when {
        codec == null -> 1
        codec.startsWith("avc") -> 0
        codec.startsWith("vp9") || codec.startsWith("vp09") -> 0
        codec.startsWith("av01") -> 2
        else -> 1
    }
}
