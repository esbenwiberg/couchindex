package com.couchindex.app.tmdb

import com.couchindex.core.MediaKind
import com.couchindex.core.Provider
import com.couchindex.core.Rating
import com.couchindex.core.RatingAdapter
import com.couchindex.core.RatingScope
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TmdbCatalogueRepositoryTest {
    private val providers = listOf(
        Provider(
            id = "netflix",
            name = "Netflix",
            tmdbProviderId = 8,
            androidPackageName = "com.netflix.ninja",
        ),
        Provider(id = "disney", name = "Disney+", tmdbProviderId = 337),
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
            genreIds = setOf(878, 18),
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
            retrievedAt = { "2026-07-12T00:00:00Z" },
        )

        val titles = repository.discoverSubscriptionTitles(
            region = "DK",
            providerIds = setOf("netflix", "disney"),
        )

        assertEquals(1, titles.size)
        assertEquals(setOf("netflix", "disney"), titles.single().offers.map { it.providerId }.toSet())
        assertEquals(listOf("Netflix", "Disney+"), titles.single().launchTargets.map { it.label })
        assertEquals(
            "https://www.themoviedb.org/movie/42/watch?locale=DK",
            titles.single().launchTargets.first().uri,
        )
        assertEquals("com.netflix.ninja", titles.single().launchTargets.first().androidPackageName)
        assertEquals(8.1, titles.single().ratings.single().value, 0.0)
        assertEquals(12_400, titles.single().ratings.single().voteCount)
        assertEquals("https://image.tmdb.org/t/p/w500/poster.jpg", titles.single().posterUrl)
        assertEquals(setOf(878, 18), titles.single().genreIds)
    }

    @Test
    fun `ignores subscriptions without a TMDb provider mapping`() = runBlocking {
        var requested = false
        val repository = TmdbCatalogueRepository(
            source = TmdbDiscoverSource {
                requested = true
                error("Unexpected request")
            },
            providers = listOf(Provider(id = "unknown", name = "Unknown")),
        )

        val titles = repository.discoverSubscriptionTitles("DK", setOf("unknown"))

        assertTrue(titles.isEmpty())
        assertEquals(false, requested)
    }

    @Test
    fun `enriches discovered titles through replaceable rating adapters`() = runBlocking {
        val item = TmdbDiscoverItem(
            tmdbId = 42,
            mediaKind = MediaKind.Movie,
            name = "Shared Movie",
            year = 2025,
            overview = "Available from both subscriptions.",
            posterPath = null,
            voteAverage = 8.1,
            voteCount = 12_400,
        )
        val repository = TmdbCatalogueRepository(
            source = TmdbDiscoverSource { query ->
                TmdbDiscoverPage(
                    page = 1,
                    totalPages = 1,
                    totalResults = if (query.mediaType == TmdbDiscoverMediaType.Movie) 1 else 0,
                    results = if (query.mediaType == TmdbDiscoverMediaType.Movie) listOf(item) else emptyList(),
                )
            },
            providers = providers,
            ratingAdapters = listOf(
                RatingAdapter {
                    listOf(
                        Rating(
                            source = "IMDb",
                            value = 8.3,
                            scale = 10.0,
                            voteCount = 50_000,
                            scope = RatingScope.Title,
                            retrievedAt = "2026-07-12",
                        ),
                    )
                },
            ),
        )

        val title = repository.discoverSubscriptionTitles("DK", setOf("netflix")).single()

        assertEquals(listOf("TMDb", "IMDb"), title.ratings.map { it.source })
    }

    @Test
    fun `adds explicit external identifiers without fuzzy matching`() = runBlocking {
        val item = TmdbDiscoverItem(
            tmdbId = 42,
            mediaKind = MediaKind.Movie,
            name = "Shared Movie",
            year = 2025,
            overview = "Available from both subscriptions.",
            posterPath = null,
            voteAverage = 8.1,
            voteCount = 12_400,
        )
        val repository = TmdbCatalogueRepository(
            source = TmdbDiscoverSource { query ->
                TmdbDiscoverPage(
                    page = 1,
                    totalPages = 1,
                    totalResults = if (query.mediaType == TmdbDiscoverMediaType.Movie) 1 else 0,
                    results = if (query.mediaType == TmdbDiscoverMediaType.Movie) listOf(item) else emptyList(),
                )
            },
            providers = providers,
            titleDetailsSource = TmdbTitleDetailsSource { titleId ->
                assertEquals(item.tmdbId, titleId.tmdbId)
                TmdbTitleDetails(
                    externalIds = mapOf("imdb" to "tt1234567"),
                    runtimeMinutes = 121,
                )
            },
        )

        val title = repository.discoverSubscriptionTitles("DK", setOf("netflix")).single()

        assertEquals("tt1234567", title.externalIds["imdb"])
        assertEquals(121, title.runtimeMinutes)
    }
}
