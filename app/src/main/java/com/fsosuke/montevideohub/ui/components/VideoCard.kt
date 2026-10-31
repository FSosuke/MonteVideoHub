package com.fsosuke.montevideohub.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fsosuke.montevideohub.data.DownloadFormat
import com.fsosuke.montevideohub.data.Track
import com.fsosuke.montevideohub.ui.formatAge
import com.fsosuke.montevideohub.ui.formatDuration
import com.fsosuke.montevideohub.ui.formatViews
import com.fsosuke.montevideohub.ui.theme.MontedioYellow

/** Große Video-Karte wie auf der YouTube-Startseite. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VideoCard(
    track: Track,
    resolving: Boolean,
    onPlay: () -> Unit,
    onPlayNext: () -> Unit,
    onEnqueue: () -> Unit,
    onDownload: (DownloadFormat) -> Unit,
    onToggleSubscribe: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var menuOpen by remember { mutableStateOf(false) }
    Column(
        modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onPlay, onLongClick = { menuOpen = true })
            .padding(bottom = 16.dp)
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
        ) {
            Artwork(track.thumbnail, Modifier.fillMaxSize(), RoundedCornerShape(14.dp))
            formatDuration(track.durationSec).takeIf { it.isNotEmpty() }?.let {
                Text(
                    it,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Black.copy(alpha = 0.75f))
                        .padding(horizontal = 5.dp, vertical = 1.dp),
                )
            }
            if (resolving) {
                CircularProgressIndicator(color = MontedioYellow, modifier = Modifier.align(Alignment.Center))
            }
        }
        Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f).padding(start = 4.dp)) {
                Text(
                    track.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                    SourceBadge(track.source)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        listOfNotNull(
                            track.artist.takeIf { it.isNotBlank() },
                            formatViews(track.views).ifEmpty { null },
                            formatAge(track.uploadedAt).ifEmpty { null },
                        ).joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Box {
                IconButton(onClick = { menuOpen = true }, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Rounded.MoreVert, "Optionen")
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
                    SubscribeMenuItem(track, onToggle = { menuOpen = false; onToggleSubscribe() })
                }
            }
        }
    }
}

/** Kleine quadratische Karte (z. B. „Zuletzt gespielt“). */
@Composable
fun SmallCard(track: Track, onPlay: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier
            .width(128.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onPlay)
    ) {
        Artwork(track.thumbnail, Modifier.size(128.dp), RoundedCornerShape(12.dp))
        Text(
            track.title,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 6.dp),
        )
        Text(
            track.artist,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
