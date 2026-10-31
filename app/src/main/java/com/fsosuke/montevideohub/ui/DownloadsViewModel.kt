package com.fsosuke.montevideohub.ui

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import androidx.lifecycle.AndroidViewModel
import com.fsosuke.montevideohub.data.DownloadFormat
import com.fsosuke.montevideohub.data.Settings
import com.fsosuke.montevideohub.data.Track
import com.fsosuke.montevideohub.download.DownloadEntry
import com.fsosuke.montevideohub.download.DownloadManager
import com.fsosuke.montevideohub.download.DownloadStore
import com.fsosuke.montevideohub.download.DownloadTask
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow

class DownloadsViewModel(app: Application) : AndroidViewModel(app) {
    val tasks: StateFlow<List<DownloadTask>> = DownloadStore.tasks
    val entries: StateFlow<List<DownloadEntry>> = DownloadStore.entries

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    /** Ereignis: Ordnerauswahl öffnen (Wert = vorgeschlagener Startordner). */
    private val _pickFolder = MutableSharedFlow<Uri?>(extraBufferCapacity = 1)
    val pickFolder: SharedFlow<Uri?> = _pickFolder.asSharedFlow()

    private var pending: Pair<Track, DownloadFormat>? = null

    /** Download anfordern – fragt nach dem Ordner, wenn nötig oder gewünscht. */
    fun request(track: Track, format: DownloadFormat) {
        val s = Settings.value
        val tree = s.downloadTreeUri?.takeIf { hasPermission(it) }
        if (tree == null || s.askFolderEachTime) {
            pending = track to format
            _pickFolder.tryEmit(tree?.let(Uri::parse))
        } else {
            start(track, format, tree)
        }
    }

    /** Nur Ordner ändern (aus den Einstellungen). */
    fun changeFolder() {
        pending = null
        _pickFolder.tryEmit(Settings.value.downloadTreeUri?.let(Uri::parse))
    }

    fun onFolderPicked(uri: Uri?) {
        val request = pending
        pending = null
        if (uri == null) {
            if (request != null) _messages.tryEmit("Download abgebrochen – kein Ordner gewählt")
            return
        }
        val resolver = getApplication<Application>().contentResolver
        runCatching {
            resolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            )
        }
        Settings.update { it.copy(downloadTreeUri = uri.toString()) }
        if (request != null) start(request.first, request.second, uri.toString())
        else _messages.tryEmit("Download-Ordner: ${folderLabel(uri.toString())}")
    }

    private fun start(track: Track, format: DownloadFormat, tree: String) {
        DownloadManager.enqueue(getApplication(), track, format, tree)
        _messages.tryEmit("Download gestartet: ${track.title}")
    }

    fun retry(task: DownloadTask) = DownloadManager.retry(getApplication(), task.id)
    fun cancel(task: DownloadTask) = DownloadManager.cancel(task.id)

    fun delete(entry: DownloadEntry) {
        runCatching {
            DocumentsContract.deleteDocument(getApplication<Application>().contentResolver, Uri.parse(entry.uri))
        }
        DownloadStore.removeEntry(entry.id)
    }

    private fun hasPermission(uri: String): Boolean =
        getApplication<Application>().contentResolver.persistedUriPermissions
            .any { it.uri.toString() == uri && it.isWritePermission }

    companion object {
        /** „primary:Music/Monte“ → „Interner Speicher/Music/Monte“ */
        fun folderLabel(uri: String?): String {
            if (uri == null) return "Noch nicht gewählt"
            val id = runCatching { DocumentsContract.getTreeDocumentId(Uri.parse(uri)) }.getOrNull() ?: return uri
            val volume = id.substringBefore(':')
            val path = id.substringAfter(':', "")
            val root = if (volume == "primary") "Interner Speicher" else "SD-Karte ($volume)"
            return if (path.isBlank()) root else "$root/$path"
        }
    }
}
