package com.couchindex.app.tmdb

import com.couchindex.core.MediaKind
import com.couchindex.core.Provider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TmdbCatalogueRepositoryTest {
    private val providers = listOf(
        Provider(id = "netflix", name = "Netflix"),
        Provider(id = "disney", name = "Disney+"),
    )

    @Test
    fun `discovers each provider separately and merges offers by title identity`() = runBlocking {
        val sharedMovie = TmdbDiscoverItem(
            tmdbId = 42,
            mediaKind = MediaKind.Movie,
            name = "Shared Movie",
            year = 2025,
            overview = "Available from both subscriptions.",
            posterPath = "/poster.jpg",
            voteAverage = 8.1,
            voteCount = 12_400,
        )
        val source = TmdbDiscoverSource { query ->
            TmdbDiscoverPage(
                page = 1,
                totalPages = 1,
                totalResults = if (query.mediaType == TmdbDiscoverMediaType.Movie) 1 else 0,
                results = if (query.mediaType == TmdbDiscoverMediaType.Movie) listOf(sharedMovie) else emptyList(),
            )
        }
        val repository = TmdbCatalogueRepository(
            source = source,
            providers = providers,
            tmdbProviderIds = mapOf("netflix" to 8, "disney" to 337),
            retrievedAt = { "2026-07-12T00:00:00Z" },
        )

        val titles = repository.discoverSubscriptionTitles(
            region = "DK",
            providerIds = setOf("netflix", "disney"),
        )

        assertEquals(1, titles.size)
        assertEquals(setOf("netflix", "disney"), titles.single().offers.map { it.providerId }.toSet())
        assertEquals(listOf("Open in Netflix", "Open in Disney+"), titles.single().launchTargets.map { it.label })
        assertEquals(8.1, titles.single().ratings.single().value, 0.0)
        assertEquals(12_400, titles.single().ratings.single().voteCount)
    }

    @Test
    fun `ignores subscriptions without a TMDb provider mapping`() = runBlocking {
        var requested = false
        val repository = TmdbCatalogueRepository(
            source = TmdbDiscoverSource {
                requested = true
                error("Unexpected request")
            },
            providers = providers,
            tmdbProviderIds = emptyMap(),
        )

        val titles = repository.discoverSubscriptionTitles("DK", setOf("unknown"))

        assertTrue(titles.isEmpty())
        assertEquals(false, requested)
    }
}
