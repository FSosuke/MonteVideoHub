package com.fsosuke.montevideohub.data

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

data class SearchResult(val tracks: List<Track>, val errors: List<String>)

object SearchRepository {

    /** Sucht parallel auf beiden Plattformen und mischt die Ergebnisse abwechselnd. */
    suspend fun search(query: String, filter: SourceFilter, musicOnly: Boolean): SearchResult =
        coroutineScope {
            val yt = if (filter != SourceFilter.NICONICO) {
                async { runCatching { YouTubeSource.search(query, musicOnly) } }
            } else null
            val nico = if (filter != SourceFilter.YOUTUBE) {
                async { runCatching { NicoSource.search(query, musicOnly) } }
            } else null

            val ytResult = yt?.await()
            val nicoResult = nico?.await()

            val errors = buildList {
                ytResult?.exceptionOrNull()?.let { add("YouTube: ${it.message ?: it.javaClass.simpleName}") }
                nicoResult?.exceptionOrNull()?.let { add("niconico: ${it.message ?: it.javaClass.simpleName}") }
            }
            SearchResult(
                interleave(ytResult?.getOrNull().orEmpty(), nicoResult?.getOrNull().orEmpty()).distinctBy { it.key },
                errors,
            )
        }

    suspend fun resolve(track: Track): ResolvedStream = when (track.source) {
        Source.YOUTUBE -> YouTubeSource.resolve(track.id)
        Source.NICONICO -> NicoSource.resolve(track.id)
    }

    private fun <T> interleave(a: List<T>, b: List<T>): List<T> = buildList {
        val max = maxOf(a.size, b.size)
        for (i in 0 until max) {
            a.getOrNull(i)?.let(::add)
            b.getOrNull(i)?.let(::add)
        }
    }
}
