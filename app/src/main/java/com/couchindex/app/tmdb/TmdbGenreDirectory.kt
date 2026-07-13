package com.couchindex.app.tmdb

import com.couchindex.core.Genre
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

class TmdbGenreDirectory(
    private val source: TmdbGenreSource,
) {
    suspend fun fetchGenres(language: String = "en-US"): List<Genre> = coroutineScope {
        TmdbGenreMediaType.entries
            .map { mediaType ->
                async(Dispatchers.IO) {
                    mediaType to source.fetchGenres(mediaType, language)
                }
            }
            .awaitAll()
            .flatMap { (mediaType, genres) -> genres.map { genre -> genre to mediaType.mediaKind } }
            .groupBy { (genre, _) -> genre.id }
            .map { (id, matches) ->
                Genre(
                    id = id,
                    name = matches.first().first.name,
                    mediaKinds = matches.map { it.second }.toSet(),
                )
            }
            .sortedBy { it.name }
    }
}
