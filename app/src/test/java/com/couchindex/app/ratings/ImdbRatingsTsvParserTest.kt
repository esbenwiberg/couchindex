package com.couchindex.app.ratings

import java.io.StringReader
import org.junit.Assert.assertEquals
import org.junit.Test

class ImdbRatingsTsvParserTest {
    @Test
    fun `imports only requested IMDb title ratings`() {
        val input = """
            tconst	averageRating	numVotes
            tt0000001	5.7	2150
            tt0816692	8.7	2360000
            tt9999999	7.1	42
        """.trimIndent()

        val ratings = ImdbRatingsTsvParser.parse(
            reader = StringReader(input),
            requestedIds = setOf("tt0816692", "tt4040404"),
            retrievedAt = "2026-07-12",
        )

        assertEquals(setOf("tt0816692"), ratings.keys)
        assertEquals(8.7, ratings.getValue("tt0816692").value, 0.0)
        assertEquals(2_360_000, ratings.getValue("tt0816692").voteCount)
    }
}
