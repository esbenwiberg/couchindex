package com.couchindex.app.tmdb

import com.couchindex.core.MediaKind
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class TmdbGenreDirectoryTest {
    @Test
    fun `builds official movie and TV genre URLs`() {
        val client = TmdbDiscoverClient("", "https://example.test/3")

        assertEquals(
            "https://example.test/3/genre/movie/list?language=en-US",
            client.genresUrl(TmdbGenreMediaType.Movie).toString(),
        )
        assertEquals(
            "https://example.test/3/genre/tv/list?language=en-US",
            client.genresUrl(TmdbGenreMediaType.Tv).toString(),
        )
    }

    @Test
    fun `merges shared genre ids and preserves media support`() = runBlocking {
        val source = TmdbGenreSource { mediaType, _ ->
            when (mediaType) {
                TmdbGenreMediaType.Movie -> listOf(TmdbGenre(18, "Drama"), TmdbGenre(28, "Action"))
                TmdbGenreMediaType.Tv -> listOf(TmdbGenre(18, "Drama"), TmdbGenre(10765, "Sci-Fi & Fantasy"))
            }
        }

        val genres = TmdbGenreDirectory(source).fetchGenres()

        assertEquals(listOf("Action", "Drama", "Sci-Fi & Fantasy"), genres.map { it.name })
        assertEquals(setOf(MediaKind.Movie, MediaKind.Series), genres.first { it.id == 18 }.mediaKinds)
        assertEquals(setOf(MediaKind.Series), genres.first { it.id == 10765 }.mediaKinds)
    }
}
