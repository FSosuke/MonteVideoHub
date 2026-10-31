package com.fsosuke.montevideohub.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fsosuke.montevideohub.data.SearchRepository
import com.fsosuke.montevideohub.data.SourceFilter
import com.fsosuke.montevideohub.data.Track
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SearchUiState(
    val query: String = "",
    val filter: SourceFilter = SourceFilter.ALL,
    val musicOnly: Boolean = true,
    val loading: Boolean = false,
    val searched: Boolean = false,
    val results: List<Track> = emptyList(),
    val error: String? = null,
)

class SearchViewModel : ViewModel() {
    private val _state = MutableStateFlow(SearchUiState())
    val state: StateFlow<SearchUiState> = _state.asStateFlow()
    private var job: Job? = null

    fun onQueryChange(query: String) = _state.update { it.copy(query = query) }

    fun setFilter(filter: SourceFilter) {
        _state.update { it.copy(filter = filter) }
        if (_state.value.searched) search()
    }

    fun setMusicOnly(musicOnly: Boolean) {
        _state.update { it.copy(musicOnly = musicOnly) }
        if (_state.value.searched) search()
    }

    fun search() {
        val s = _state.value
        val query = s.query.trim()
        if (query.isEmpty()) return
        job?.cancel()
        job = viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null, searched = true) }
            val result = SearchRepository.search(query, s.filter, s.musicOnly)
            _state.update {
                it.copy(
                    loading = false,
                    results = result.tracks,
                    error = result.errors.joinToString("\n").ifEmpty { null },
                )
            }
        }
    }
}
