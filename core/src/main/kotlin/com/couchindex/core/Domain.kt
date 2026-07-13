package com.couchindex.core

enum class MediaKind {
    Movie,
    Series,
}

enum class ViewerProfile {
    Adult,
    Kids,
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
    val androidPackageName: String? = null,
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
    val androidPackageName: String? = null,
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
    val launchedAtEpochMillis: Long? = null,
)

data class WatchlistEntry(
    val titleId: TitleId,
    val addedAtEpochMillis: Long,
)

data class WatchedEntry(
    val titleId: TitleId,
    val watchedAtEpochMillis: Long,
)

enum class FeedbackValue {
    Liked,
    Disliked,
}

enum class KidsOverrideDecision {
    Allowed,
    Blocked,
}

data class KidsCatalogueOverride(
    val titleId: TitleId,
    val decision: KidsOverrideDecision,
    val allowsOverAge: Boolean = false,
    val changedAtEpochMillis: Long,
    val titleName: String = "",
)

data class FeedbackEntry(
    val titleId: TitleId,
    val value: FeedbackValue,
    val changedAtEpochMillis: Long,
)

data class ContentCertification(
    val countryCode: String,
    val rating: String,
    val minimumAge: Int,
)

data class Genre(
    val id: Int,
    val name: String,
    val mediaKinds: Set<MediaKind>,
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
    val releaseDate: String? = null,
    val isNewOnService: Boolean = false,
    val isHiddenGem: Boolean = false,
    val externalIds: Map<String, String> = emptyMap(),
    val posterUrl: String? = null,
    val genreIds: Set<Int> = emptySet(),
    val certification: ContentCertification? = null,
)

data class BrowseRow(
    val id: String,
    val label: String,
    val titles: List<Title>,
)
