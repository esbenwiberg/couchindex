package com.couchindex.app.tmdb

import com.couchindex.core.Provider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class TmdbProviderDirectoryTest {
    @Test
    fun `builds a region-scoped provider directory URL`() {
        val client = TmdbDiscoverClient(
            readAccessToken = "",
            baseUrl = "https://example.test/3",
        )

        val url = client.watchProvidersUrl(TmdbProviderMediaType.Movie, "DK")

        assertEquals(
            "https://example.test/3/watch/providers/movie?language=en-US&watch_region=DK",
            url.toString(),
        )
    }

    @Test
    fun `merges movie and TV providers while preserving known local ids`() = runBlocking {
        val source = TmdbProviderSource { mediaType, _ ->
            when (mediaType) {
                TmdbProviderMediaType.Movie -> listOf(
                    TmdbWatchProvider(8, "Netflix", 2),
                    TmdbWatchProvider(999, "Another Stream", 9),
                )

                TmdbProviderMediaType.Tv -> listOf(
                    TmdbWatchProvider(8, "Netflix", 1),
                    TmdbWatchProvider(337, "Disney Plus", 3),
                )
            }
        }
        val directory = TmdbProviderDirectory(
            source = source,
            fallbackProviders = listOf(
                Provider(id = "netflix", name = "Netflix", tmdbProviderId = 8),
                Provider(id = "disney", name = "Disney+", shortName = "Disney+", tmdbProviderId = 337),
            ),
        )

        val providers = directory.fetchProviders("DK")

        assertEquals(listOf("netflix", "disney", "tmdb-999"), providers.map { it.id })
        assertEquals(listOf(1, 3, 9), providers.map { it.displayPriority })
        assertEquals("Disney+", providers[1].shortName)
    }
}
