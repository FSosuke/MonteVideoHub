package com.fsosuke.montevideohub.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fsosuke.montevideohub.data.DownloadFormat
import com.fsosuke.montevideohub.data.SourceFilter
import com.fsosuke.montevideohub.data.Track
import com.fsosuke.montevideohub.ui.components.TrackRow
import com.fsosuke.montevideohub.ui.theme.MontedioYellow
import com.fsosuke.montevideohub.ui.theme.Navy800
import com.fsosuke.montevideohub.ui.theme.Navy900
import com.fsosuke.montevideohub.ui.theme.Snow

@Composable
fun SearchScreen(
    state: SearchUiState,
    resolving: Set<String>,
    contentPadding: PaddingValues,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onFilter: (SourceFilter) -> Unit,
    onMusicOnly: (Boolean) -> Unit,
    onPlay: (Track) -> Unit,
    onPlayNext: (Track) -> Unit,
    onEnqueue: (Track) -> Unit,
    onDownload: (Track, DownloadFormat) -> Unit,
    onToggleSubscribe: (Track) -> Unit,
    updateAvailable: Boolean,
    checkingUpdate: Boolean,
    onUpdateClick: () -> Unit,
) {
    val focus = LocalFocusManager.current
    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(bottom = contentPadding.calculateBottomPadding())
    ) {
        Header(updateAvailable, checkingUpdate, onUpdateClick)

        OutlinedTextField(
            value = state.query,
            onValueChange = onQueryChange,
            placeholder = { Text("Song, Künstler, Vocaloid …") },
            leadingIcon = { Icon(Icons.Rounded.Search, null) },
            trailingIcon = {
                if (state.query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Rounded.Close, contentDescription = "Leeren")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(28.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = {
                focus.clearFocus()
                onSearch()
            }),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MontedioYellow,
                unfocusedBorderColor = Snow.copy(alpha = 0.35f),
                focusedContainerColor = Navy900.copy(alpha = 0.75f),
                unfocusedContainerColor = Navy900.copy(alpha = 0.55f),
                cursorColor = MontedioYellow,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            SourceFilter.entries.forEach { filter ->
                MonteChip(state.filter == filter, filter.label) { onFilter(filter) }
            }
            Spacer(Modifier.width(4.dp))
            MonteChip(state.musicOnly, "Nur Musik", leading = true) { onMusicOnly(!state.musicOnly) }
        }

        Box(Modifier.weight(1f).fillMaxWidth()) {
            when {
                state.loading -> CircularProgressIndicator(
                    color = MontedioYellow,
                    modifier = Modifier.align(Alignment.Center),
                )

                state.results.isNotEmpty() -> LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    state.error?.let { item { ErrorNote(it) } }
                    items(state.results, key = { it.key }) { track ->
                        TrackRow(
                            track = track,
                            resolving = track.key in resolving,
                            onPlay = { onPlay(track) },
                            onPlayNext = { onPlayNext(track) },
                            onEnqueue = { onEnqueue(track) },
                            onDownload = { onDownload(track, it) },
                            onToggleSubscribe = { onToggleSubscribe(track) },
                        )
                    }
                }

                state.searched -> Column(
                    Modifier
                        .align(Alignment.Center)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("Keine Treffer", style = MaterialTheme.typography.titleMedium)
                    state.error?.let { ErrorNote(it) }
                }

                else -> Text(
                    "Suche gleichzeitig auf YouTube und niconico",
                    textAlign = TextAlign.Center,
                    color = Snow.copy(alpha = 0.85f),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(32.dp),
                )
            }
        }
    }
}

@Composable
fun Header(updateAvailable: Boolean, checking: Boolean, onUpdateClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(start = 20.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
    ) {
    Column(Modifier.weight(1f)) {
        Text(
            buildAnnotatedString {
                append("Monte")
                withStyle(SpanStyle(color = MontedioYellow)) { append("Video") }
                append("Hub")
            },
            fontSize = 30.sp,
            fontWeight = FontWeight.Black,
            fontStyle = FontStyle.Italic,
            color = Snow,
        )
        Text(
            "M O N T E D I O   Y A M A G A T A",
            fontSize = 11.sp,
            letterSpacing = 1.sp,
            color = Snow.copy(alpha = 0.6f),
        )
    }
        IconButton(onClick = onUpdateClick) {
            if (checking) {
                CircularProgressIndicator(color = MontedioYellow, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
            } else {
                BadgedBox(badge = { if (updateAvailable) Badge(containerColor = MontedioYellow) }) {
                    Icon(Icons.Rounded.SystemUpdate, contentDescription = "Nach Updates suchen")
                }
            }
        }
    }
}

@Composable
private fun MonteChip(selected: Boolean, label: String, leading: Boolean = false, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = if (leading) {
            { Icon(Icons.Rounded.MusicNote, null, Modifier.height(18.dp)) }
        } else null,
        colors = FilterChipDefaults.filterChipColors(
            containerColor = Navy800.copy(alpha = 0.6f),
            labelColor = Snow,
            iconColor = Snow,
            selectedContainerColor = MontedioYellow,
            selectedLabelColor = Navy900,
            selectedLeadingIconColor = Navy900,
        ),
    )
}

@Composable
private fun ErrorNote(text: String) {
    Text(
        text,
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.padding(vertical = 4.dp),
    )
}
