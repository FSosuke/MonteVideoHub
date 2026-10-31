package com.fsosuke.montevideohub.download

import com.fsosuke.montevideohub.data.Http
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

/** Lädt eine Datei in Stücken (Range-Requests) – YouTube drosselt/blockt große Einzel-Requests. */
object HttpFile {
    private const val CHUNK = 4L * 1024 * 1024

    suspend fun download(url: String, file: File, onProgress: (Float) -> Unit) {
        file.outputStream().use { out ->
            var pos = 0L
            var total = -1L
            while (total < 0 || pos < total) {
                currentCoroutineContext().ensureActive()
                val request = Request.Builder().url(url).header("Range", "bytes=$pos-${pos + CHUNK - 1}").build()
                val done = Http.client.newCall(request).execute().use { response ->
                    when (response.code) {
                        200 -> {
                            // Server ignoriert Range → komplette Datei in einem Stück
                            val body = response.body ?: throw IOException("Leere Antwort")
                            val length = body.contentLength()
                            copy(body.byteStream(), out) { n -> if (length > 0) onProgress(n.toFloat() / length) }
                            true
                        }
                        206 -> {
                            total = response.header("Content-Range")?.substringAfter('/')?.toLongOrNull() ?: -1
                            val n = copy(response.body?.byteStream() ?: throw IOException("Leere Antwort"), out) {}
                            pos += n
                            if (total > 0) onProgress(pos.toFloat() / total)
                            n == 0L || (total < 0 && n < CHUNK)
                        }
                        416 -> true
                        else -> throw IOException("Download fehlgeschlagen (HTTP ${response.code})")
                    }
                }
                if (done) break
            }
        }
    }

    suspend fun bytes(url: String): ByteArray {
        currentCoroutineContext().ensureActive()
        return Http.client.newCall(Request.Builder().url(url).build()).execute().use { response ->
            if (!response.isSuccessful) throw IOException("HTTP ${response.code} beim Laden von ${response.request.url.host}")
            response.body?.bytes() ?: throw IOException("Leere Antwort")
        }
    }

    suspend fun text(url: String): String = String(bytes(url), Charsets.UTF_8)

    private inline fun copy(input: InputStream, out: OutputStream, onBytes: (Long) -> Unit): Long {
        val buffer = ByteArray(64 * 1024)
        var total = 0L
        while (true) {
            val n = input.read(buffer)
            if (n < 0) break
            out.write(buffer, 0, n)
            total += n
            onBytes(total)
        }
        return total
    }
}
