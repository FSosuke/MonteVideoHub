package com.fsosuke.montevideohub.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fsosuke.montevideohub.data.DownloadFormat
import com.fsosuke.montevideohub.data.Source
import com.fsosuke.montevideohub.data.SourceFilter
import com.fsosuke.montevideohub.data.Track
import com.fsosuke.montevideohub.ui.components.SmallCard
import com.fsosuke.montevideohub.ui.components.SourceBadge
import com.fsosuke.montevideohub.ui.components.VideoCard
import com.fsosuke.montevideohub.ui.theme.MontedioYellow
import com.fsosuke.montevideohub.ui.theme.Navy800
import com.fsosuke.montevideohub.ui.theme.Navy900
import com.fsosuke.montevideohub.ui.theme.Snow

/** Startseite wie bei YouTube/niconico: Filter-Chips, „Zuletzt gespielt“, Trends und Rankings. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: HomeUiState,
    history: List<Track>,
    resolving: Set<String>,
    contentPadding: PaddingValues,
    onFilter: (SourceFilter) -> Unit,
    onRefresh: () -> Unit,
    onPlay: (Track) -> Unit,
    onPlayNext: (Track) -> Unit,
    onEnqueue: (Track) -> Unit,
    onDownload: (Track, DownloadFormat) -> Unit,
    header: @Composable () -> Unit,
) {
    PullToRefreshBox(
        isRefreshing = state.loading,
        onRefresh = onRefresh,
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
        LazyColumn(
            contentPadding = PaddingValues(bottom = contentPadding.calculateBottomPadding() + 16.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            item { header() }
            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                ) {
                    SourceFilter.entries.forEach { f ->
                        FilterChip(
                            selected = state.filter == f,
                            onClick = { onFilter(f) },
                            label = { Text(f.label) },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = Navy800.copy(alpha = 0.6f),
                                labelColor = Snow,
                                selectedContainerColor = MontedioYellow,
                                selectedLabelColor = Navy900,
                            ),
                        )
                    }
                }
            }

            val recent = history.filter { state.filter.matches(it.source) }
            if (recent.isNotEmpty()) {
                item { SectionTitle("Zuletzt gespielt", null) }
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(recent.take(20), key = { "h_" + it.key }) { track ->
                            SmallCard(track, onPlay = { onPlay(track) })
                        }
                    }
                }
            }

            state.sections.filter { state.filter.matches(it.source) }.forEach { section ->
                item(key = "t_" + section.title) { SectionTitle(section.title, section.source) }
                section.error?.let { err ->
                    item(key = "e_" + section.title) {
                        Text(
                            "Konnte nicht geladen werden: $err",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        )
                    }
                }
                items(section.tracks.distinctBy { it.key }.take(15), key = { section.title + it.key }) { track ->
                    VideoCard(
                        track = track,
                        resolving = track.key in resolving,
                        onPlay = { onPlay(track) },
                        onPlayNext = { onPlayNext(track) },
                        onEnqueue = { onEnqueue(track) },
                        onDownload = { onDownload(track, it) },
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
            }

            if (!state.loading && state.sections.isEmpty() && recent.isEmpty()) {
                item {
                    Text(
                        "Zum Aktualisieren nach unten ziehen",
                        color = Snow.copy(alpha = 0.8f),
                        modifier = Modifier.padding(32.dp),
                    )
                }
            }
        }
    }
}

private fun SourceFilter.matches(source: Source) = when (this) {
    SourceFilter.ALL -> true
    SourceFilter.YOUTUBE -> source == Source.YOUTUBE
    SourceFilter.NICONICO -> source == Source.NICONICO
}

@Composable
private fun SectionTitle(title: String, source: Source?) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 10.dp),
    ) {
        if (source != null) {
            SourceBadge(source)
            Spacer(Modifier.width(8.dp))
        }
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = Snow)
    }
}
