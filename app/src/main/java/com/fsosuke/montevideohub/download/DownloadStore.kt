package com.fsosuke.montevideohub.download

import android.content.Context
import com.fsosuke.montevideohub.data.DownloadFormat
import com.fsosuke.montevideohub.data.Track
import com.fsosuke.montevideohub.data.objects
import com.fsosuke.montevideohub.data.toJson
import com.fsosuke.montevideohub.data.toTrack
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

enum class DownloadState { QUEUED, RUNNING, FAILED }

data class DownloadTask(
    val id: String,
    val track: Track,
    val format: DownloadFormat,
    val treeUri: String,
    val state: DownloadState = DownloadState.QUEUED,
    val progress: Float = 0f,
    val status: String = "Wartet …",
    val error: String? = null,
)

data class DownloadEntry(
    val id: String,
    val track: Track,
    val format: DownloadFormat,
    /** content://-URI der fertigen Datei im gewählten Ordner */
    val uri: String,
    val fileName: String,
    val sizeBytes: Long,
    val createdAt: Long,
)

/** Laufende Downloads (im Speicher) + fertige Downloads (in downloads.json). */
object DownloadStore {
    private lateinit var file: File
    private val _tasks = MutableStateFlow<List<DownloadTask>>(emptyList())
    val tasks: StateFlow<List<DownloadTask>> = _tasks.asStateFlow()
    private val _entries = MutableStateFlow<List<DownloadEntry>>(emptyList())
    val entries: StateFlow<List<DownloadEntry>> = _entries.asStateFlow()

    fun init(context: Context) {
        file = File(context.filesDir, "downloads.json")
        _entries.value = runCatching {
            JSONArray(file.readText()).objects().mapNotNull { o ->
                DownloadEntry(
                    id = o.getString("id"),
                    track = o.getJSONObject("track").toTrack() ?: return@mapNotNull null,
                    format = DownloadFormat.valueOf(o.getString("format")),
                    uri = o.getString("uri"),
                    fileName = o.optString("fileName"),
                    sizeBytes = o.optLong("size"),
                    createdAt = o.optLong("createdAt"),
                )
            }
        }.getOrDefault(emptyList())
    }

    fun task(id: String): DownloadTask? = _tasks.value.firstOrNull { it.id == id }

    fun addTask(task: DownloadTask) = _tasks.update { it + task }

    fun updateTask(id: String, transform: (DownloadTask) -> DownloadTask) =
        _tasks.update { list -> list.map { if (it.id == id) transform(it) else it } }

    fun removeTask(id: String) = _tasks.update { list -> list.filterNot { it.id == id } }

    fun complete(entry: DownloadEntry) {
        removeTask(entry.id)
        _entries.update { listOf(entry) + it }
        save()
    }

    fun removeEntry(id: String) {
        _entries.update { list -> list.filterNot { it.id == id } }
        save()
    }

    private fun save() {
        val json = JSONArray(_entries.value.map { e ->
            JSONObject()
                .put("id", e.id)
                .put("track", e.track.toJson())
                .put("format", e.format.name)
                .put("uri", e.uri)
                .put("fileName", e.fileName)
                .put("size", e.sizeBytes)
                .put("createdAt", e.createdAt)
        })
        file.writeText(json.toString())
    }
}
