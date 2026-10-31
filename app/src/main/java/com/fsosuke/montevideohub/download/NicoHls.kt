package com.fsosuke.montevideohub.download

import okhttp3.HttpUrl.Companion.toHttpUrl
import java.io.File
import java.math.BigInteger
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Lädt niconico-HLS (domand) herunter: Playlists lesen, AES-128 entschlüsseln,
 * Init-Segment + Segmente zu einer fragmentierten MP4 zusammensetzen.
 */
object NicoHls {
    data class Variant(val url: String, val bandwidth: Long, val height: Int)
    data class Master(val audioUrl: String?, val variants: List<Variant>)

    private val attrRegex = Regex("""([A-Z0-9-]+)=("[^"]*"|[^,]*)""")

    private fun attrs(line: String): Map<String, String> =
        attrRegex.findAll(line.substringAfter(':')).associate {
            it.groupValues[1] to it.groupValues[2].trim('"')
        }

    private fun resolve(base: String, ref: String) = base.toHttpUrl().resolve(ref)?.toString() ?: ref

    suspend fun master(masterUrl: String): Master {
        val lines = HttpFile.text(masterUrl).lines().map { it.trim() }
        var audio: String? = null
        val variants = mutableListOf<Variant>()
        lines.forEachIndexed { i, line ->
            when {
                line.startsWith("#EXT-X-MEDIA:") -> {
                    val a = attrs(line)
                    if (a["TYPE"] == "AUDIO" && audio == null) a["URI"]?.let { audio = resolve(masterUrl, it) }
                }
                line.startsWith("#EXT-X-STREAM-INF:") -> {
                    val a = attrs(line)
                    val uri = lines.drop(i + 1).firstOrNull { it.isNotBlank() && !it.startsWith("#") } ?: return@forEachIndexed
                    variants += Variant(
                        url = resolve(masterUrl, uri),
                        bandwidth = a["BANDWIDTH"]?.toLongOrNull() ?: 0,
                        height = a["RESOLUTION"]?.substringAfter('x')?.toIntOrNull() ?: 0,
                    )
                }
            }
        }
        return Master(audio, variants)
    }

    private data class Key(val uri: String, val iv: ByteArray?)
    private data class Part(val url: String, val key: Key?, val sequence: Long)

    /** Eine Rendition (Audio- oder Video-Playlist) komplett laden und entschlüsselt in [out] schreiben. */
    suspend fun downloadRendition(playlistUrl: String, out: File, onProgress: (Float) -> Unit) {
        val lines = HttpFile.text(playlistUrl).lines().map { it.trim() }
        var sequence = 0L
        var key: Key? = null
        var initPart: Part? = null
        val segments = mutableListOf<Part>()
        for (line in lines) {
            when {
                line.isBlank() -> Unit
                line.startsWith("#EXT-X-MEDIA-SEQUENCE:") -> sequence = line.substringAfter(':').toLongOrNull() ?: 0
                line.startsWith("#EXT-X-KEY:") -> {
                    val a = attrs(line)
                    key = if (a["METHOD"] == "AES-128") Key(resolve(playlistUrl, a.getValue("URI")), a["IV"]?.let(::hex)) else null
                }
                line.startsWith("#EXT-X-MAP:") -> attrs(line)["URI"]?.let { initPart = Part(resolve(playlistUrl, it), key, sequence) }
                line.startsWith("#") -> Unit
                else -> segments += Part(resolve(playlistUrl, line), key, sequence++)
            }
        }
        if (segments.isEmpty()) throw java.io.IOException("Leere Playlist")

        val keys = HashMap<String, ByteArray>()
        suspend fun data(part: Part): ByteArray {
            val raw = HttpFile.bytes(part.url)
            val k = part.key ?: return raw
            val keyBytes = keys.getOrPut(k.uri) { HttpFile.bytes(k.uri) }
            val iv = k.iv ?: BigInteger.valueOf(part.sequence).toByteArray().let { b ->
                ByteArray(16).also { b.copyInto(it, 16 - minOf(16, b.size), maxOf(0, b.size - 16)) }
            }
            return Cipher.getInstance("AES/CBC/PKCS5Padding").run {
                init(Cipher.DECRYPT_MODE, SecretKeySpec(keyBytes, "AES"), IvParameterSpec(iv))
                doFinal(raw)
            }
        }

        out.outputStream().use { stream ->
            initPart?.let { stream.write(data(it)) }
            segments.forEachIndexed { i, part ->
                stream.write(data(part))
                onProgress((i + 1).toFloat() / segments.size)
            }
        }
    }

    private fun hex(value: String): ByteArray {
        val h = value.removePrefix("0x").removePrefix("0X").padStart(32, '0')
        return ByteArray(16) { i -> h.substring(i * 2, i * 2 + 2).toInt(16).toByte() }
    }
}
