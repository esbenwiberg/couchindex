package com.couchindex.app.tmdb

import com.couchindex.core.MediaKind
import com.couchindex.core.TitleId
import org.junit.Assert.assertEquals
import org.junit.Test

class TmdbTitleDetailsParserTest {
    @Test
    fun `parses movie runtime and supported external identifiers`() {
        val details = TmdbTitleDetailsParser.parse(
            """{"runtime":169,"external_ids":{"imdb_id":"tt0816692","wikidata_id":""}}""",
            MediaKind.Movie,
        )

        assertEquals(169, details.runtimeMinutes)
        assertEquals(mapOf("imdb" to "tt0816692"), details.externalIds)
    }

    @Test
    fun `parses the first positive series episode runtime`() {
        val details = TmdbTitleDetailsParser.parse(
            """{"episode_run_time":[0,52],"external_ids":{"imdb_id":"tt0903747"}}""",
            MediaKind.Series,
        )

        assertEquals(52, details.runtimeMinutes)
        assertEquals("tt0903747", details.externalIds["imdb"])
    }

    @Test
    fun `builds media-specific details urls with appended external IDs`() {
        val client = TmdbDiscoverClient("token", "https://example.test/3")

        assertEquals(
            "https://example.test/3/movie/42?append_to_response=external_ids&language=en-US",
            client.titleDetailsUrl(TitleId(42, MediaKind.Movie)).toString(),
        )
        assertEquals(
            "https://example.test/3/tv/84?append_to_response=external_ids&language=en-US",
            client.titleDetailsUrl(TitleId(84, MediaKind.Series)).toString(),
        )
    }
}
