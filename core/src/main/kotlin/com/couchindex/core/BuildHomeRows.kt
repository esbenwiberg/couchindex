package com.couchindex.core

class BuildHomeRows(
    private val ratingQualityPolicy: RatingQualityPolicy = RatingQualityPolicy(),
    private val recommendTitles: RecommendTitles = RecommendTitles(),
) {
    fun invoke(
        catalogue: List<Title>,
        subscriptions: List<Subscription>,
        recentLaunches: List<RecentLaunch>,
        watchlistEntries: List<WatchlistEntry> = emptyList(),
        feedbackEntries: List<FeedbackEntry> = emptyList(),
        region: String = "DK",
    ): List<BrowseRow> {
        val enabledProviderIds = subscriptions
            .filter { it.enabled }
            .map { it.providerId }
            .toSet()

        val availableTitles = catalogue.filter { title ->
            title.offers.any { offer ->
                offer.region == region &&
                    offer.monetizationType == MonetizationType.Subscription &&
                    offer.providerId in enabledProviderIds
            }
        }

        val byId = availableTitles.associateBy { it.id }
        val continueWatching = recentLaunches.mapNotNull { byId[it.titleId] }
        val watchlist = watchlistEntries.mapNotNull { byId[it.titleId] }

        return listOf(
            BrowseRow(
                id = "continue-watching",
                label = "Continue Watching",
                titles = continueWatching,
            ),
            BrowseRow(
                id = "watchlist",
                label = "My Watchlist",
                titles = watchlist,
            ),
            BrowseRow(
                id = "for-you",
                label = "For You",
                titles = recommendTitles.invoke(availableTitles, feedbackEntries),
            ),
            BrowseRow(
                id = "new-on-my-services",
                label = "New on My Services",
                titles = availableTitles.filter { it.isNewOnService },
            ),
            BrowseRow(
                id = "highly-rated",
                label = "Highly Rated",
                titles = ratingQualityPolicy.highlyRated(availableTitles),
            ),
            BrowseRow(
                id = "movies",
                label = "Movies",
                titles = availableTitles.filter { it.mediaKind == MediaKind.Movie },
            ),
            BrowseRow(
                id = "series",
                label = "Series",
                titles = availableTitles.filter { it.mediaKind == MediaKind.Series },
            ),
            BrowseRow(
                id = "under-two-hours",
                label = "Under Two Hours",
                titles = availableTitles.filter { runtimeUnderTwoHours(it) },
            ),
            BrowseRow(
                id = "hidden-gems",
                label = "Hidden Gems",
                titles = ratingQualityPolicy.hiddenGems(availableTitles),
            ),
        ).filter { row -> row.id == "continue-watching" || row.titles.isNotEmpty() }
    }

    private fun runtimeUnderTwoHours(title: Title): Boolean =
        title.mediaKind == MediaKind.Movie &&
            title.runtimeMinutes != null &&
            title.runtimeMinutes <= 120
}
