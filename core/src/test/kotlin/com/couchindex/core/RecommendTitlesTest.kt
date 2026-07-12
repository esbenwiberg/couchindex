package com.couchindex.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecommendTitlesTest {
    private val drama = title(1, "Drama", setOf(18))
    private val spaceDrama = title(2, "Space Drama", setOf(18, 878))
    private val spaceComedy = title(3, "Space Comedy", setOf(35, 878))
    private val comedy = title(4, "Comedy", setOf(35))

    @Test
    fun `recommends unrated titles that overlap liked genres`() {
        val results = RecommendTitles().invoke(
            catalogue = listOf(drama, spaceDrama, spaceComedy, comedy),
            feedback = listOf(FeedbackEntry(drama.id, FeedbackValue.Liked, 10)),
        )

        assertEquals(listOf(spaceDrama), results)
    }

    @Test
    fun `disliked genre overlap suppresses otherwise matching titles`() {
        val results = RecommendTitles().invoke(
            catalogue = listOf(drama, spaceDrama, spaceComedy, comedy),
            feedback = listOf(
                FeedbackEntry(spaceDrama.id, FeedbackValue.Liked, 10),
                FeedbackEntry(comedy.id, FeedbackValue.Disliked, 20),
            ),
        )

        assertEquals(listOf(drama), results)
    }

    @Test
    fun `returns no recommendations without usable liked genres`() {
        assertTrue(RecommendTitles().invoke(listOf(drama), emptyList()).isEmpty())
    }

    private fun title(id: Int, name: String, genres: Set<Int>): Title =
        SampleCatalogue.titles.first().copy(
            id = TitleId(id, MediaKind.Movie),
            name = name,
            genreIds = genres,
        )
}
