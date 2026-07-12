package com.couchindex.app.tmdb

import org.junit.Assert.assertEquals
import org.junit.Test

class TmdbDiscoverParserTest {
    @Test
    fun `preserves valid genre identifiers from discover results`() {
        val page = TmdbDiscoverParser.parse(
            body = """
                {
                  "page": 1,
                  "total_pages": 1,
                  "total_results": 1,
                  "results": [{
                    "id": 42,
                    "title": "Shared Movie",
                    "release_date": "2025-01-02",
                    "genre_ids": [878, 18, 0]
                  }]
                }
            """.trimIndent(),
            mediaType = TmdbDiscoverMediaType.Movie,
        )

        assertEquals(setOf(878, 18), page.results.single().genreIds)
    }
}
