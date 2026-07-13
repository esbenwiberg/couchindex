package com.couchindex.core

object SampleCatalogue {
    val providers = listOf(
        Provider(
            id = "netflix",
            name = "Netflix",
            tmdbProviderId = 8,
            androidPackageName = "com.netflix.ninja",
        ),
        Provider(
            id = "disney",
            name = "Disney+",
            tmdbProviderId = 337,
            androidPackageName = "com.disney.disneyplus",
        ),
        Provider(
            id = "max",
            name = "Max",
            tmdbProviderId = 1899,
            androidPackageName = "com.wbd.stream",
        ),
        Provider(
            id = "viaplay",
            name = "Viaplay",
            tmdbProviderId = 76,
            androidPackageName = "com.viaplay.android",
        ),
    )

    val subscriptions = listOf(
        Subscription(providerId = "netflix", enabled = true),
        Subscription(providerId = "disney", enabled = true),
        Subscription(providerId = "max", enabled = true),
        Subscription(providerId = "viaplay", enabled = false),
    )

    val genres = listOf(
        Genre(18, "Drama", setOf(MediaKind.Movie, MediaKind.Series)),
        Genre(35, "Comedy", setOf(MediaKind.Movie, MediaKind.Series)),
        Genre(99, "Documentary", setOf(MediaKind.Movie)),
        Genre(878, "Science Fiction", setOf(MediaKind.Movie, MediaKind.Series)),
        Genre(9648, "Mystery", setOf(MediaKind.Movie, MediaKind.Series)),
    )

    val recentLaunches = listOf(
        RecentLaunch(
            titleId = TitleId(tmdbId = 1002, mediaKind = MediaKind.Series),
            launchedAtLabel = "Yesterday",
            nextEpisodeLabel = "S1 E4",
        ),
        RecentLaunch(
            titleId = TitleId(tmdbId = 1005, mediaKind = MediaKind.Movie),
            launchedAtLabel = "Friday",
        ),
    )

    val titles = listOf(
        Title(
            id = TitleId(tmdbId = 1001, mediaKind = MediaKind.Movie),
            name = "Northern Signal",
            year = 2026,
            releaseDate = "2026-05-14",
            mediaKind = MediaKind.Movie,
            runtimeMinutes = 104,
            synopsis = "A radio engineer in Tromso finds a lost broadcast that seems to predict the next storm.",
            offers = subscriptionOffers("netflix", "max"),
            ratings = ratings(imdb = 7.8, imdbVotes = 18420, tmdb = 78.0, tmdbVotes = 2410),
            launchTargets = launchTargets("netflix", "max"),
            isNewOnService = true,
            genreIds = setOf(878, 18),
            certification = certification(11),
        ),
        Title(
            id = TitleId(tmdbId = 1002, mediaKind = MediaKind.Series),
            name = "Kitchen Shift",
            year = 2025,
            releaseDate = "2025-09-03",
            mediaKind = MediaKind.Series,
            runtimeMinutes = null,
            synopsis = "A Copenhagen restaurant team tries to keep service calm while everything personal boils over.",
            offers = subscriptionOffers("disney"),
            ratings = ratings(imdb = 8.5, imdbVotes = 62300, tmdb = 83.0, tmdbVotes = 5100),
            launchTargets = launchTargets("disney"),
            isNewOnService = true,
            genreIds = setOf(18, 35),
            certification = certification(7),
        ),
        Title(
            id = TitleId(tmdbId = 1003, mediaKind = MediaKind.Movie),
            name = "Low Tide",
            year = 2024,
            releaseDate = "2024-11-22",
            mediaKind = MediaKind.Movie,
            runtimeMinutes = 91,
            synopsis = "Two sisters return to an island hotel and uncover why every guest left on the same night.",
            offers = subscriptionOffers("max"),
            ratings = ratings(imdb = 7.3, imdbVotes = 9200, tmdb = 71.0, tmdbVotes = 840),
            launchTargets = launchTargets("max"),
            isHiddenGem = true,
            genreIds = setOf(9648, 18),
            certification = certification(15),
        ),
        Title(
            id = TitleId(tmdbId = 1004, mediaKind = MediaKind.Series),
            name = "Orbit Season",
            year = 2026,
            releaseDate = "2026-02-08",
            mediaKind = MediaKind.Series,
            runtimeMinutes = null,
            synopsis = "A small orbital crew manages supply failures, private doubts and the strangest sunrise on record.",
            offers = subscriptionOffers("netflix"),
            ratings = ratings(imdb = 8.1, imdbVotes = 35100, tmdb = 80.0, tmdbVotes = 4300),
            launchTargets = launchTargets("netflix"),
            genreIds = setOf(878, 18),
            certification = certification(7),
        ),
        Title(
            id = TitleId(tmdbId = 1005, mediaKind = MediaKind.Movie),
            name = "Long Weekend",
            year = 2023,
            releaseDate = "2023-06-16",
            mediaKind = MediaKind.Movie,
            runtimeMinutes = 112,
            synopsis = "A family holiday turns into a dry, funny negotiation over who gets to remember the past correctly.",
            offers = subscriptionOffers("disney", "viaplay"),
            ratings = ratings(imdb = 7.6, imdbVotes = 14600, tmdb = 76.0, tmdbVotes = 1200),
            launchTargets = launchTargets("disney", "viaplay"),
            isHiddenGem = true,
            genreIds = setOf(35, 18),
            certification = certification(0),
        ),
        Title(
            id = TitleId(tmdbId = 1006, mediaKind = MediaKind.Movie),
            name = "Glass Mountain",
            year = 2025,
            releaseDate = "2025-01-30",
            mediaKind = MediaKind.Movie,
            runtimeMinutes = 136,
            synopsis = "A climbing documentary follows three attempts, one impossible route and no clean heroic ending.",
            offers = subscriptionOffers("viaplay"),
            ratings = ratings(imdb = 8.0, imdbVotes = 6300, tmdb = 79.0, tmdbVotes = 940),
            launchTargets = launchTargets("viaplay"),
            isNewOnService = true,
            genreIds = setOf(99),
            certification = certification(7),
        ),
    )

    private fun subscriptionOffers(vararg providerIds: String): List<Offer> =
        providerIds.map { providerId ->
            Offer(
                providerId = providerId,
                region = "DK",
                monetizationType = MonetizationType.Subscription,
            )
        }

    private fun launchTargets(vararg providerIds: String): List<LaunchTarget> =
        providerIds.map { providerId ->
            val provider = providers.first { it.id == providerId }
            LaunchTarget(
                providerId = providerId,
                label = provider.name,
                androidPackageName = provider.androidPackageName,
            )
        }

    private fun ratings(
        imdb: Double,
        imdbVotes: Int,
        tmdb: Double,
        tmdbVotes: Int,
    ): List<Rating> = listOf(
        Rating(
            source = "IMDb",
            value = imdb,
            scale = 10.0,
            voteCount = imdbVotes,
            scope = RatingScope.Title,
            retrievedAt = "2026-07-11",
        ),
        Rating(
            source = "TMDb",
            value = tmdb,
            scale = 100.0,
            voteCount = tmdbVotes,
            scope = RatingScope.Title,
            retrievedAt = "2026-07-11",
        ),
    )

    private fun certification(minimumAge: Int): ContentCertification =
        ContentCertification(countryCode = "DK", rating = minimumAge.toString(), minimumAge = minimumAge)

    private fun providerName(providerId: String): String =
        providers.first { it.id == providerId }.name
}
