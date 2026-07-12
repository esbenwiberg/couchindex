package com.couchindex.app.tmdb

import com.couchindex.core.MediaKind
import com.couchindex.core.TitleId
import org.junit.Assert.assertEquals
import org.junit.Test

class TmdbExternalIdParserTest {
    @Test
    fun `parses supported external identifiers and omits blanks`() {
        val ids = TmdbExternalIdParser.parse(
            """{"imdb_id":"tt0816692","wikidata_id":""}""",
        )

        assertEquals(mapOf("imdb" to "tt0816692"), ids)
    }

    @Test
    fun `builds media-specific external ID urls`() {
        val client = TmdbDiscoverClient("token", "https://example.test/3")

        assertEquals(
            "https://example.test/3/movie/42/external_ids",
            client.externalIdsUrl(TitleId(42, MediaKind.Movie)).toString(),
        )
        assertEquals(
            "https://example.test/3/tv/84/external_ids",
            client.externalIdsUrl(TitleId(84, MediaKind.Series)).toString(),
        )
    }
}
