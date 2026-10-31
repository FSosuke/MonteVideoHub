package com.fsosuke.montevideohub.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fsosuke.montevideohub.data.DownloadFormat
import com.fsosuke.montevideohub.download.DownloadEntry
import com.fsosuke.montevideohub.download.DownloadState
import com.fsosuke.montevideohub.download.DownloadTask
import com.fsosuke.montevideohub.ui.components.Artwork
import com.fsosuke.montevideohub.ui.components.SourceBadge
import com.fsosuke.montevideohub.ui.theme.MontedioYellow
import com.fsosuke.montevideohub.ui.theme.Navy800
import com.fsosuke.montevideohub.ui.theme.Snow
import java.util.Locale

@Composable
fun DownloadsScreen(
    tasks: List<DownloadTask>,
    entries: List<DownloadEntry>,
    folder: String?,
    contentPadding: PaddingValues,
    onChangeFolder: () -> Unit,
    onPlay: (DownloadEntry) -> Unit,
    onDelete: (DownloadEntry) -> Unit,
    onRetry: (DownloadTask) -> Unit,
    onCancel: (DownloadTask) -> Unit,
) {
    LazyColumn(
        contentPadding = PaddingValues(
            start = 16.dp, end = 16.dp, bottom = contentPadding.calculateBottomPadding() + 16.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
        item { ScreenTitle("Downloads") }
        item {
            Surface(
                onClick = onChangeFolder,
                shape = RoundedCornerShape(14.dp),
                color = Navy800.copy(alpha = 0.8f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Folder, null, tint = MontedioYellow)
                    Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                        Text("Speicherort", style = MaterialTheme.typography.labelMedium, color = MontedioYellow)
                        Text(DownloadsViewModel.folderLabel(folder), maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                    Text("Ändern", color = MontedioYellow, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (tasks.isNotEmpty()) {
            item { SubTitle("Läuft") }
            items(tasks, key = { it.id }) { task -> TaskRow(task, onRetry, onCancel) }
        }

        item { SubTitle("Fertig") }
        if (entries.isEmpty()) {
            item {
                Text(
                    "Noch nichts heruntergeladen. Über das ⋮-Menü bei einem Titel oder im Player " +
                        "„Herunterladen“ wählen.",
                    color = Snow.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        items(entries, key = { it.id }) { entry -> EntryRow(entry, onPlay, onDelete) }
    }
}

@Composable
fun ScreenTitle(text: String) {
    Text(
        text,
        fontSize = 28.sp,
        fontWeight = FontWeight.Black,
        fontStyle = FontStyle.Italic,
        color = Snow,
        modifier = Modifier.padding(top = 12.dp, bottom = 8.dp),
    )
}

@Composable
private fun SubTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MontedioYellow,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 12.dp),
    )
}

@Composable
private fun TaskRow(task: DownloadTask, onRetry: (DownloadTask) -> Unit, onCancel: (DownloadTask) -> Unit) {
    Surface(shape = RoundedCornerShape(14.dp), color = Navy800.copy(alpha = 0.8f)) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Artwork(task.track.thumbnail, Modifier.size(44.dp))
                Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    Text(task.track.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
                    Text(
                        "${task.format.label} · ${task.error ?: task.status}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (task.state == DownloadState.FAILED) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                    )
                }
                if (task.state == DownloadState.FAILED) {
                    IconButton(onClick = { onRetry(task) }) { Icon(Icons.Rounded.Refresh, "Erneut versuchen") }
                }
                IconButton(onClick = { onCancel(task) }) { Icon(Icons.Rounded.Close, "Abbrechen") }
            }
            if (task.state != DownloadState.FAILED) {
                LinearProgressIndicator(
                    progress = { task.progress },
                    color = MontedioYellow,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun EntryRow(entry: DownloadEntry, onPlay: (DownloadEntry) -> Unit, onDelete: (DownloadEntry) -> Unit) {
    Surface(
        onClick = { onPlay(entry) },
        shape = RoundedCornerShape(14.dp),
        color = Navy800.copy(alpha = 0.72f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Artwork(entry.track.thumbnail, Modifier.size(52.dp))
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(entry.track.title, maxLines = 2, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SourceBadge(entry.track.source)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        (if (entry.format == DownloadFormat.AUDIO) "Audio" else "Video") + " · " +
                            String.format(Locale.GERMANY, "%.1f MB", entry.sizeBytes / 1_048_576.0),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            IconButton(onClick = { onDelete(entry) }) {
                Icon(Icons.Rounded.Delete, "Löschen", tint = Snow.copy(alpha = 0.7f))
            }
        }
    }
}

@Composable
fun YellowTextButton(text: String, onClick: () -> Unit) {
    TextButton(onClick = onClick) { Text(text, color = MontedioYellow, fontWeight = FontWeight.Bold) }
}
