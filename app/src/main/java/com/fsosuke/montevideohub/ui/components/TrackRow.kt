package com.fsosuke.montevideohub.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.fsosuke.montevideohub.data.DownloadFormat
import com.fsosuke.montevideohub.data.Source
import com.fsosuke.montevideohub.data.Track
import com.fsosuke.montevideohub.ui.formatDuration
import com.fsosuke.montevideohub.ui.formatViews
import com.fsosuke.montevideohub.ui.theme.MontedioYellow
import com.fsosuke.montevideohub.ui.theme.Navy800

@Composable
fun TrackRow(
    track: Track,
    resolving: Boolean,
    onPlay: () -> Unit,
    onPlayNext: () -> Unit,
    onEnqueue: () -> Unit,
    onDownload: (DownloadFormat) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onPlay,
        shape = RoundedCornerShape(14.dp),
        color = Navy800.copy(alpha = 0.72f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Artwork(track.thumbnail, Modifier.size(56.dp))
            Column(
                Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
            ) {
                Text(
                    track.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SourceBadge(track.source)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        subtitle(track),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (resolving) {
                CircularProgressIndicator(
                    color = MontedioYellow,
                    strokeWidth = 2.dp,
                    modifier = Modifier
                        .padding(12.dp)
                        .size(24.dp),
                )
            } else {
                var menuOpen by remember { mutableStateOf(false) }
                Box {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Rounded.MoreVert, contentDescription = "Optionen")
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text("Als Nächstes spielen") },
                            leadingIcon = { Icon(Icons.Rounded.SkipNext, null) },
                            onClick = { menuOpen = false; onPlayNext() },
                        )
                        DropdownMenuItem(
                            text = { Text("Zur Warteschlange") },
                            leadingIcon = { Icon(Icons.Rounded.Add, null) },
                            onClick = { menuOpen = false; onEnqueue() },
                        )
                        DownloadMenuItems(onDownload = { menuOpen = false; onDownload(it) })
                    }
                }
            }
        }
    }
}

private fun subtitle(track: Track): String = listOfNotNull(
    track.artist.takeIf { track.source == Source.YOUTUBE && it.isNotBlank() },
    formatDuration(track.durationSec).ifEmpty { null },
    formatViews(track.views).ifEmpty { null },
).joinToString(" · ")

/** Menüeinträge „Herunterladen (Audio/Video)“ – auch im Player und auf der Startseite genutzt. */
@Composable
fun DownloadMenuItems(onDownload: (DownloadFormat) -> Unit) {
    DropdownMenuItem(
        text = { Text("Herunterladen (Audio)") },
        leadingIcon = { Icon(Icons.Rounded.Headphones, null) },
        onClick = { onDownload(DownloadFormat.AUDIO) },
    )
    DropdownMenuItem(
        text = { Text("Herunterladen (Video)") },
        leadingIcon = { Icon(Icons.Rounded.Download, null) },
        onClick = { onDownload(DownloadFormat.VIDEO) },
    )
}
