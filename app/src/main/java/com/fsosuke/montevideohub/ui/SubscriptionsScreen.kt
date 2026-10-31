package com.fsosuke.montevideohub.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FileOpen
import androidx.compose.material.icons.rounded.PersonAddAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.fsosuke.montevideohub.data.DownloadFormat
import com.fsosuke.montevideohub.data.FeedState
import com.fsosuke.montevideohub.data.Source
import com.fsosuke.montevideohub.data.Subscription
import com.fsosuke.montevideohub.data.Track
import com.fsosuke.montevideohub.ui.components.SourceBadge
import com.fsosuke.montevideohub.ui.components.VideoCard
import com.fsosuke.montevideohub.ui.theme.MontedioBlue
import com.fsosuke.montevideohub.ui.theme.MontedioYellow
import com.fsosuke.montevideohub.ui.theme.Navy800
import com.fsosuke.montevideohub.ui.theme.Navy900
import com.fsosuke.montevideohub.ui.theme.Snow

/** Abos ohne Konto: Kanalleiste + Feed der neuesten Videos (wie der Abo-Tab bei YouTube). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionsScreen(
    subscriptions: List<Subscription>,
    feed: FeedState,
    selected: String?,
    resolving: Set<String>,
    contentPadding: PaddingValues,
    onSelect: (String?) -> Unit,
    onUnsubscribe: (Subscription) -> Unit,
    onRefresh: () -> Unit,
    onAddChannel: () -> Unit,
    onImport: () -> Unit,
    onPlay: (Track) -> Unit,
    onPlayNext: (Track) -> Unit,
    onEnqueue: (Track) -> Unit,
    onDownload: (Track, DownloadFormat) -> Unit,
    onToggleSubscribe: (Track) -> Unit,
) {
    var confirm by remember { mutableStateOf<Subscription?>(null) }
    val selectedSub = subscriptions.firstOrNull { it.key == selected }
    val items = if (selectedSub == null) feed.items
    else feed.items.filter { it.source == selectedSub.source && it.channelId == selectedSub.id }

    PullToRefreshBox(
        isRefreshing = feed.loading,
        onRefresh = onRefresh,
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
        LazyColumn(
            contentPadding = PaddingValues(bottom = contentPadding.calculateBottomPadding() + 16.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 16.dp)) {
                    Box(Modifier.weight(1f)) { ScreenTitle("Abos") }
                    IconButton(onClick = onAddChannel) { Icon(Icons.Rounded.PersonAddAlt, "Kanal hinzufügen", tint = Snow) }
                    IconButton(onClick = onImport) { Icon(Icons.Rounded.FileOpen, "Abos importieren", tint = Snow) }
                }
            }

            if (subscriptions.isEmpty()) {
                item { EmptySubscriptions(onAddChannel, onImport) }
                return@LazyColumn
            }

            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item { ChannelBubble("Alle", null, null, selected == null, onClick = { onSelect(null) }, onLongClick = {}) }
                    items(subscriptions, key = { it.key }) { sub ->
                        ChannelBubble(
                            name = sub.name,
                            avatar = sub.avatar,
                            source = sub.source,
                            selected = sub.key == selected,
                            onClick = { onSelect(sub.key) },
                            onLongClick = { confirm = sub },
                        )
                    }
                }
            }

            item {
                Text(
                    buildString {
                        append("${subscriptions.size} Abos")
                        if (feed.updatedAt > 0) append(" · aktualisiert ${formatAge(feed.updatedAt)}")
                        if (feed.failed > 0) append(" · ${feed.failed} nicht erreichbar")
                        append(" · lange drücken zum Entfernen")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                )
            }

            if (selectedSub != null) {
                item {
                    OutlinedButton(
                        onClick = { confirm = selectedSub },
                        modifier = Modifier.padding(start = 16.dp, bottom = 8.dp),
                    ) { Text("Abo von „${selectedSub.name}“ beenden", color = Snow) }
                }
            }

            if (items.isEmpty() && !feed.loading) {
                item {
                    Text(
                        "Noch keine Videos – zum Aktualisieren nach unten ziehen.",
                        color = Snow.copy(alpha = 0.8f),
                        modifier = Modifier.padding(24.dp),
                    )
                }
            }

            items(items.take(150), key = { "f_" + it.key }) { track ->
                VideoCard(
                    track = track,
                    resolving = track.key in resolving,
                    onPlay = { onPlay(track) },
                    onPlayNext = { onPlayNext(track) },
                    onEnqueue = { onEnqueue(track) },
                    onDownload = { onDownload(track, it) },
                    onToggleSubscribe = { onToggleSubscribe(track) },
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        }
    }

    confirm?.let { sub ->
        AlertDialog(
            onDismissRequest = { confirm = null },
            containerColor = Navy800,
            title = { Text("Abo beenden?", color = MontedioYellow, fontWeight = FontWeight.Black) },
            text = { Text("„${sub.name}“ aus deinen Abos entfernen?", color = Snow) },
            confirmButton = {
                Button(
                    onClick = { onUnsubscribe(sub); confirm = null },
                    colors = ButtonDefaults.buttonColors(containerColor = MontedioYellow, contentColor = Navy900),
                ) { Text("Entfernen", fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { confirm = null }) { Text("Abbrechen", color = Snow) } },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ChannelBubble(
    name: String,
    avatar: String?,
    source: Source?,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(68.dp)
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
    ) {
        Box {
            ChannelAvatar(
                name, avatar,
                Modifier
                    .size(56.dp)
                    .border(2.dp, if (selected) MontedioYellow else Color.Transparent, CircleShape),
            )
            if (source == Source.NICONICO) {
                SourceBadge(source, Modifier.align(Alignment.BottomCenter))
            }
        }
        Text(
            name,
            fontSize = 11.sp,
            color = if (selected) MontedioYellow else Snow,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

/** Rundes Kanalbild; ohne Bild der Anfangsbuchstabe auf Vereinsblau. */
@Composable
fun ChannelAvatar(name: String, avatar: String?, modifier: Modifier = Modifier) {
    Box(
        modifier
            .clip(CircleShape)
            .background(MontedioBlue),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            name.trim().take(1).uppercase().ifEmpty { "★" },
            color = Snow,
            fontWeight = FontWeight.Black,
            fontSize = 22.sp,
        )
        if (avatar != null) {
            AsyncImage(model = avatar, contentDescription = null, modifier = Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun EmptySubscriptions(onAddChannel: () -> Unit, onImport: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Navy800.copy(alpha = 0.85f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Noch keine Abos", color = MontedioYellow, fontWeight = FontWeight.Black, fontSize = 18.sp)
            Text(
                "Abos funktionieren ohne Konto und bleiben nur auf deinem Handy. " +
                    "Kanäle abonnierst du über „Kanal hinzufügen“, über das ⋮-Menü bei einem Video oder im Player.",
                color = Snow,
                style = MaterialTheme.typography.bodyMedium,
            )
            Button(
                onClick = onAddChannel,
                colors = ButtonDefaults.buttonColors(containerColor = MontedioYellow, contentColor = Navy900),
            ) { Text("Kanal hinzufügen", fontWeight = FontWeight.Bold) }

            Text("YouTube-Abos übernehmen", color = MontedioYellow, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
            Text(
                "1. takeout.google.com öffnen\n" +
                    "2. „Auswahl aufheben“, dann nur „YouTube und YouTube Music“ wählen\n" +
                    "3. „Alle YouTube-Daten“ → nur „Abos“ anhaken\n" +
                    "4. Export erstellen, ZIP herunterladen und entpacken\n" +
                    "5. Hier „Datei importieren“ → Abos.csv wählen\n" +
                    "(Ein NewPipe-Export als .json geht auch.)",
                color = Snow.copy(alpha = 0.9f),
                style = MaterialTheme.typography.bodySmall,
            )
            OutlinedButton(onClick = onImport) { Text("Datei importieren", color = Snow) }
        }
    }
}
