package com.couchindex.core

import org.junit.Assert.assertEquals
import org.junit.Test

class BrowseCatalogueTest {
    private val browse = BrowseCatalogue()

    @Test
    fun `filters media kind and genre locally`() {
        val movie = title(1, "Movie", MediaKind.Movie, genres = setOf(18))
        val series = title(2, "Series", MediaKind.Series, genres = setOf(18, 35))
        val comedy = title(3, "Comedy", MediaKind.Movie, genres = setOf(35))

        val result = browse.invoke(
            listOf(movie, series, comedy),
            BrowseCatalogueQuery(BrowseMediaFilter.Movies, genreId = 18),
        )

        assertEquals(listOf(movie), result)
    }

    @Test
    fun `newest uses complete release date and leaves missing dates last`() {
        val february = title(1, "February", releaseDate = "2026-02-01")
        val december = title(2, "December", releaseDate = "2026-12-01")
        val unknown = title(3, "Unknown")

        val result = browse.invoke(listOf(february, unknown, december), BrowseCatalogueQuery(sort = BrowseSort.Newest))

        assertEquals(listOf("December", "February", "Unknown"), result.map { it.name })
    }

    @Test
    fun `rating sources remain distinct and use vote support as tie breaker`() {
        val first = title(
            1,
            "First",
            ratings = listOf(rating("IMDb", 8.0, 100), rating("TMDb", 9.0, 50)),
        )
        val supported = title(
            2,
            "Supported",
            ratings = listOf(rating("IMDb", 8.0, 1_000), rating("TMDb", 7.0, 2_000)),
        )
        val missing = title(3, "Missing")

        assertEquals(
            listOf("Supported", "First", "Missing"),
            browse.invoke(listOf(first, missing, supported), BrowseCatalogueQuery(sort = BrowseSort.ImdbRating)).map { it.name },
        )
        assertEquals(
            listOf("First", "Supported", "Missing"),
            browse.invoke(listOf(first, missing, supported), BrowseCatalogueQuery(sort = BrowseSort.TmdbRating)).map { it.name },
        )
    }

    private fun title(
        id: Int,
        name: String,
        mediaKind: MediaKind = MediaKind.Movie,
        releaseDate: String? = null,
        genres: Set<Int> = emptySet(),
        ratings: List<Rating> = emptyList(),
    ) = Title(
        id = TitleId(id, mediaKind),
        name = name,
        year = releaseDate?.take(4)?.toIntOrNull(),
        releaseDate = releaseDate,
        mediaKind = mediaKind,
        runtimeMinutes = null,
        synopsis = "",
        offers = emptyList(),
        ratings = ratings,
        launchTargets = emptyList(),
        genreIds = genres,
    )

    private fun rating(source: String, value: Double, votes: Int) = Rating(
        source = source,
        value = value,
        scale = 10.0,
        voteCount = votes,
        scope = RatingScope.Title,
        retrievedAt = "2026-07-13",
    )
}
