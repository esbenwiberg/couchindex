package com.couchindex.core

enum class MediaKind {
    Movie,
    Series,
}

enum class MonetizationType {
    Subscription,
    Rent,
    Buy,
}

enum class RatingScope {
    Title,
    Season,
    Episode,
}

data class TitleId(
    val tmdbId: Int,
    val mediaKind: MediaKind,
)

data class Provider(
    val id: String,
    val name: String,
    val shortName: String = name,
    val tmdbProviderId: Int? = null,
    val displayPriority: Int = Int.MAX_VALUE,
)

data class Offer(
    val providerId: String,
    val region: String,
    val monetizationType: MonetizationType,
)

data class LaunchTarget(
    val providerId: String,
    val label: String,
    val uri: String? = null,
)

data class Rating(
    val source: String,
    val value: Double,
    val scale: Double,
    val voteCount: Int?,
    val scope: RatingScope,
    val retrievedAt: String,
)

data class Subscription(
    val providerId: String,
    val enabled: Boolean,
)

data class RecentLaunch(
    val titleId: TitleId,
    val launchedAtLabel: String,
    val nextEpisodeLabel: String? = null,
)

data class Title(
    val id: TitleId,
    val name: String,
    val year: Int?,
    val mediaKind: MediaKind,
    val runtimeMinutes: Int?,
    val synopsis: String,
    val offers: List<Offer>,
    val ratings: List<Rating>,
    val launchTargets: List<LaunchTarget>,
    val isNewOnService: Boolean = false,
    val isHiddenGem: Boolean = false,
)

data class BrowseRow(
    val id: String,
    val label: String,
    val titles: List<Title>,
)
