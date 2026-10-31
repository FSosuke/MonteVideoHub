package com.fsosuke.montevideohub.download

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.fsosuke.montevideohub.data.DownloadFormat
import com.fsosuke.montevideohub.data.NicoSource
import com.fsosuke.montevideohub.data.Settings
import com.fsosuke.montevideohub.data.Source
import com.fsosuke.montevideohub.data.Track
import com.fsosuke.montevideohub.data.YouTubeSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import java.util.UUID

/** Download-Warteschlange: eins nach dem anderen, Ergebnis landet im gewählten Ordner. */
object DownloadManager {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val queue = Channel<String>(Channel.UNLIMITED)
    private lateinit var appContext: Context
    @Volatile private var current: Pair<String, Job>? = null

    fun init(context: Context) {
        appContext = context.applicationContext
        scope.launch {
            for (id in queue) {
                val job = launch { process(id) }
                current = id to job
                job.join()
                current = null
            }
        }
    }

    fun enqueue(context: Context, track: Track, format: DownloadFormat, treeUri: String) {
        val task = DownloadTask(UUID.randomUUID().toString(), track, format, treeUri)
        DownloadStore.addTask(task)
        queue.trySend(task.id)
        DownloadService.start(context)
    }

    fun retry(context: Context, id: String) {
        DownloadStore.updateTask(id) { it.copy(state = DownloadState.QUEUED, progress = 0f, error = null, status = "Wartet …") }
        queue.trySend(id)
        DownloadService.start(context)
    }

    fun cancel(id: String) {
        current?.takeIf { it.first == id }?.second?.cancel()
        DownloadStore.removeTask(id)
    }

    private suspend fun process(id: String) {
        val task = DownloadStore.task(id) ?: return
        if (task.state != DownloadState.QUEUED) return
        val work = File(appContext.cacheDir, "dl/$id").apply { mkdirs() }
        fun progress(p: Float, status: String) =
            DownloadStore.updateTask(id) { it.copy(state = DownloadState.RUNNING, progress = p.coerceIn(0f, 1f), status = status) }

        try {
            progress(0f, "Stream wird gesucht …")
            val video = task.format == DownloadFormat.VIDEO
            val maxHeight = Settings.value.maxVideoHeight
            val out = File(work, "out.mp4")
            val audioFile = File(work, "audio.bin")
            val videoFile = File(work, "video.bin")

            when (task.track.source) {
                Source.YOUTUBE -> {
                    val streams = YouTubeSource.downloadStreams(task.track.id, video, maxHeight)
                    when {
                        !video -> {
                            HttpFile.download(streams.audioUrl, audioFile) { progress(it * 0.95f, "Audio wird geladen …") }
                            Mp4Muxer.mux(null, audioFile, out)
                        }
                        streams.videoUrl != null -> {
                            HttpFile.download(streams.videoUrl, videoFile) { progress(it * 0.75f, "Video wird geladen …") }
                            HttpFile.download(streams.audioUrl, audioFile) { progress(0.75f + it * 0.2f, "Audio wird geladen …") }
                            progress(0.96f, "Wird zusammengefügt …")
                            Mp4Muxer.mux(videoFile, audioFile, out)
                        }
                        else -> HttpFile.download(streams.muxedVideoUrl!!, out) { progress(it * 0.98f, "Video wird geladen …") }
                    }
                }

                Source.NICONICO -> {
                    val master = NicoHls.master(NicoSource.resolve(task.track.id).uri)
                    val audioUrl = master.audioUrl ?: throw IOException("Keine Audiospur in der Playlist")
                    if (video) {
                        val variant = master.variants.filter { it.height in 1..maxHeight }.maxByOrNull { it.bandwidth }
                            ?: master.variants.minByOrNull { it.bandwidth }
                            ?: throw IOException("Keine Videospur in der Playlist")
                        NicoHls.downloadRendition(variant.url, videoFile) { progress(it * 0.75f, "Video wird geladen …") }
                        NicoHls.downloadRendition(audioUrl, audioFile) { progress(0.75f + it * 0.2f, "Audio wird geladen …") }
                        progress(0.96f, "Wird zusammengefügt …")
                        Mp4Muxer.mux(videoFile, audioFile, out)
                    } else {
                        NicoHls.downloadRendition(audioUrl, audioFile) { progress(it * 0.95f, "Audio wird geladen …") }
                        Mp4Muxer.mux(null, audioFile, out)
                    }
                }
            }

            progress(0.99f, "Wird gespeichert …")
            val (uri, name) = export(task, out)
            DownloadStore.complete(
                DownloadEntry(id, task.track, task.format, uri.toString(), name, out.length(), System.currentTimeMillis())
            )
        } catch (e: CancellationException) {
            DownloadStore.removeTask(id)
        } catch (e: Exception) {
            DownloadStore.updateTask(id) {
                it.copy(state = DownloadState.FAILED, error = e.message ?: e.javaClass.simpleName, status = "Fehlgeschlagen")
            }
        } finally {
            work.deleteRecursively()
        }
    }

    /** Fertige Datei in den gewählten Ordner (Storage Access Framework) kopieren. */
    private fun export(task: DownloadTask, file: File): Pair<Uri, String> {
        val tree = DocumentFile.fromTreeUri(appContext, Uri.parse(task.treeUri))
            ?: throw IOException("Download-Ordner nicht verfügbar")
        if (!tree.canWrite()) throw IOException("Keine Schreibrechte für den Download-Ordner – bitte neu wählen")
        val audioOnly = task.format == DownloadFormat.AUDIO
        val mime = if (audioOnly) "audio/mp4" else "video/mp4"
        val ext = if (audioOnly) "m4a" else "mp4"
        val base = buildString {
            if (task.track.artist.isNotBlank() && task.track.artist != "niconico") append(task.track.artist).append(" - ")
            append(task.track.title)
        }.replace(Regex("""[\\/:*?"<>|\n\r\t]"""), "_").trim().take(120).ifBlank { task.track.id }
        val target = tree.createFile(mime, base) ?: throw IOException("Datei konnte nicht angelegt werden")
        appContext.contentResolver.openOutputStream(target.uri)?.use { out ->
            file.inputStream().use { it.copyTo(out) }
        } ?: throw IOException("Datei konnte nicht geschrieben werden")
        return target.uri to (target.name ?: "$base.$ext")
    }
}
