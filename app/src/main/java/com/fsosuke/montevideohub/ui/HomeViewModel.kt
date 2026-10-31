package com.fsosuke.montevideohub.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fsosuke.montevideohub.data.NicoSource
import com.fsosuke.montevideohub.data.Source
import com.fsosuke.montevideohub.data.SourceFilter
import com.fsosuke.montevideohub.data.Track
import com.fsosuke.montevideohub.data.YouTubeSource
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeSection(val title: String, val source: Source, val tracks: List<Track>, val error: String? = null)

data class HomeUiState(
    val loading: Boolean = false,
    val filter: SourceFilter = SourceFilter.ALL,
    val sections: List<HomeSection> = emptyList(),
)

class HomeViewModel : ViewModel() {
    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun setFilter(filter: SourceFilter) = _state.update { it.copy(filter = filter) }

    fun refresh() {
        if (_state.value.loading) return
        viewModelScope.launch {
            _state.update { it.copy(loading = true) }
            val yt = async {
                runCatching { YouTubeSource.home() }.fold(
                    { (name, tracks) -> HomeSection("YouTube · $name", Source.YOUTUBE, tracks) },
                    { HomeSection("YouTube · Trends", Source.YOUTUBE, emptyList(), it.message ?: "Fehler") },
                )
            }
            val nicoMusic = async { nicoSection("niconico ランキング · 音楽・サウンド", "music_sound") }
            val nicoAll = async { nicoSection("niconico ランキング · 総合", "all") }
            val sections = listOf(yt.await(), nicoMusic.await(), nicoAll.await())
            _state.update { it.copy(loading = false, sections = sections) }
        }
    }

    private suspend fun nicoSection(title: String, genre: String) =
        runCatching { NicoSource.ranking(genre) }.fold(
            { HomeSection(title, Source.NICONICO, it) },
            { HomeSection(title, Source.NICONICO, emptyList(), it.message ?: "Fehler") },
        )
}
