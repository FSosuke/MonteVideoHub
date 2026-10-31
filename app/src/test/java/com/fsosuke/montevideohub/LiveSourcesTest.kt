package com.fsosuke.montevideohub

import com.fsosuke.montevideohub.data.Http
import com.fsosuke.montevideohub.data.NewPipeDownloader
import com.fsosuke.montevideohub.data.NicoSource
import com.fsosuke.montevideohub.data.YouTubeSource
import com.fsosuke.montevideohub.download.NicoHls
import java.io.File
import kotlinx.coroutines.runBlocking
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.localization.ContentCountry
import org.schabi.newpipe.extractor.localization.Localization

/**
 * Echte Netzwerk-Tests gegen YouTube und niconico (nur mit LIVE_TESTS=1, z. B. in der CI).
 * Prüft Suche, Stream-Auflösung und ob die ersten Bytes des Streams wirklich ladbar sind.
 */
class LiveSourcesTest {

    @Before
    fun setUp() {
        assumeTrue("Live-Tests deaktiviert", System.getenv("LIVE_TESTS") == "1")
        NewPipe.init(NewPipeDownloader(Http.client), Localization("de", "DE"), ContentCountry("DE"))
    }

    private fun fetchStart(url: String, rangeHeader: Boolean = true): Int =
        Http.client.newCall(
            Request.Builder().url(url).apply { if (rangeHeader) header("Range", "bytes=0-4095") }.build()
        ).execute().use { response ->
            val bytes = response.body?.bytes()?.size ?: 0
            println("  -> HTTP ${response.code}, ${bytes} Bytes, ${response.header("Content-Type")}")
            response.code
        }

    @Test
    fun youtubeSearchAndStream() = runBlocking {
        for (musicOnly in listOf(true, false)) {
            val tracks = YouTubeSource.search("YOASOBI アイドル", musicOnly)
            println("YouTube (musicOnly=$musicOnly): ${tracks.size} Treffer")
            tracks.take(3).forEach { println("  ${it.id} | ${it.title} | ${it.artist} | ${it.durationSec}s") }
            assertTrue(tracks.isNotEmpty())
        }
        val track = YouTubeSource.search("YOASOBI アイドル", true).first()
        val stream = YouTubeSource.resolve(track.id)
        println("YouTube-Stream: ${stream.uri.take(160)}… (mime=${stream.mimeType})")
        assertTrue(fetchStart(stream.uri) in 200..299)
    }

    @Test
    fun homeFeeds() = runBlocking {
        println("YouTube-Kioske: ${YouTubeSource.kioskIds()}")
        val (name, yt) = YouTubeSource.home()
        println("YouTube-Start „$name“: ${yt.size} Titel")
        yt.take(3).forEach { println("  ${it.id} | ${it.title} | ${it.artist}") }
        for (genre in listOf("music_sound", "all")) {
            val nico = NicoSource.ranking(genre)
            println("niconico-Ranking $genre: ${nico.size} Titel")
            nico.take(3).forEach { println("  ${it.id} | ${it.title} | ${it.artist}") }
            assertTrue(nico.isNotEmpty())
        }
    }

    /** Lädt die Audiospur eines kurzen niconico-Videos komplett herunter und prüft die Entschlüsselung. */
    @Test
    fun niconicoDownloadDecrypts() = runBlocking {
        val master = NicoHls.master(NicoSource.resolve("sm9").uri)
        println("Master: audio=${master.audioUrl != null}, Varianten=${master.variants.map { "${it.height}p" }}")
        val out = File.createTempFile("nico", ".mp4")
        NicoHls.downloadRendition(master.audioUrl!!, out) {}
        val bytes = out.readBytes()
        val text = String(bytes, Charsets.ISO_8859_1)
        println("Audio-Datei: ${bytes.size} Bytes, ftyp=${text.contains("ftyp")}, moof=${text.contains("moof")}, mdat=${text.contains("mdat")}")
        assertTrue("ftyp fehlt", text.contains("ftyp"))
        assertTrue("moof fehlt – Entschlüsselung kaputt?", text.contains("moof"))
        out.delete()
    }

    @Test
    fun niconicoSearchAndStream() = runBlocking {
        for (musicOnly in listOf(true, false)) {
            val tracks = NicoSource.search("千本桜", musicOnly)
            println("niconico (musicOnly=$musicOnly): ${tracks.size} Treffer")
            tracks.take(3).forEach { println("  ${it.id} | ${it.title} | ${it.artist} | ${it.durationSec}s") }
            assertTrue(tracks.isNotEmpty())
        }
        val stream = NicoSource.resolve("sm15630734")
        println("niconico-Master: ${stream.uri.take(160)}")
        val master = Http.client.newCall(Request.Builder().url(stream.uri).build()).execute().use {
            println("  -> Master HTTP ${it.code}")
            it.body?.string().orEmpty()
        }
        println(master.take(800))
        val audioUri = Regex("URI=\"([^\"]+)\"").find(master)?.groupValues?.get(1)
        assertTrue("Keine Audio-Rendition in der Master-Playlist", audioUri != null)
        val audioUrl = stream.uri.toHttpUrl().resolve(audioUri!!).toString()
        val audioPlaylist = Http.client.newCall(Request.Builder().url(audioUrl).build()).execute().use {
            println("  -> Audio-Playlist HTTP ${it.code}")
            it.body?.string().orEmpty()
        }
        println(audioPlaylist.take(600))
        val keyUri = Regex("#EXT-X-KEY:[^\\n]*URI=\"([^\"]+)\"").find(audioPlaylist)?.groupValues?.get(1)
        if (keyUri != null) {
            println("Schlüssel:")
            assertTrue(fetchStart(audioUrl.toHttpUrl().resolve(keyUri).toString(), rangeHeader = false) in 200..299)
        }
        val segment = audioPlaylist.lines().firstOrNull { it.isNotBlank() && !it.startsWith("#") }
        assertTrue("Kein Segment", segment != null)
        println("Erstes Segment:")
        assertTrue(fetchStart(audioUrl.toHttpUrl().resolve(segment!!).toString(), rangeHeader = false) in 200..299)
    }
}
