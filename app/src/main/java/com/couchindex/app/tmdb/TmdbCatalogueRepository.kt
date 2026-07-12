package com.couchindex.app.tmdb

import com.couchindex.core.CatalogueRepository
import com.couchindex.core.BatchRatingAdapter
import com.couchindex.core.EnrichTitleBatchRatings
import com.couchindex.core.EnrichTitleRatings
import com.couchindex.core.LaunchTarget
import com.couchindex.core.MediaKind
import com.couchindex.core.MonetizationType
import com.couchindex.core.Offer
import com.couchindex.core.Provider
import com.couchindex.core.Rating
import com.couchindex.core.RatingAdapter
import com.couchindex.core.RatingScope
import com.couchindex.core.Title
import com.couchindex.core.TitleId
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

class TmdbCatalogueRepository(
    private val source: TmdbDiscoverSource,
    providers: List<Provider>,
    private val titleDetailsSource: TmdbTitleDetailsSource? = source as? TmdbTitleDetailsSource,
    ratingAdapters: List<RatingAdapter> = emptyList(),
    batchRatingAdapters: List<BatchRatingAdapter> = emptyList(),
    private val retrievedAt: () -> String = { Instant.now().toString() },
) : CatalogueRepository {
    private val enrichTitleRatings = EnrichTitleRatings(ratingAdapters)
    private val enrichTitleBatchRatings = EnrichTitleBatchRatings(batchRatingAdapters)
    private val providersById = providers.associateBy { it.id }
    private val tmdbProviderIds = providers.mapNotNull { provider ->
        provider.tmdbProviderId?.let { provider.id to it }
    }.toMap()

    override suspend fun discoverSubscriptionTitles(
        region: String,
        providerIds: Set<String>,
    ): List<Title> = coroutineScope {
        val requests = providerIds.mapNotNull { providerId ->
            tmdbProviderIds[providerId]?.let { providerId to it }
        }.flatMap { (providerId, tmdbProviderId) ->
            TmdbDiscoverMediaType.entries.map { mediaType ->
                async(Dispatchers.IO) {
                    source.fetchDiscoverPage(
                        TmdbDiscoverQuery(
                            mediaType = mediaType,
                            region = region,
                            tmdbProviderIds = setOf(tmdbProviderId),
                        ),
                    ).results.map { item -> DiscoveredOffer(item, providerId) }
                }
            }
        }

        val titles = mergeOffers(requests.awaitAll().flatten(), region)
        val detailsSource = titleDetailsSource
        val detailedTitles = if (detailsSource == null) {
            titles
        } else {
            buildList {
                for (batch in titles.chunked(TITLE_DETAILS_CONCURRENCY)) {
                    addAll(
                        batch.map { title ->
                            async(Dispatchers.IO) {
                                val details = runCatching { detailsSource.fetchTitleDetails(title.id) }.getOrNull()
                                title.copy(
                                    externalIds = details?.externalIds.orEmpty(),
                                    runtimeMinutes = details?.runtimeMinutes,
                                )
                            }
                        }.awaitAll(),
                    )
                }
            }
        }
        val individuallyEnriched = detailedTitles.map(enrichTitleRatings::invoke)
        withContext(Dispatchers.IO) { enrichTitleBatchRatings.invoke(individuallyEnriched) }
    }

    private fun mergeOffers(discovered: List<DiscoveredOffer>, region: String): List<Title> {
        val timestamp = retrievedAt()
        return discovered
            .groupBy { TitleId(it.item.tmdbId, it.item.mediaKind) }
            .map { (titleId, matches) ->
                val item = matches.first().item
                val matchedProviderIds = matches.map { it.providerId }.distinct()
                Title(
                    id = titleId,
                    name = item.name,
                    year = item.year,
                    mediaKind = item.mediaKind,
                    runtimeMinutes = null,
                    synopsis = item.overview,
                    posterUrl = item.posterPath?.let { path -> "$TMDB_IMAGE_BASE_URL$path" },
                    genreIds = item.genreIds,
                    offers = matchedProviderIds.map { providerId ->
                        Offer(
                            providerId = providerId,
                            region = region,
                            monetizationType = MonetizationType.Subscription,
                        )
                    },
                    ratings = item.tmdbRating(timestamp),
                    launchTargets = matchedProviderIds.map { providerId ->
                        val provider = providersById[providerId]
                        LaunchTarget(
                            providerId = providerId,
                            label = provider?.name ?: providerId,
                            uri = titleId.tmdbWatchUri(region),
                            androidPackageName = provider?.androidPackageName,
                        )
                    },
                )
            }
            .sortedWith(compareByDescending<Title> { it.ratings.firstOrNull()?.voteCount ?: 0 }.thenBy { it.name })
    }

    private fun TmdbDiscoverItem.tmdbRating(timestamp: String): List<Rating> =
        voteAverage?.takeIf { it > 0.0 }?.let { average ->
            listOf(
                Rating(
                    source = "TMDb",
                    value = average,
                    scale = 10.0,
                    voteCount = voteCount,
                    scope = RatingScope.Title,
                    retrievedAt = timestamp,
                ),
            )
        }.orEmpty()

    private data class DiscoveredOffer(
        val item: TmdbDiscoverItem,
        val providerId: String,
    )

    private fun TitleId.tmdbWatchUri(region: String): String {
        val mediaPath = when (mediaKind) {
            MediaKind.Movie -> "movie"
            MediaKind.Series -> "tv"
        }
        return "https://www.themoviedb.org/$mediaPath/$tmdbId/watch?locale=$region"
    }

    companion object {
        private const val TITLE_DETAILS_CONCURRENCY = 8
        private const val TMDB_IMAGE_BASE_URL = "https://image.tmdb.org/t/p/w500"
    }
}
