package com.fsosuke.montevideohub.data

enum class Source(val label: String) {
    YOUTUBE("YouTube"),
    NICONICO("niconico"),
}

enum class SourceFilter(val label: String) {
    ALL("Alle"),
    YOUTUBE("YouTube"),
    NICONICO("niconico"),
}

data class Track(
    val source: Source,
    /** YouTube-Video-ID bzw. niconico-ID (z. B. sm9). */
    val id: String,
    val title: String,
    val artist: String,
    val durationSec: Long,
    val thumbnail: String?,
    val views: Long,
) {
    /** Eindeutiger Schlüssel, wird auch als Media3-mediaId verwendet. */
    val key: String get() = "${source.name}:$id"
}

/**
 * @param uri Audio- (bzw. kombinierter HLS-) Stream
 * @param videoUri separater Videostream (YouTube liefert Bild und Ton getrennt), sonst null
 */
data class ResolvedStream(val uri: String, val mimeType: String?, val videoUri: String? = null)
