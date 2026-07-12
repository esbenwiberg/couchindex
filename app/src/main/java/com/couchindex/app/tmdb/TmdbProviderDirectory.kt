package com.couchindex.app.tmdb

import com.couchindex.core.Provider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

class TmdbProviderDirectory(
    private val source: TmdbProviderSource,
    fallbackProviders: List<Provider>,
) {
    private val fallbackByTmdbId = fallbackProviders.mapNotNull { provider ->
        provider.tmdbProviderId?.let { it to provider }
    }.toMap()

    suspend fun fetchProviders(region: String): List<Provider> = coroutineScope {
        TmdbProviderMediaType.entries
            .map { mediaType ->
                async(Dispatchers.IO) { source.fetchWatchProviders(mediaType, region) }
            }
            .awaitAll()
            .flatten()
            .groupBy { it.tmdbProviderId }
            .map { (tmdbProviderId, matches) ->
                val fallback = fallbackByTmdbId[tmdbProviderId]
                val provider = matches.minBy { it.displayPriority }
                Provider(
                    id = fallback?.id ?: "tmdb-$tmdbProviderId",
                    name = provider.name,
                    shortName = fallback?.shortName ?: provider.name,
                    tmdbProviderId = tmdbProviderId,
                    displayPriority = provider.displayPriority,
                )
            }
            .sortedWith(compareBy<Provider> { it.displayPriority }.thenBy { it.name })
    }
}
