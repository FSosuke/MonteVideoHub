package com.fsosuke.montevideohub.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.media3.common.Player
import com.fsosuke.montevideohub.playback.PlayerUiState
import com.fsosuke.montevideohub.ui.components.Artwork
import com.fsosuke.montevideohub.ui.components.MontedioBackground
import com.fsosuke.montevideohub.ui.components.SourceBadge
import com.fsosuke.montevideohub.ui.components.VideoSurface
import com.fsosuke.montevideohub.ui.theme.MontedioYellow
import com.fsosuke.montevideohub.ui.theme.Navy800
import com.fsosuke.montevideohub.ui.theme.Navy900
import com.fsosuke.montevideohub.ui.theme.Snow

@Composable
fun PlayerScreen(
    state: PlayerUiState,
    player: Player?,
    vm: PlayerViewModel,
    showVideo: Boolean,
    onFullscreen: () -> Unit,
    onClose: () -> Unit,
) {
    val current = state.current ?: return
    Box(Modifier.fillMaxSize()) {
        MontedioBackground(dim = 0.88f)
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                IconButton(onClick = onClose) {
                    Icon(Icons.Rounded.KeyboardArrowDown, "Schließen", Modifier.size(32.dp))
                }
                Text(
                    "JETZT LÄUFT",
                    color = MontedioYellow,
                    letterSpacing = 3.sp,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
                // Umschalter Audio ⇄ Video
                IconButton(onClick = { vm.setVideoMode(!state.videoMode) }) {
                    Icon(
                        if (state.videoMode) Icons.Rounded.Videocam else Icons.Rounded.Headphones,
                        contentDescription = if (state.videoMode) "Video-Modus (tippen für nur Audio)" else "Audio-Modus (tippen für Video)",
                        tint = MontedioYellow,
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            if (state.videoMode && state.hasVideo) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(16.dp))
                        .border(BorderStroke(2.dp, MontedioYellow), RoundedCornerShape(16.dp))
                        .background(Color.Black)
                ) {
                    if (showVideo) {
                        VideoSurface(player = player, keepScreenOn = state.isPlaying, modifier = Modifier.fillMaxSize())
                    }
                    IconButton(
                        onClick = onFullscreen,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(4.dp),
                    ) {
                        Icon(Icons.Rounded.Fullscreen, contentDescription = "Vollbild", tint = Snow)
                    }
                }
            } else {
                Artwork(
                    current.artwork,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth(0.82f)
                        .aspectRatio(1f)
                        .border(BorderStroke(2.dp, MontedioYellow), RoundedCornerShape(20.dp)),
                )
            }
            Spacer(Modifier.height(20.dp))

            Text(
                current.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                SourceBadge(current.source)
                Spacer(Modifier.width(6.dp))
                Text(current.artist, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }

            SeekBar(state, onSeek = vm::seekTo)
            Controls(state, vm)

            Text(
                "Warteschlange",
                style = MaterialTheme.typography.titleSmall,
                color = MontedioYellow,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 4.dp),
            )
            Queue(state, vm, Modifier.weight(1f))
        }
    }
}

@Composable
private fun SeekBar(state: PlayerUiState, onSeek: (Long) -> Unit) {
    var dragging by remember { mutableStateOf<Float?>(null) }
    val duration = state.durationMs
    val progress = if (duration > 0) (state.positionMs.toFloat() / duration).coerceIn(0f, 1f) else 0f
    Column(Modifier.padding(top = 12.dp)) {
        Slider(
            value = dragging ?: progress,
            onValueChange = { dragging = it },
            onValueChangeFinished = {
                dragging?.let { if (duration > 0) onSeek((it * duration).toLong()) }
                dragging = null
            },
            colors = SliderDefaults.colors(
                thumbColor = MontedioYellow,
                activeTrackColor = MontedioYellow,
                inactiveTrackColor = Snow.copy(alpha = 0.2f),
            ),
        )
        Row(Modifier.fillMaxWidth()) {
            val shown = dragging?.let { (it * duration).toLong() } ?: state.positionMs
            Text(formatMs(shown), fontSize = 12.sp, color = Snow.copy(alpha = 0.7f))
            Spacer(Modifier.weight(1f))
            Text(formatMs(duration), fontSize = 12.sp, color = Snow.copy(alpha = 0.7f))
        }
    }
}

@Composable
private fun Controls(state: PlayerUiState, vm: PlayerViewModel) {
    Row(
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
    ) {
        IconButton(onClick = vm::toggleShuffle) {
            Icon(
                Icons.Rounded.Shuffle, "Zufall",
                tint = if (state.shuffle) MontedioYellow else Snow.copy(alpha = 0.6f),
            )
        }
        IconButton(onClick = vm::previous) {
            Icon(Icons.Rounded.SkipPrevious, "Zurück", Modifier.size(36.dp))
        }
        Surface(
            onClick = vm::togglePlayPause,
            shape = CircleShape,
            color = MontedioYellow,
            contentColor = Navy900,
            modifier = Modifier.size(72.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                when {
                    state.isBuffering -> CircularProgressIndicator(
                        color = Navy900,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(32.dp),
                    )
                    state.isPlaying -> Icon(Icons.Rounded.Pause, "Pause", Modifier.size(40.dp))
                    else -> Icon(Icons.Rounded.PlayArrow, "Abspielen", Modifier.size(40.dp))
                }
            }
        }
        IconButton(onClick = vm::next) {
            Icon(Icons.Rounded.SkipNext, "Weiter", Modifier.size(36.dp))
        }
        IconButton(onClick = vm::cycleRepeat) {
            Icon(
                if (state.repeatMode == Player.REPEAT_MODE_ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                "Wiederholen",
                tint = if (state.repeatMode != Player.REPEAT_MODE_OFF) MontedioYellow else Snow.copy(alpha = 0.6f),
            )
        }
    }
}

@Composable
private fun Queue(state: PlayerUiState, vm: PlayerViewModel, modifier: Modifier) {
    val listState = rememberLazyListState()
    LaunchedEffect(state.currentIndex) {
        if (state.currentIndex >= 0) listState.animateScrollToItem(state.currentIndex)
    }
    LazyColumn(state = listState, modifier = modifier.fillMaxWidth()) {
        itemsIndexed(state.queue) { index, entry ->
            val isCurrent = index == state.currentIndex
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { vm.skipTo(index) }
                    .background(if (isCurrent) Navy800.copy(alpha = 0.7f) else Navy800.copy(alpha = 0f))
                    .padding(vertical = 6.dp, horizontal = 4.dp),
            ) {
                Artwork(entry.artwork, Modifier.size(40.dp), RoundedCornerShape(8.dp))
                Column(
                    Modifier
                        .weight(1f)
                        .padding(horizontal = 10.dp)
                ) {
                    Text(
                        entry.title,
                        color = if (isCurrent) MontedioYellow else Snow,
                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        entry.artist,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
                if (!isCurrent) {
                    IconButton(onClick = { vm.remove(index) }) {
                        Icon(Icons.Rounded.Close, "Entfernen", tint = Snow.copy(alpha = 0.6f))
                    }
                }
            }
        }
    }
}
