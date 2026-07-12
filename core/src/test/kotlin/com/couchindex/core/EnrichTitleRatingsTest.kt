package com.couchindex.core

import org.junit.Assert.assertEquals
import org.junit.Test

class EnrichTitleRatingsTest {
    private val title = SampleCatalogue.titles.first()

    @Test
    fun `keeps distinct sources separate`() {
        val imdb = rating(source = "IMDb", value = 8.2, votes = 50_000)
        val enriched = EnrichTitleRatings(
            adapters = listOf(RatingAdapter { listOf(imdb) }),
        ).invoke(title.copy(ratings = listOf(rating(source = "TMDb", value = 7.8, votes = 4_000))))

        assertEquals(listOf("TMDb", "IMDb"), enriched.ratings.map { it.source })
    }

    @Test
    fun `newer adapter value replaces the same source and scope`() {
        val enriched = EnrichTitleRatings(
            adapters = listOf(RatingAdapter { listOf(rating(source = "imdb", value = 8.4, votes = 60_000)) }),
        ).invoke(title.copy(ratings = listOf(rating(source = "IMDb", value = 8.2, votes = 50_000))))

        assertEquals(1, enriched.ratings.size)
        assertEquals(8.4, enriched.ratings.single().value, 0.0)
        assertEquals(60_000, enriched.ratings.single().voteCount)
    }

    @Test
    fun `one failing adapter does not discard catalogue or healthy ratings`() {
        val enriched = EnrichTitleRatings(
            adapters = listOf(
                RatingAdapter { error("source unavailable") },
                RatingAdapter { listOf(rating(source = "IMDb", value = 8.2, votes = 50_000)) },
            ),
        ).invoke(title.copy(ratings = emptyList()))

        assertEquals(listOf("IMDb"), enriched.ratings.map { it.source })
    }

    @Test
    fun `batch adapter enriches matching titles and preserves order`() {
        val second = SampleCatalogue.titles[1]
        val enriched = EnrichTitleBatchRatings(
            adapters = listOf(
                BatchRatingAdapter {
                    mapOf(second.id to listOf(rating(source = "IMDb", value = 8.6, votes = 70_000)))
                },
            ),
        ).invoke(listOf(title.copy(ratings = emptyList()), second.copy(ratings = emptyList())))

        assertEquals(listOf(title.id, second.id), enriched.map { it.id })
        assertEquals(emptyList<String>(), enriched.first().ratings.map { it.source })
        assertEquals(listOf("IMDb"), enriched.last().ratings.map { it.source })
    }

    private fun rating(source: String, value: Double, votes: Int): Rating =
        Rating(
            source = source,
            value = value,
            scale = 10.0,
            voteCount = votes,
            scope = RatingScope.Title,
            retrievedAt = "2026-07-12",
        )
}
