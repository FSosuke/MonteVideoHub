package com.fsosuke.montevideohub.download

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import java.io.File
import java.io.IOException
import java.nio.ByteBuffer

/**
 * Setzt Video- und Audiospur verlustfrei (ohne Neukodierung) zu einer normalen MP4/M4A zusammen.
 * Wandelt dabei auch fragmentierte MP4 (YouTube-DASH, niconico-CMAF) in reguläre Dateien um.
 */
object Mp4Muxer {

    fun mux(video: File?, audio: File, out: File) {
        val muxer = MediaMuxer(out.path, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        val inputs = mutableListOf<Input>()
        try {
            video?.let { addTrack(it, "video/", muxer)?.let(inputs::add) ?: throw IOException("Keine Videospur gefunden") }
            addTrack(audio, "audio/", muxer)?.let(inputs::add) ?: throw IOException("Keine Audiospur gefunden")
            muxer.start()

            val buffer = ByteBuffer.allocateDirect(8 * 1024 * 1024)
            val info = MediaCodec.BufferInfo()
            // Zeitlich verzahnt schreiben (Interleaving)
            while (true) {
                val next = inputs.filter { !it.done }.minByOrNull { it.extractor.sampleTime } ?: break
                val size = next.extractor.readSampleData(buffer, 0)
                if (size < 0) {
                    next.done = true
                    continue
                }
                val flags = if (next.extractor.sampleFlags and MediaExtractor.SAMPLE_FLAG_SYNC != 0) {
                    MediaCodec.BUFFER_FLAG_KEY_FRAME
                } else 0
                info.set(0, size, next.extractor.sampleTime, flags)
                muxer.writeSampleData(next.muxerTrack, buffer, info)
                if (!next.extractor.advance()) next.done = true
            }
            muxer.stop()
        } finally {
            inputs.forEach { it.extractor.release() }
            runCatching { muxer.release() }
        }
    }

    private class Input(val extractor: MediaExtractor, val muxerTrack: Int, var done: Boolean = false)

    private fun addTrack(file: File, mimePrefix: String, muxer: MediaMuxer): Input? {
        val extractor = MediaExtractor()
        extractor.setDataSource(file.path)
        for (i in 0 until extractor.trackCount) {
            val format = extractor.getTrackFormat(i)
            if (format.getString(MediaFormat.KEY_MIME)?.startsWith(mimePrefix) == true) {
                extractor.selectTrack(i)
                return Input(extractor, muxer.addTrack(format))
            }
        }
        extractor.release()
        return null
    }
}
